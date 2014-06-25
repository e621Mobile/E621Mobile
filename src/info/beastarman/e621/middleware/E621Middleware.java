package info.beastarman.e621.middleware;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.api.E621Tag;
import info.beastarman.e621.api.E621TagAlias;
import info.beastarman.e621.api.E621Vote;
import info.beastarman.e621.backend.ImageCacheManager;
import info.beastarman.e621.backend.Pair;
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
import java.util.Arrays;
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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;

public class E621Middleware extends E621
{
	HashMap<String,E621Image> e621ImageCache = new HashMap<String,E621Image>();
	HashMap<String,Integer> searchCount = new HashMap<String,Integer>();
	
	File cache_path = null;
	File full_cache_path = null;
	File sd_path = null;
	File download_path = null;
	File export_path = null;
	File report_path = null;
	File interrupted_path = null;
	FailedDownloadManager failed_download_manager = null;
	
	InterruptedSearchManager interrupt;
	
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
	private static String DIRECTORY_MISC = "misc/";
	
	private AlarmManager alarmMgr;
	private PendingIntent alarmIntent;
	
	private String login = null;
	private String password_hash = null;
	
	public static final String LOG_TAG = "E621MobileLogging";
	
	Context ctx;
	
	protected E621Middleware(Context new_ctx)
	{
		if(new_ctx != null)
		{
			this.ctx = new_ctx;
		}
		
		cache_path = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"cache/");
		full_cache_path = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"full_cache/");
		sd_path = new File(Environment.getExternalStorageDirectory(),"e621/");
		download_path = new File(sd_path,"e612 Images/");
		export_path = new File(sd_path,"export/");
		report_path = new File(ctx.getExternalFilesDir(DIRECTORY_SYNC),"reports/");
		interrupted_path = new File(ctx.getExternalFilesDir(DIRECTORY_MISC),"interrupt/");
		
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
		return getInstance(null);
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
		
		if(!interrupted_path.exists())
		{
			interrupted_path.mkdirs();
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
		
		File failed_download_file = new File(ctx.getExternalFilesDir(DIRECTORY_SYNC),"failed_downloads.txt");
		
		if(!failed_download_file.exists())
		{
			failed_download_file.mkdirs();
			try {
				failed_download_file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(failed_download_manager == null)
		{
			failed_download_manager = new FailedDownloadManager(failed_download_file);
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
		
		interrupt = new InterruptedSearchManager(interrupted_path);
		
		String savedLogin = settings.getString("userLogin",null);
		String savedPasswordHash = settings.getString("userPasswordHash",null);
		
		if(savedLogin!=null && savedPasswordHash!=null)
		{
			login = savedLogin;
			password_hash = savedPasswordHash;
		}
	}
	
	@Override
	protected HttpResponse tryHttpGet(String url, Integer tries) throws ClientProtocolException, IOException
	{
		android.util.Log.i(LOG_TAG,"GET " + url);
		
		return super.tryHttpGet(url, tries);
	}
	
	@Override
	protected HttpResponse tryHttpPost(String url, List<NameValuePair> pairs, Integer tries) throws ClientProtocolException, IOException
	{
		android.util.Log.i(LOG_TAG,"POST " + url);
		
		return super.tryHttpPost(url, pairs, tries);
	}
	
	public boolean playGifs()
	{
		return settings.getBoolean("playGifs", true);
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
			
			e621ImageCache.put(img.id, img);
			
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
			
			searchCount.put(tags, ret.count);
		}
		
		return ret;
	}
	
	public Integer getSearchResultsCount(String tags)
	{
		tags = prepareQuery(tags);
		
		if(searchCount.containsKey(tags))
		{
			return searchCount.get(tags);
		}
		else
		{
			return null;
		}
	}
	
	public Integer getSearchResultsCountForce(String tags) throws IOException
	{
		tags = prepareQuery(tags);
		
		if(searchCount.containsKey(tags))
		{
			return searchCount.get(tags);
		}
		else
		{
			int temp = post__index(tags,0,1).count;
			
			searchCount.put(tags, temp);
			
			return temp;
		}
	}
	
	public Integer getSearchResultsPages(String tags, int results_per_page)
	{
		Integer count = getSearchResultsCount(tags);
		
		if(count == null)
		{
			return null;
		}
		
		return (int) Math.ceil(((double)count)/((double)results_per_page));
	}
	
	public Integer getSearchContinueResultsPages(String tags, int results_per_page)
	{
		Pair<String,String> pair = interrupt.getSearch(tags);
		
		if(pair == null)
		{
			return null;
		}
		
		String search_new = tags + " id:>" + pair.right + " order:id";
		String search_old = tags + " id:<" + pair.left;
		
		Integer count_new = getSearchResultsCount(search_new);
		Integer count_old = getSearchResultsCount(search_old);
		
		if(count_new == null || count_old == null)
		{
			return null;
		}
		
		int count = count_new + count_old;
		
		return (int) Math.ceil(((double)count)/((double)results_per_page));
	}
	
	public boolean isSaved(E621Image img)
	{
		return download_manager.hasFile(img);
	}
	
	private NotificationManager saveImageNotificationManager;
	private Semaphore saveImageSemaphore = new Semaphore(1);
	private ArrayList<E621Image> downloading = new ArrayList<E621Image>();
	
	public void saveImageAsync(final E621Image img, final Runnable success_callback, final Runnable error_callback, final boolean notificate)
	{
		failed_download_manager.addFile(img.id);
		
		if(notificate)
		{
			try {
				saveImageSemaphore.acquire();
				
				if(saveImageNotificationManager == null)
				{
					saveImageNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
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
					
					Intent resultIntent = new Intent(ctx, DownloadsActivity.class);
					PendingIntent pIntent = PendingIntent.getActivity(ctx, 0, resultIntent, 0);
					
					mBuilder.setContentIntent(pIntent);
					
					saveImageNotificationManager.notify(SAVE_IMAGES_NOTIFICATION_ID,mBuilder.build());
				}
				
				downloading.add(img);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			finally
			{
				saveImageSemaphore.release();
			}
		}
		
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				boolean successfull = saveImage(img);
				
				if(successfull)
				{
					if(success_callback != null)
					{
						success_callback.run();
					}
					
					failed_download_manager.removeFile(img.id);
				}
				else
				{
					if(error_callback != null)
					{
						error_callback.run();
					}
				}
				
				if(notificate)
				{
					try
					{
						saveImageSemaphore.acquire();
						
						downloading.remove(img);
						
						if(downloading.size() == 0)
						{
							saveImageNotificationManager.cancel(SAVE_IMAGES_NOTIFICATION_ID);
						}
					}
					catch(InterruptedException e)
					{
						e.printStackTrace();
					}
					finally
					{
						saveImageSemaphore.release();
					}
				}
			}
		}).start();
	}
	
	public boolean saveImage(final E621Image img)
	{
		final InputStream in = getImage(img,getFileDownloadSize());
		
		if(in != null)
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					download_manager.createOrUpdate(img, in);
				}
			}).start();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public void deleteImage(E621Image img)
	{
		download_manager.removeFile(img);
	}
	
	Semaphore getImageSemaphore = new Semaphore(10);
	
	private byte[] getImageFromInternet(String url)
	{
		try
		{
			getImageSemaphore.acquire();
			
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
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return null;
		}
		finally
		{
			getImageSemaphore.release();
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
					download_manager.updateMetadata();
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
		
		for(String file : failed_download_manager.getFiles())
		{
			E621Image img = null;
			try {
				img = post__show(file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
			
			if(img != null)
			{
				saveImageAsync(img,null,null,true);
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
	
	public Boolean post_favorite(int id, boolean create)
	{
		if(create)
		{
			return favorite__create(id,login,password_hash);
		}
		else
		{
			return favorite__destroy(id,login,password_hash);
		}
	}
	
	public E621Vote post__vote(int id, boolean up, String login, String password_hash)
	{
		E621Vote ret = super.post__vote(id, up, login, password_hash);
		
		String sid = String.valueOf(id);
		
		if(ret != null && ret.success && e621ImageCache.containsKey(sid))
		{
			E621Image img = e621ImageCache.get(sid);
			img.score = ret.score;
			e621ImageCache.put(sid, img);
		}
		
		return ret;
	}
	
	public E621Vote post__vote(int id, boolean up)
	{
		return post__vote(id,up,login,password_hash);
	}
	
	public Boolean comment__create(int id, String body)
	{
		return comment__create(id,body,login,password_hash);
	}
	
	public boolean login(String name, String password, boolean remember)
	{
		password_hash = user__login(name,password);
		
		if(password_hash != null)
		{
			login = name;
			
			if(remember)
			{
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("userLogin", login);
				editor.putString("userPasswordHash", password_hash);
				editor.commit();
			}
			
			return true;
		}
		else
		{
			login = null;
			return false;
		}
	}
	
	public void logout()
	{
		password_hash = null;
		login = null;
		
		SharedPreferences.Editor editor = settings.edit();
		editor.remove("userLogin");
		editor.remove("userPasswordHash");
		editor.commit();
	}
	
	public String getLoggedUser()
	{
		return login;
	}
	
	public boolean isLoggedIn()
	{
		return getLoggedUser() != null;
	}
	
	public void continue_later(String search, String seen_past, String seen_until)
	{
		search= prepareQuery(search);
		
		interrupt.addOrUpdateSearch(search, seen_past, seen_until);
	}
	
	public Pair<String,String> get_continue_ids(String search)
	{
		search = prepareQuery(search);
		
		return interrupt.getSearch(search);
	}
	
	public ArrayList<String> getAllSearches()
	{
		return interrupt.getAllSearches();
	}
	
	public void removeSearch(String search)
	{
		search = prepareQuery(search);
		
		interrupt.remove(search);
	}
	
	HashMap<Pair<String,Integer>,E621Search> continue_cache = new HashMap<Pair<String,Integer>,E621Search>();
	
	@SuppressWarnings("unchecked")
	public E621Search continue_search(String search, int page, int limit) throws IOException
	{
		Pair<String,String> pair = interrupt.getSearch(search);
		
		String search_new = search + " id:>" + pair.right + " order:id";
		String search_old = search + " id:<" + pair.left;
		
		int total_new = getSearchResultsCountForce(search_new);
		int total_old = getSearchResultsCountForce(search_old);
		
		if((page+1)*limit < total_new) // All new
		{
			E621Search results = post__index(search_new,page,limit);
			results.count = total_new + total_old;
			return results;
		}
		else if(page*limit < total_new) // Some new, some not
		{
			E621Search new_results = post__index(search_new,page,limit);
			E621Search old_results = post__index(search_old,0,limit);
			
			continue_cache.put(new Pair<String,Integer>(search,0), old_results);
			
			ArrayList<E621Image> images = (ArrayList<E621Image>) new_results.images.clone();
			images.addAll(old_results.images.subList(0, Math.min(limit - images.size(), old_results.images.size())));
			
			return new E621Search(images,page*limit,total_new+total_old,limit);
		}
		else if(page*limit < (total_new + total_old)) // Some old
		{
			int old_offset = (page*limit) - total_new;
			
			if(old_offset%limit == 0)
			{
				E621Search temp = post__index(search_old,old_offset/limit,limit);
				return new E621Search(temp.images,page*limit,total_new+total_old,limit);
			}
			else
			{
				E621Search first;
				E621Search second;
				int first_page = (int)Math.floor(old_offset/limit);
				int second_page = first_page+1;
				
				if(continue_cache.containsKey(new Pair<String,Integer>(search,first_page)))
				{
					first = continue_cache.get(new Pair<String,Integer>(search,first_page));
				}
				else
				{
					first = post__index(search_old,first_page,limit);
					continue_cache.put(new Pair<String,Integer>(search,first_page), first);
				}
				
				ArrayList<E621Image> first_images = new ArrayList<E621Image>(first.images.subList(old_offset%limit, Math.min(limit,first.images.size())));
				
				if(!first.has_next_page())
				{
					return new E621Search(
							first_images,
							page*limit,
							total_new+total_old,
							limit);
				}
				
				if(continue_cache.containsKey(new Pair<String,Integer>(search,second_page)))
				{
					second = continue_cache.get(new Pair<String,Integer>(search,second_page));
				}
				else
				{
					second = post__index(search_old,second_page,limit);
					continue_cache.put(new Pair<String,Integer>(search,second_page), second);
				}
				
				first_images.addAll(second.images.subList(0, limit - first_images.size()));
				
				return new E621Search(
						first_images,
						page*limit,
						total_new+total_old,
						limit);
			}
		}
		else
		{
			return new E621Search(
					new ArrayList<E621Image>(),
					page*limit,
					total_new+total_old,
					limit);
		}
	}
	
	public E621Tag getTag(String name)
	{
		return download_manager.getTag(name);
	}
	
	private class FailedDownloadManager
	{
		File file;
		
		public FailedDownloadManager(File file)
		{
			this.file = file;
		}
		
		public synchronized ArrayList<String> getFiles()
		{
			FileInputStream in;
			try {
				in = new FileInputStream(file);
				
				String temp = IOUtils.toString(in).trim();
				
				if(temp.length() == 0)
				{
					return new ArrayList<String>();
				}
				
				ArrayList<String> ret = new ArrayList<String>(Arrays.asList(temp.split("\\s+")));
				
				in.close();
				
				return ret;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new ArrayList<String>();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new ArrayList<String>();
			}
		}
		
		public synchronized void setFiles(ArrayList<String> strings)
		{
			String listString = "";

			for (String s : strings)
			{
			    listString += s + "\n";
			}
			
			BufferedOutputStream out;
			try {
				out = new BufferedOutputStream(new FileOutputStream(file));
				out.write(listString.getBytes());
				out.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public synchronized boolean hasFile(String file)
		{
			return getFiles().contains(file);
		}
		
		public synchronized void addFile(String file)
		{
			if(!hasFile(file))
			{
				ArrayList<String> files = getFiles();
				files.add(file);
				setFiles(files);
			}
		}
		
		public synchronized void removeFile(String file)
		{
			if(hasFile(file))
			{
				ArrayList<String> files = getFiles();
				files.remove(file);
				setFiles(files);
			}
		}
	}
	
	private class E621DownloadedImages extends ImageCacheManager
	{
		public E621DownloadedImages(File base_path)
		{
			super(base_path, 0);
			
			SQLiteDatabase db = getDB();
			
			setVersion(5,db);
			
			db.close();
		}
		
		@Override
		protected synchronized void update_db(int version, SQLiteDatabase db)
		{
			super.update_db(version, db);
			
			switch(version)
			{
				case 1:
					update_0_1(db);
					break;
				case 2:
					update_1_2(db);
					break;
				case 3:
					update_2_3(db);
					break;
				case 4:
					update_3_4(db);
					break;
				case 5:
					update_4_5(db);
					break;
			}
		}
		
		protected synchronized void update_0_1(SQLiteDatabase db)
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
		
		protected synchronized void update_1_2(SQLiteDatabase db)
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
		
		protected synchronized void update_2_3(SQLiteDatabase db)
		{
			db.execSQL("ALTER TABLE e621image ADD COLUMN width INTEGER DEFAULT 1;");
			db.execSQL("ALTER TABLE e621image ADD COLUMN height INTEGER DEFAULT 1;");
		}
		
		protected synchronized void update_3_4(SQLiteDatabase db)
		{
			db.execSQL("ALTER TABLE tags ADD COLUMN name TEXT DEFAULT '';");
			
			db.execSQL("CREATE TABLE tag_alias (" +
					"alias TEXT" +
					", " +
					"id TEXT" +
					", " +
					"tag TEXT" +
					", " +
					"PRIMARY KEY(alias)" +
					", " +
					"FOREIGN KEY (tag) REFERENCES tags(id)" +
				");"
			);
			
			db.execSQL("DROP TABLE image_tags;");
			
			db.execSQL("CREATE TABLE image_tag (" +
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
			
			db.execSQL("DELETE FROM tags WHERE 1;");
		}
		
		protected synchronized void update_4_5(SQLiteDatabase db)
		{
			db.execSQL("ALTER TABLE tags ADD COLUMN type INTEGER DEFAULT " + String.valueOf(E621Tag.GENERAL) + ";");
			
			db.execSQL("DELETE FROM tags WHERE 1;");
		}
		
		public E621Tag getTag(String name)
		{
			SQLiteDatabase db = getDB();
			
			Cursor c = db.rawQuery("SELECT id, name, type FROM tags WHERE name = ? LIMIT 1;", new String[]{name});
			
			if(c == null || !c.moveToFirst())
			{
				if(c != null) c.close();
				
				return null;
			}
			
			E621Tag tag = new E621Tag(name,Integer.valueOf(c.getString(c.getColumnIndex("id"))),null,c.getInt(c.getColumnIndex("type")),null);
			
			c.close();
			
			db.close();
			
			return tag;
		}
		
		public ArrayList<E621DownloadedImage> search(int page, int limit, SearchQuery query)
		{
			String sqlQuery = toSql(query);
			
			SQLiteDatabase db = getDB();
			
			try
			{
				Cursor c = db.rawQuery("SELECT images.id as id, e621image.width as width, e621image.height as height FROM images INNER JOIN e621image ON e621image.image = images.id WHERE " + sqlQuery + " ORDER BY id LIMIT ? OFFSET ?;", new String[]{String.valueOf(limit),String.valueOf(limit*page)});
				
				if(c == null || !c.moveToFirst())
				{
					if(c != null) c.close();
					
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
				
				c.close();
				
				return ins;
			}
			finally
			{
				db.close();
			}
		}
		
		public int totalEntries(SearchQuery query)
		{
			String sqlQuery = toSql(query);
			
			SQLiteDatabase db = getDB();
			Cursor c = null;
			
			try
			{
				c = db.rawQuery("SELECT COUNT(*) as c FROM images WHERE " + sqlQuery + ";", null);
				
				if(c == null || !c.moveToFirst())
				{
					return 0;
				}
				
				return c.getInt(c.getColumnIndex("c"));
			}
			finally
			{
				if(c != null) c.close();
				db.close();
			}
		}
		
		public synchronized boolean hasFile(E621Image img)
		{
			if(img == null) return false;
			
			return super.hasFile(img.id + "." + img.file_ext);
		}
		
		public synchronized InputStream getFile(E621Image img)
		{
			if(img == null) return null;
			
			return super.getFile(img.id + "." + img.file_ext);
		}
		
		public synchronized void removeFile(E621Image img)
		{
			if(img == null) return;
			
			String id = img.id + "." + img.file_ext;
			
			super.removeFile(id);
			
			String[] query_params = new String[]{id};
			
			SQLiteDatabase db = getDB();
			
			db.delete("image_tag", "image = ?", query_params);
			
			db.close();
		}
		
		public synchronized void createOrUpdate(E621Image img, InputStream in)
		{
			if(img == null) return;
			
			super.createOrUpdate(img.id + "." + img.file_ext, in);
			
			SQLiteDatabase db = getDB();
			
			try
			{
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
				
				int i=0;
				
				for(i=0; i<img.tags.size(); i++)
				{
					E621Tag tag = img.tags.get(i);
					
					Cursor c = db.rawQuery("SELECT id FROM tags WHERE name = ?;", new String[]{tag.getTag()});
					
					if(c == null || !c.moveToFirst())
					{
						ArrayList<E621Tag> loaded_tag = tag__index(1,1,null,null,null,tag.getTag(),null);
						
						if(loaded_tag.size() < 1)
						{
							continue;
						}
						else
						{
							tag = loaded_tag.get(0);
							img.tags.set(i,tag);
						}
					}
					else
					{
						tag.setId(c.getInt(c.getColumnIndex("id")));
						img.tags.set(i,tag);
					}
					
					ContentValues image_tag_values = new ContentValues();
					image_tag_values.put("image", img.id + "." + img.file_ext);
					image_tag_values.put("tag", tag.getId());
					
					try
					{
						db.insert("image_tag", null, image_tag_values);
					}
					catch(SQLiteException e)
					{
					}
				}
			}
			finally
			{
				db.close();
			}
		}
		
		public synchronized void updateMetadata()
		{
			android.util.Log.i(LOG_TAG,"Starting Tag sync");
			updateTagBase();
			
			android.util.Log.i(LOG_TAG,"Starting Tag Alias sync");
			updateTagAliasBase();
			
			android.util.Log.i(LOG_TAG,"Starting Image Tag sync");
			updateImageTags();
		}
		
		protected void updateTagAliasBase()
		{
			SQLiteDatabase db = getDB();
			
			try
			{
				Cursor c = db.rawQuery("SELECT MAX(CAST(id AS INTEGER)) as max_id FROM tag_alias;", null);
				
				Integer max_id = null;
				
				if(c != null && c.moveToFirst())
				{
					max_id = c.getInt(c.getColumnIndex("max_id"));
				}
				
				if(c != null) c.close();
				
				int page = 0;
				boolean stop = false;
				ArrayList<E621TagAlias> tag_aliases;
				
				do
				{
					db.beginTransaction();
					
					try
					{
						stop = false;
						
						tag_aliases = tag_alias__index(true,"date",page);
						
						for(E621TagAlias tag_alias : tag_aliases)
						{
							if(tag_alias.alias_id > max_id)
							{
								ContentValues values = new ContentValues();
								values.put("alias", tag_alias.name);
								values.put("id", tag_alias.alias_id);
								values.put("tag", tag_alias.id);
								
								db.insert("tag_alias", null, values);
							}
							else
							{
								stop = true;
							}
						}
						
						page++;
						
						db.setTransactionSuccessful();
					}
					finally
					{
						db.endTransaction();
					}
				}
				while(!stop && (tag_aliases.size() > 0));
			}
			finally
			{
				db.close();
			}
		}
		
		private HashMap<String,String> getAllTags()
		{
			SQLiteDatabase db = getDB();
			
			db.beginTransaction();
			try
			{
				Cursor c = db.rawQuery("SELECT id,name FROM tags;", null);
				
				HashMap<String,String> ret = new HashMap<String,String>();
				
				if(c == null || !c.moveToFirst())
				{
					if(c != null) c.close();
					
					return ret; 
				}
				
				while(!c.isClosed())
				{
					ret.put(c.getString(c.getColumnIndex("name")), c.getString(c.getColumnIndex("id")));
					
					if(!c.moveToNext())
					{
						break;
					}
				}
				
				c.close();
				
				db.setTransactionSuccessful();
				
				return ret;
			}
			finally
			{
				db.endTransaction();
			}
		}
		
		protected void updateTagBase()
		{
			SQLiteDatabase db = getDB();
			
			try
			{
				Cursor c = db.rawQuery("SELECT MAX(CAST(id AS INTEGER)) as max_id FROM tags;", null);
				
				Integer max_id = null;
				
				if(c != null && c.moveToFirst())
				{
					max_id = c.getInt(c.getColumnIndex("max_id"));
				}
				
				if(c != null) c.close();
				
				int page = 0;
				ArrayList<E621Tag> tags;
				
				do
				{
					db.beginTransaction();
					
					try
					{
						tags = tag__index(10000,page,null,null,max_id,null,null);
						
						for(E621Tag tag : tags)
						{
							ContentValues values = new ContentValues();
							values.put("name", tag.getTag());
							values.put("id", tag.getId());
							values.put("type", tag.type);
							
							db.insert("tags", null, values);
						}
						
						page++;
						
						db.setTransactionSuccessful();
					}
					finally
					{
						db.endTransaction();
					}
				}
				while(tags.size() == 10000);
			}
			finally
			{
				db.close();
			}
		}
		
		protected void updateImageTags()
		{
			final List<E621Image> images = Collections.synchronizedList(new ArrayList<E621Image>());
			SQLiteDatabase db = getDB();
			
			try
			{
				Cursor c = db.rawQuery("SELECT image FROM e621image;", null);
	
				if(!(c != null && c.moveToFirst()))
				{
					if(c != null) c.close();
					
					return;
				}
				
				ArrayList<Thread> threads = new ArrayList<Thread>();
				
				final Semaphore s = new Semaphore(10, true);
				
				while(true)
				{
					final String id = c.getString(c.getColumnIndex("image")).split("\\.")[0];
					
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
			}
			finally
			{
				db.close();
			}
			
			updateImageTags(images);
		}
		
		protected synchronized void updateImageTags(List<E621Image> images)
		{
			HashMap<String,String> tagMap = getAllTags();
			
			SQLiteDatabase db = getDB();
			db.beginTransaction();
			try
			{
				for(E621Image img : images)
				{
					ContentValues e621image_values = new ContentValues();
					e621image_values.put("image", img.id + "." + img.file_ext);
					e621image_values.put("rating", img.rating);
					e621image_values.put("width", img.width);
					e621image_values.put("height", img.height);
					
					try
					{
						db.update("e621image", e621image_values, "image = ?", new String[]{img.id + "." + img.file_ext});
					}
					catch(SQLiteException e)
					{
					}
					
					for(E621Tag tag : img.tags)
					{
						ContentValues image_tag_values = new ContentValues();
						image_tag_values.put("image", img.id + "." + img.file_ext);
						image_tag_values.put("tag", tagMap.get(tag.getTag()));
						
						try
						{
							db.insert("image_tag", null, image_tag_values);
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
				db.close();
			}
		}
		
		public String antiAlias(String alias)
		{
			String ret = alias;
			
			SQLiteDatabase db = getDB();
			
			try
			{
				Cursor c = db.rawQuery("SELECT id FROM tag_alias WHERE alias = ?;", new String[]{alias});
				
				if(c == null || !c.moveToFirst())
				{
					if(c != null) c.close();
					
					c = db.rawQuery("SELECT id FROM tags WHERE name = ?", new String[]{alias});
					
					if(c == null || !c.moveToFirst())
					{
						if(c != null) c.close();
						
						return "-1";
					}
					
					ret = c.getString(c.getColumnIndex("id"));
					
					return ret;
				}
				
				ret = c.getString(c.getColumnIndex("id"));
				
				c.close();
			}
			finally
			{
				db.close();
			}
			
			return ret;
		}
		
		private String toSql(SearchQuery sq)
		{
			String sql = " 1 ";
			
			for(String s : sq.ands)
			{
				if(s.contains(":"))
				{
					String meta = s.split(":")[0];
					String value= s.split(":")[1];
					
					if(meta.equals("rating"))
					{
						value = value.substring(0, 1);
						
						if(value.equals(E621Image.SAFE) || value.equals(E621Image.QUESTIONABLE) || value.equals(E621Image.EXPLICIT))
						{
							sql = sql + " AND";
							sql = sql + String.format(" EXISTS(SELECT 1 FROM e621image WHERE image=id AND rating=\"%1$s\") ", value);
						}
					}
				}
				else
				{
					s = antiAlias(s);
					
					if(s == null) continue;
					
					sql = sql + " AND";
					sql = sql + String.format(" EXISTS(SELECT 1 FROM image_tag WHERE image=id AND tag=\"%1$s\") ", s);
				}
			}
			
			if(sq.ors.size() > 0)
			{
				sql = sql + " AND ( 0 ";
				
				for(String s : sq.ors)
				{
					if(s.contains(":"))
					{
						String meta = s.split(":")[0];
						String value= s.split(":")[1];
						
						if(meta.equals("rating"))					{
							value = value.substring(0, 1);
							
							if(value.equals(E621Image.SAFE) || value.equals(E621Image.QUESTIONABLE) || value.equals(E621Image.EXPLICIT))
							{
								sql = sql + " OR";
								sql = sql + String.format(" EXISTS(SELECT 1 FROM e621image WHERE image=id AND rating=\"%1$s\") ", value);
							}
						}
					}
					else
					{
						s = antiAlias(s);
						
						if(s == null) continue;
						
						sql = sql + " OR";
						sql = sql + String.format(" EXISTS(SELECT 1 FROM image_tag WHERE image=id AND tag=\"%1$s\") ", s);
					}
				}
				
				sql = sql + " ) ";
			}
			
			if(sq.nots.size() > 0)
			{
				sql = sql + " AND NOT ( 0 ";
				
				for(String s : sq.nots)
				{
					if(s.contains(":"))
					{
						String meta = s.split(":")[0];
						String value= s.split(":")[1];
						
						if(meta.equals("rating"))
						{
							value = value.substring(0, 1);
							
							if(value.equals(E621Image.SAFE) || value.equals(E621Image.QUESTIONABLE) || value.equals(E621Image.EXPLICIT))
							{
								sql = sql + " OR";
								sql = sql + String.format(" EXISTS(SELECT 1 FROM e621image WHERE image=id AND rating=\"%1$s\") ", value);
							}
						}
					}
					else
					{
						s = antiAlias(s);
						
						if(s == null) continue;
						
						sql = sql + " OR";
						sql = sql + String.format(" EXISTS(SELECT 1 FROM image_tag WHERE image=id AND tag=\"%1$s\") ", s);
					}
				}
				
				sql = sql + " ) ";
			}
			
			return sql;
		}
	}
	
	private class InterruptedSearchManager
	{
		protected int version = 0;
		protected File path;
		
		public InterruptedSearchManager(File path)
		{
			this.path = path;
		}
		
		private void new_db(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE search (" +
					"search_query TEXT PRIMARY KEY" +
					", " +
					"seen_past UNSIGNED BIG INT" +
					", " +
					"seen_until UNSIGNED BIG INT" +
				");"
			);
			
			db.setVersion(0);
		}
		
		private SQLiteDatabase get_db()
		{
			SQLiteDatabase db;
			
			File db_path = new File(path,"db.sqlite3");
			
			try
			{
				db = SQLiteDatabase.openDatabase(db_path.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
			}
			catch(SQLiteException e)
			{
				db = SQLiteDatabase.openOrCreateDatabase(db_path, null);
				new_db(db);
			}
			
			while(db.getVersion() < version)
			{
				update_db(db.getVersion()+1);
				
				db.setVersion(db.getVersion()+1);
			}
			
			return db;
		}
		
		protected synchronized void update_db(int version)
		{
		}
		
		public void addOrUpdateSearch(String search, String seen_past, String seen_until)
		{
			SQLiteDatabase db = get_db();
			
			Cursor c = db.rawQuery("SELECT * FROM search WHERE search_query = ? LIMIT 1;", new String[]{search});
			
			if(!(c != null && c.moveToFirst()))
			{
				add(search,seen_past,seen_until,db);
			}
			else
			{
				if(c.getCount() == 0)
				{
					add(search,seen_past,seen_until,db);
				}
				else
				{
					update(search,seen_past,seen_until,db);
				}
				
				c.close();
			}
			
			db.close();
		}
		
		public void remove(String search)
		{
			SQLiteDatabase db = get_db();
			
			remove(search,db);
			
			db.close();
		}
		
		private void remove(String search, SQLiteDatabase db)
		{
			db.delete("search", "search_query = ?", new String[]{search});
		}
		
		private void add(String search, String seen_past, String seen_until, SQLiteDatabase db)
		{
			search = search.trim();
			
			ContentValues values = new ContentValues();
			values.put("search_query", search);
			values.put("seen_past", seen_past);
			values.put("seen_until", seen_until);
			
			db.insert("search", null, values);
		}
		
		private void update(String search, String seen_past, String seen_until, SQLiteDatabase db)
		{
			search = search.trim();
			
			Pair<String,String> current = getSearch(search,db);
			
			if(current != null)
			{
				seen_past = String.valueOf(Math.min(Integer.parseInt(seen_past),Integer.parseInt(current.left)));
				seen_until= String.valueOf(Math.max(Integer.parseInt(seen_until),Integer.parseInt(current.right)));
			}
			
			ContentValues values = new ContentValues();
			values.put("seen_past", seen_past);
			values.put("seen_until", seen_until);
			
			db.update("search", values, "search_query = ?", new String[]{search});
		}
		
		public Pair<String,String> getSearch(String search)
		{
			SQLiteDatabase db = get_db();
			
			Pair<String,String> ret = getSearch(search,db);
			
			db.close();
			
			return ret;
		}
		
		private Pair<String,String> getSearch(String search, SQLiteDatabase db)
		{
			Pair<String,String> ret = null;
			
			search = search.trim();
			
			Cursor c = db.rawQuery("SELECT seen_past, seen_until FROM search WHERE search_query = ? LIMIT 1", new String[]{search});
			
			if(c != null && c.moveToFirst())
			{
				if(c.getCount() > 0)
				{
					ret = new Pair<String,String>(c.getString(c.getColumnIndex("seen_past")),c.getString(c.getColumnIndex("seen_until")));
				}
				
				c.close();
			}
			
			if(ret.left == null || ret.right == null)
			{
				ret = null;
			}
			
			return ret;
		}
	
		public ArrayList<String> getAllSearches()
		{
			ArrayList<String> searches = new ArrayList<String>();
			
			SQLiteDatabase db = get_db();
			
			Cursor c = db.rawQuery("SELECT search_query FROM search ORDER BY search_query;", null);
			
			if(!(c != null && c.moveToFirst()))
			{
				return searches;
			}
			else
			{
				while(!c.isAfterLast())
				{
					searches.add(c.getString(c.getColumnIndex("search_query")));
					
					c.moveToNext();
				}
				
				c.close();
			}
			
			db.close();
			
			return searches;
		}
	}
}
