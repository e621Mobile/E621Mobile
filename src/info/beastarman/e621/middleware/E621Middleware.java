package info.beastarman.e621.middleware;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.api.E621Tag;
import info.beastarman.e621.backend.ImageCacheManager;
import info.beastarman.e621.frontend.DownloadsActivity;
import info.beastarman.e621.frontend.MainActivity;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlarmManager;
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

public class E621Middleware extends E621
{
	HashMap<String,E621Image> e621ImageCache = new HashMap<String,E621Image>();
	
	File cache_path = null;
	File full_cache_path = null;
	File sd_path = null;
	File download_path = null;
	File export_path = null;
	File report_path = null;
	
	public static final String PREFS_NAME = "E621MobilePreferences";
	
	SharedPreferences settings;
	SharedPreferences.OnSharedPreferenceChangeListener settingsListener;
	
	HashSet<String> allowedRatings = new HashSet<String>(); 
	
	ImageCacheManager thumb_cache;
	ImageCacheManager full_cache;
	E621DownloadedImages download_manager;
	
	private static int UPDATE_TAGS_NOTIFICATION_ID = 1;
	private static int SAVE_IMAGES_NOTIFICATION_ID = 2;
	private Semaphore updateTagsSemaphore = new Semaphore(1);
	
	private static E621Middleware instance;
	
	private static String DIRECTORY_SYNC = "sync/";
	
	private AlarmManager alarmMgr;
	private PendingIntent alarmIntent;
	
	Context ctx;
	
	protected E621Middleware(Context ctx)
	{
		this.ctx = ctx;
		
		cache_path = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"cache/");
		full_cache_path = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"full_cache/");
		sd_path = new File(Environment.getExternalStorageDirectory(),"e621/");
		download_path = new File(sd_path,"e612 Images/");
		export_path = new File(sd_path,"export/");
		report_path = new File(ctx.getExternalFilesDir(DIRECTORY_SYNC),"reports/");
		
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
	
	public static E621Middleware getInstance(Context ctx)
	{
		if(instance == null)
		{
			instance = new E621Middleware(ctx);
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
		
		if(!export_path.exists())
		{
			export_path.mkdirs();
		}
		
		if(!report_path.exists())
		{
			report_path.mkdirs();
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
		
		HashSet<String> allowedRatingsTemp = (HashSet<String>) settings.getStringSet("allowedRatings",new HashSet<String>());
		allowedRatings.clear();
		
		if(allowedRatingsTemp.contains(E621Image.SAFE))
		{
			allowedRatings.add(E621Image.SAFE);
		}
		if(allowedRatingsTemp.contains(E621Image.QUESTIONABLE))
		{
			allowedRatings.add(E621Image.QUESTIONABLE);
		}
		if(allowedRatingsTemp.contains(E621Image.EXPLICIT))
		{
			allowedRatings.add(E621Image.EXPLICIT);
		}
		
		Intent intent = new Intent(ctx, E621SyncReciever.class);
		
		alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		alarmIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);
		
		alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
		        AlarmManager.INTERVAL_HOUR*3,
		        AlarmManager.INTERVAL_HOUR*3, alarmIntent);
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
		tags = prepareQuery(tags);
		
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
	
	private NotificationManager saveImageNotificationManager;
	private Semaphore saveImageSemaphore = new Semaphore(1);
	private ArrayList<E621Image> downloading = new ArrayList<E621Image>();
	
	public void saveImageAsync(final E621Image img, Activity activity, final Runnable after_download)
	{
		try {
			saveImageSemaphore.acquire();
			
			if(saveImageNotificationManager == null)
			{
				saveImageNotificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
			}
			
			if(downloading.contains(img))
			{
				return;
			}
			
			if(downloading.size() == 0)
			{
				NotificationCompat.Builder mBuilder =
				        new NotificationCompat.Builder(ctx)
				        .setSmallIcon(R.drawable.ic_launcher)
				        .setContentTitle("Downloading images")
				        .setContentText("Please wait")
				        .setOngoing(true);
				
				Intent resultIntent = new Intent(activity, DownloadsActivity.class);
				PendingIntent pIntent = PendingIntent.getActivity(activity, 0, resultIntent, 0);
				
				mBuilder.setContentIntent(pIntent);
				
				saveImageNotificationManager.notify(SAVE_IMAGES_NOTIFICATION_ID,mBuilder.build());
			}
			
			downloading.add(img);
		} catch (InterruptedException e) {
			return;
		}
		finally
		{
			saveImageSemaphore.release();
		}
		
		new Thread(new Runnable()
		{
			@Override
			public void run() {
				saveImage(img);
				
				if(after_download != null)
				{
					after_download.run();
				}
				
				try {
					saveImageSemaphore.acquire();
					
					downloading.remove(img);
					
					if(downloading.size() == 0)
					{
						saveImageNotificationManager.cancel(SAVE_IMAGES_NOTIFICATION_ID);
					}
				}
				catch(InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				finally
				{
					saveImageSemaphore.release();
				}
			}
		}).start();
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
		        new NotificationCompat.Builder(ctx)
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
	
	private String prepareQuery(String tags)
	{
		tags = new SearchQuery(tags).normalize();
		
		String[] tt = tags.split("\\s");
		boolean specific = false;
		
		for(String t : tt)
		{
			if(t.startsWith("rating:"))
			{
				specific = true;
				break;
			}
		}
		
		if(!specific)
		{
			if(allowedRatings.size() == 1)
			{
				if(allowedRatings.contains(E621Image.SAFE))
				{
					tags = tags + " rating:" + E621Image.SAFE;
				}
				else if(allowedRatings.contains(E621Image.QUESTIONABLE))
				{
					tags = tags + " rating:" + E621Image.QUESTIONABLE;
				}
				else if(allowedRatings.contains(E621Image.EXPLICIT))
				{
					tags = tags + " rating:" + E621Image.EXPLICIT;
				}
			}
			else if(allowedRatings.size() == 2)
			{
				if(!allowedRatings.contains(E621Image.SAFE))
				{
					tags = tags + " -rating:" + E621Image.SAFE;
				}
				else if(!allowedRatings.contains(E621Image.QUESTIONABLE))
				{
					tags = tags + " -rating:" + E621Image.QUESTIONABLE;
				}
				else if(!allowedRatings.contains(E621Image.EXPLICIT))
				{
					tags = tags + " -rating:" + E621Image.EXPLICIT;
				}
			}
		}
		
		return tags;
	}
	
	public ArrayList<E621DownloadedImage> localSearch(int page, int limit, String tags)
	{
		tags = prepareQuery(tags);
		
		return download_manager.search(page, limit, new SearchQuery(tags));
	}
	
	public void export(String search)
	{
		search = prepareQuery(search);
		
		SearchQuery sq = new SearchQuery(search);
		
		ArrayList<E621DownloadedImage> ids = download_manager.search(0, Integer.MAX_VALUE, sq);
		
		final Semaphore sem = new Semaphore(10);
		final File path;
		if(sq.normalize().length() > 0)
		{
			path = new File(export_path,sq.normalize().replace(":", ".."));
		}
		else
		{
			path = new File(export_path,"all_images_");
		}
		
		if(!path.exists())
		{
			path.mkdirs();
		}
		
		ArrayList<Thread> threads = new ArrayList<Thread>();
		
		for(E621DownloadedImage image : ids)
		{
			final String s = image.filename;
			
			Thread t = new Thread(new Runnable()
			{
				@Override
				public void run() {
					try {
						sem.acquire();
						
						File f = new File(path,s);
						
						InputStream in = download_manager.getFile(s);
						
						try {
							BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
							out.write(IOUtils.toByteArray(in));
							out.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						finally
						{
							try {
								in.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						
						return;
					}
					finally
					{
						sem.release();
					}
				}
			});
			
			t.start();
			
			threads.add(t);
		}
		
		for(Thread t : threads)
		{
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void removeExported(String search)
	{
		search = prepareQuery(search);
		
		SearchQuery sq = new SearchQuery(search);
		final File f;
		if(sq.normalize().length() > 0)
		{
			f = new File(export_path,sq.normalize().replace(":", ".."));
		}
		else
		{
			f = new File(export_path,"all_images_");
		}
		
		if(f.exists())
		{
			for(String s : f.list())
			{
				new File(f,s).delete();
			}
			
			f.delete();
		}
	}
	
	public boolean wasExported(String search)
	{
		search = prepareQuery(search);
		
		SearchQuery sq = new SearchQuery(search);
		final File path;
		if(sq.normalize().length() > 0)
		{
			path = new File(export_path,sq.normalize().replace(":", ".."));
		}
		else
		{
			path = new File(export_path,"all_images_");
		}
		
		if(path.exists())
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public int pages(int results_per_page, String query)
	{
		query = prepareQuery(query);
		
		return (int) Math.ceil(((double)download_manager.totalEntries(new SearchQuery(query))) / results_per_page);
	}
	
	public void sync()
	{
		for(String file : report_path.list())
		{
			File report = new File(report_path,file);
			
			try
			{
				FileInputStream in = new FileInputStream(report);
				
				sendReportOnline(IOUtils.toString(in));
				
				in.close();
				report.delete();
			}
			catch (FileNotFoundException e)
			{
			}
			catch(ClientProtocolException e)
			{
			}
			catch (IOException e)
			{
			}
		}
	}
	
	public void sendReport(final String report)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				String report_trim = report.trim();
				
				try
				{
					sendReportOnline(report_trim);
				}
				catch(ClientProtocolException e)
				{
					saveReportForLater(report_trim);
				}
				catch (IOException e)
				{
					saveReportForLater(report_trim);
				}
			}
		}).start();
	}
	
	private void sendReportOnline(String report) throws ClientProtocolException, IOException
	{
		HttpClient httpclient = new DefaultHttpClient();
		
		HttpPost post = new HttpPost("http://beastarman.info/report/e621/");
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);  
		nameValuePairs.add(new BasicNameValuePair("text", report));
		post.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
		
		httpclient.execute(post);
	}
	
	private void saveReportForLater(String report)
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
        String currentTimeStamp = dateFormat.format(new Date());
		
		File report_file = new File(report_path,currentTimeStamp + ".txt");
		
		try
		{
			report_file.createNewFile();
			
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(report_file));
			out.write(report.getBytes());
			out.close();
		}
		catch (IOException e)
		{
		}
	}
	
	private class E621DownloadedImages extends ImageCacheManager
	{
		public E621DownloadedImages(File base_path)
		{
			super(base_path, 0);
			
			setVersion(3);
		}
		
		@Override
		protected synchronized void update_db(int version)
		{
			super.update_db(version);
			
			switch(version)
			{
				case 1:
					update_0_1();
					break;
				case 2:
					update_1_2();
					break;
				case 3:
					update_2_3();
					break;
			}
		}
		
		protected synchronized void update_0_1()
		{
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
		
		protected synchronized void update_1_2()
		{
			db.execSQL("CREATE TABLE e621image (" +
					"image TEXT PRIMARY KEY" +
					", " +
					"rating VARCHAR(1)" +
					", " +
					"FOREIGN KEY (image) REFERENCES images(id)" +
				");"
			);
		}
		
		protected synchronized void update_2_3()
		{
			db.execSQL("ALTER TABLE e621image ADD COLUMN width INTEGER DEFAULT 1;");
			db.execSQL("ALTER TABLE e621image ADD COLUMN height INTEGER DEFAULT 1;");
		}
		
		public ArrayList<E621DownloadedImage> search(int page, int limit, SearchQuery query)
		{
			Cursor c = db.rawQuery("SELECT images.id as id, e621image.width as width, e621image.height as height FROM images INNER JOIN e621image ON e621image.image = images.id WHERE " + query.toSql() + " ORDER BY id LIMIT ? OFFSET ?;", new String[]{String.valueOf(limit),String.valueOf(limit*page)});
			
			if(c == null || !c.moveToFirst())
			{
				return new ArrayList<E621DownloadedImage>();
			}
			
			ArrayList<E621DownloadedImage> ins = new ArrayList<E621DownloadedImage>();
			
			for(;limit>0; limit--)
			{
				ins.add(new E621DownloadedImage(
						c.getString(c.getColumnIndex("id")),
						c.getInt(c.getColumnIndex("width")),
						c.getInt(c.getColumnIndex("height"))
					));
				
				if(!c.moveToNext())
				{
					break;
				}
			}
			
			return ins;
		}
		
		public int totalEntries(SearchQuery query)
		{
			Cursor c = db.rawQuery("SELECT COUNT(*) as c FROM images WHERE " + query.toSql() + ";", null);
			
			if(c == null || !c.moveToFirst())
			{
				return 0;
			}
			
			return c.getInt(c.getColumnIndex("c"));
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
			
			ContentValues e621image_values = new ContentValues();
			e621image_values.put("image", img.id + "." + img.file_ext);
			e621image_values.put("rating", img.rating);
			e621image_values.put("width", img.width);
			e621image_values.put("height", img.height);
			
			try
			{
				db.insert("e621image", null, e621image_values);
			}
			catch(SQLiteException e)
			{
			}
			
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
					ContentValues e621image_values = new ContentValues();
					e621image_values.put("image", img.id + "." + img.file_ext);
					e621image_values.put("rating", img.rating);
					e621image_values.put("width", img.width);
					e621image_values.put("height", img.height);
					
					try
					{
						if(db.insert("e621image", null, e621image_values) == -1)
						{
							db.update("e621image", e621image_values, "image = ?", new String[]{img.id + "." + img.file_ext});
						}
					}
					catch(SQLiteException e)
					{
					}
					
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
