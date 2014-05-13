package info.beastarman.e621.middleware;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.api.E621Tag;
import info.beastarman.e621.backend.ImageCacheManager;
import info.beastarman.e621.frontend.MainActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class E621Middleware extends E621
{
	HashMap<String,E621Image> e621ImageCache = new HashMap<String,E621Image>();
	
	File cache_path = null;
	File full_cache_path = null;
	File sd_path = null;
	File download_path = null;
	
	public static final String PREFS_NAME = "E621MobilePreferences";
	
	SharedPreferences settings;
	SharedPreferences.OnSharedPreferenceChangeListener settingsListener;
	
	ImageCacheManager thumb_cache;
	ImageCacheManager full_cache;
	E621DownloadedImages download_manager;
	
	private static int UPDATE_TAGS_NOTIFICATION_ID = 1;
	private Semaphore updateTagsSemaphore = new Semaphore(1);
	
	private static E621Middleware instance;
	
	protected E621Middleware()
	{
		Context ctx = MainActivity.getContext();
		
		cache_path = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"cache/");
		full_cache_path = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"full_cache/");
		sd_path = new File(Environment.getExternalStorageDirectory(),"e621/");
		download_path = new File(sd_path,"e612 Images/");
		
		settings = ctx.getSharedPreferences(PREFS_NAME, 0);
		
		settingsListener = new SharedPreferences.OnSharedPreferenceChangeListener()
		{
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
			{
				setup();
			}
		};
		
		settings.registerOnSharedPreferenceChangeListener(settingsListener);
		
		setup();
	}
	
	public static E621Middleware getInstance()
	{
		if(instance == null)
		{
			instance = new E621Middleware();
		}
		return instance;
	}
	
	public void setup()
	{
		if(!cache_path.exists())
		{
			cache_path.mkdirs();
		}
		
		if(!full_cache_path.exists())
		{
			full_cache_path.mkdirs();
		}

		if(!sd_path.exists())
		{
			sd_path.mkdirs();
		}

		if(!download_path.exists())
		{
			download_path.mkdirs();
		}
		
		if(settings.getBoolean("hideDownloadFolder", true))
		{
			File no_media = new File(download_path,".nomedia");
			
			try {
				no_media.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			File no_media = new File(download_path,".nomedia");
			
			no_media.delete();
		}
		
		if(thumb_cache == null)
		{
			thumb_cache = new ImageCacheManager(cache_path,0);
		}
		
		thumb_cache.max_size = 1024L*1024*settings.getInt("thumbnailCacheSize", 5);
		thumb_cache.clean();
		
		if(full_cache == null)
		{
			full_cache = new ImageCacheManager(full_cache_path,0);
		}
		
		full_cache.max_size = 1024L*1024*settings.getInt("fullCacheSize", 10);
		full_cache.clean();
		
		if(download_manager == null)
		{
			download_manager = new E621DownloadedImages(download_path);
		}
	}
	
	public int getFileDownloadSize()
	{
		return settings.getInt("prefferedFileDownloadSize", E621Image.SAMPLE);
	}
	
	@Override
	public E621Image post__show(String id) throws IOException
	{
		if(e621ImageCache.containsKey(id))
		{
			return e621ImageCache.get(id);
		}
		else
		{
			E621Image img = super.post__show(id);
			
			e621ImageCache.put(id, img);
			
			return img;
		}
	}
	
	@Override
	public E621Search post__index(String tags, Integer page, Integer limit) throws IOException
	{
		E621Search ret = super.post__index(tags, page, limit);
		
		if(ret != null)
		{
			for(E621Image img : ret.images)
			{
				e621ImageCache.put(img.id, img);
			}
		}
		
		return ret;
	}
	
	public boolean isSaved(E621Image img)
	{
		return download_manager.hasFile(img);
	}
	
	public void saveImage(E621Image img)
	{
		InputStream in = getImage(img,getFileDownloadSize());
		
		if(in != null)
		{
			download_manager.createOrUpdate(img, in);
		}
	}
	
	public void deleteImage(E621Image img)
	{
		download_manager.removeFile(img);
	}
	
	private byte[] getImageFromInternet(String url)
	{
		HttpResponse response = null;
		try {
			response = tryHttpGet(url,5);
		} catch (ClientProtocolException e1) {
			e1.printStackTrace();
			return null;
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		
	    StatusLine statusLine = response.getStatusLine();
	    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
	    {
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        try {
	        	response.getEntity().writeTo(out);
				out.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return null;
			}
	        
	        return out.toByteArray();
	    }
	    else
	    {
	    	return null;
	    }
	}
	
	public InputStream getImage(E621Image img, int size)
	{
		if(size == E621Image.PREVIEW)
		{
			InputStream in = download_manager.getFile(img);
			
			if(in != null)
			{
				return in;
			}
			else
			{
				in = full_cache.getFile(img.id);
				
				if(in != null)
				{
					return in;
				}
				else
				{
					in = thumb_cache.getFile(img.id);
					
					if(in != null)
					{
						return in;
					}
					else
					{
						byte[] raw_file = getImageFromInternet(img.preview_url);
						
						if(raw_file == null)
						{
							return null; 
						}
				        
				        thumb_cache.createOrUpdate(img.id, new ByteArrayInputStream(raw_file));
				        
				        return new ByteArrayInputStream(raw_file);
					}
				}
			}
		}
		
		if(size != E621Image.PREVIEW)
		{
			InputStream in = download_manager.getFile(img);
			
			if(in != null)
			{
				return in;
			}
			else
			{
				in = full_cache.getFile(img.id);
				
				if(in != null)
				{
					return in;
				}
				else
				{
					byte[] raw_file;
	
					if(size == E621Image.FULL)
					{
						raw_file = getImageFromInternet(img.file_url);
					}
					else
					{
						raw_file = getImageFromInternet(img.sample_url);
					}
					
					if(raw_file == null)
					{
						return null; 
					}
			        
					full_cache.createOrUpdate(img.id, new ByteArrayInputStream(raw_file));
			        
			        return new ByteArrayInputStream(raw_file);
				}
			}
		}
		
		return null;
	}
	
	public InputStream getDownloadedImage(String id)
	{
		return download_manager.getFile(id);
	}
	
	public void update_tags(Activity activity)
	{
		if(!updateTagsSemaphore.tryAcquire())
		{
			return;
		}
		
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(MainActivity.getContext())
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentTitle("Updating tags")
		        .setContentText("Please wait")
		        .setOngoing(true);
		
		Intent resultIntent = new Intent(activity, MainActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(activity, 0, resultIntent, 0);
		
		mBuilder.setContentIntent(pIntent);
		
		final NotificationManager mNotificationManager =
		    (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.notify(UPDATE_TAGS_NOTIFICATION_ID, mBuilder.build());
		
		new Thread(new Runnable()
		{
			@Override
			public void run() {
				try
				{
					download_manager.updateTags();
					mNotificationManager.cancel(UPDATE_TAGS_NOTIFICATION_ID);
				}
				finally
				{
					updateTagsSemaphore.release();
				}
			}
		}).start();
	}
	
	public ArrayList<String> localSearch(int page, int limit, String search)
	{
		return download_manager.search(page, limit);
	}
	
	private class E621DownloadedImages extends ImageCacheManager
	{
		public E621DownloadedImages(File base_path)
		{
			super(base_path, 0);
		}
		
		@Override
		protected synchronized void update_0_1()
		{
			super.update_0_1();
			
			db.execSQL("CREATE TABLE tags (" +
					"id TEXT PRIMARY KEY" +
				");"
			);
			
			db.execSQL("CREATE TABLE image_tags (" +
					"image TEXT" +
					", " +
					"tag TEXT" +
					", " +
					"PRIMARY KEY(image,tag)" +
					", " +
					"FOREIGN KEY (image) REFERENCES images(id)" +
					", " +
					"FOREIGN KEY (tag) REFERENCES tags(id)" +
				");"
			);
		}
		
		public ArrayList<String> search(int page, int limit)
		{
			Cursor c = db.rawQuery("SELECT id FROM images WHERE 1 ORDER BY id LIMIT ? OFFSET ?;", new String[]{String.valueOf(limit),String.valueOf(limit*page)});
			
			/*
				EXISTS(SELECT 1 FROM image_tags WHERE image=id AND tag="gay")
				AND
				(
					EXISTS(SELECT 1 FROM image_tags WHERE image=id AND tag="lucario")
					OR
					EXISTS(SELECT 1 FROM image_tags WHERE image=id AND tag="mewtwo")
				)
			 */
			
			if(c == null || !c.moveToFirst())
			{
				return new ArrayList<String>();
			}
			
			ArrayList<String> ins = new ArrayList<String>();
			
			for(;limit>0; limit--)
			{
				ins.add(c.getString(c.getColumnIndex("id")));
				
				if(!c.moveToNext())
				{
					break;
				}
			}
			
			return ins;
		}
		
		public synchronized boolean hasFile(E621Image img)
		{
			return super.hasFile(img.id + "." + img.file_ext);
		}
		
		public synchronized InputStream getFile(E621Image img)
		{
			return super.getFile(img.id + "." + img.file_ext);
		}
		
		public synchronized void removeFile(E621Image img)
		{
			String id = img.id + "." + img.file_ext;
			
			super.removeFile(id);
			
			String[] query_params = new String[]{id};
			
			db.delete("image_tags", "image = ?", query_params);
		}
		
		public synchronized void createOrUpdate(E621Image img, InputStream in)
		{
			super.createOrUpdate(img.id + "." + img.file_ext, in);
			
			for(E621Tag tag : img.tags)
			{
				ContentValues tag_values = new ContentValues();
				tag_values.put("id", tag.getTag());

				try
				{
					db.insert("tags", null, tag_values);
				}
				catch(SQLiteException e)
				{
				}
				
				ContentValues image_tag_values = new ContentValues();
				image_tag_values.put("image", img.id + "." + img.file_ext);
				image_tag_values.put("tag", tag.getTag());
				
				try
				{
					db.insert("image_tags", null, image_tag_values);
				}
				catch(SQLiteException e)
				{
				}
			}
		}
		
		protected void updateTags()
		{
			Cursor c = db.rawQuery("SELECT id FROM images;", null);

			if(!(c != null && c.moveToFirst()))
			{
				return;
			}
			
			final List<E621Image> images = Collections.synchronizedList(new ArrayList<E621Image>());
			ArrayList<Thread> threads = new ArrayList<Thread>();
			
			final Semaphore s = new Semaphore(10, true);
			
			while(true)
			{
				final String id = c.getString(c.getColumnIndex("id")).split("\\.")[0];
				
				Thread t = new Thread(new Runnable()
				{
					@Override
					public void run() {
						try {
							try {
								s.acquire();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							images.add(post__show(id));
							
							s.release();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
				
				t.start();
				
				threads.add(t);
				
				if(!c.moveToNext())
				{
					break;
				}
			}
			
			c.close();
			
			for(Thread t : threads)
			{
				try {
					t.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			updateTags(images);
		}
		
		protected synchronized void updateTags(List<E621Image> images)
		{
			db.beginTransaction();
			try
			{
				HashSet<E621Tag> tags = new HashSet<E621Tag>();
				
				for(E621Image img : images)
				{
					for(E621Tag tag : img.tags)
					{
						if(!tags.contains(tag))
						{
							tags.add(tag);
							
							ContentValues tag_values = new ContentValues();
							tag_values.put("id", tag.getTag());
		
							try
							{
								db.insert("tags", null, tag_values);
							}
							catch(SQLiteException e)
							{
							}
						}
					}
				}
				
				for(E621Image img : images)
				{
					for(E621Tag tag : img.tags)
					{
						ContentValues image_tag_values = new ContentValues();
						image_tag_values.put("image", img.id + "." + img.file_ext);
						image_tag_values.put("tag", tag.getTag());
						
						try
						{
							db.insert("image_tags", null, image_tag_values);
						}
						catch(SQLiteException e)
						{
						}
					}
				}
				
				db.setTransactionSuccessful();
			}
			finally
			{
				db.endTransaction();
			}
		}
	}
}
