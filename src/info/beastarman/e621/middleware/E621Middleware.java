package info.beastarman.e621.middleware;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.api.E621Tag;
import info.beastarman.e621.api.E621TagAlias;
import info.beastarman.e621.api.E621Vote;
import info.beastarman.e621.backend.BackupManager;

import info.beastarman.e621.backend.ImageCacheManager;
import info.beastarman.e621.backend.ImageCacheManagerOld;
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
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class E621Middleware extends E621
{
	HashMap<Integer,E621Image> e621ImageCache = new HashMap<Integer,E621Image>();
	HashMap<String,Integer> searchCount = new HashMap<String,Integer>();
	
	File cache_path = null;
	File full_cache_path = null;
	File sd_path = null;
	File download_path = null;
	File export_path = null;
	File report_path = null;
	File interrupted_path = null;
	File backup_path = null;
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
	
	private Boolean firstRun = null;
	
	public static final String LOG_TAG = "E621MobileLogging";
	
	Context ctx;
	
	private static String CLIENT = "E621AndroidAppBstrm";
	
	protected E621Middleware(Context new_ctx)
	{
		super(CLIENT);
		
		if(new_ctx != null)
		{
			this.ctx = new_ctx;
		}
		
		cache_path = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"cache/");
		full_cache_path = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"full_cache/");
		sd_path = new File(Environment.getExternalStorageDirectory(),"e621/");
		download_path = new File(sd_path,"e621 Images/");
		export_path = new File(sd_path,"export/");
		report_path = new File(ctx.getExternalFilesDir(DIRECTORY_SYNC),"reports/");
		interrupted_path = new File(ctx.getExternalFilesDir(DIRECTORY_MISC),"interrupt/");
		backup_path = new File(ctx.getExternalFilesDir(DIRECTORY_MISC),"backups/");
		
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
		return getInstance((Context)null);
	}
	
	public static synchronized E621Middleware getInstance(Context ctx)
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

		if(!backup_path.exists())
		{
			backup_path.mkdirs();
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
				SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR,
		        AlarmManager.INTERVAL_HOUR, alarmIntent);
		
		interrupt = new InterruptedSearchManager(interrupted_path);
		
		String savedLogin = settings.getString("userLogin",null);
		String savedPasswordHash = settings.getString("userPasswordHash",null);
		
		if(savedLogin!=null && savedPasswordHash!=null)
		{
			login = savedLogin;
			password_hash = savedPasswordHash;
		}
		
		if(firstRun == null) firstRun = settings.getBoolean("firstRun",true);
		settings.edit().putBoolean("firstRun",false).commit();
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

	
	public boolean downloadInSearch()
	{
		return settings.getBoolean("downloadInSearch", true);
	}
	
	public boolean lazyLoad()
	{
		return settings.getBoolean("lazyLoad", true);
	}
	
	public int getFileDownloadSize()
	{
		return settings.getInt("prefferedFileDownloadSize", E621Image.SAMPLE);
	}
	
	public boolean isFirstRun()
	{
		return firstRun;
	}
	
	public int resultsPerPage()
	{
		return settings.getInt("resultsPerPage", 2)*10;
	}
	
	@Override
	public E621Image post__show(Integer id) throws IOException
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
	
	public void clearCache()
	{
		for (File child : cache_path.listFiles()) child.delete();
		cache_path.delete();
		
		for (File child : full_cache_path.listFiles()) child.delete();
		full_cache_path.delete();
		
		setup();
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
		InterruptedSearch pair = interrupt.getSearch(tags);
		
		if(pair == null)
		{
			return null;
		}
		
		String search_new = tags + " id:>" + pair.min_id + " order:id";
		String search_old = tags + " id:<" + pair.max_id;
		
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
		failed_download_manager.addFile(String.valueOf(img.id));
		
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
					
					failed_download_manager.removeFile(String.valueOf(img.id));
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
					download_manager.createOrUpdate(img, in, img.file_ext);
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
				in = full_cache.getFile(String.valueOf(img.id));
				
				if(in != null)
				{
					return in;
				}
				else
				{
					in = thumb_cache.getFile(String.valueOf(img.id));
					
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
				        
				        thumb_cache.createOrUpdate(String.valueOf(img.id), new ByteArrayInputStream(raw_file));
				        
				        return new ByteArrayInputStream(raw_file);
					}
				}
			}
		}
		
		if(size != E621Image.PREVIEW)
		{
			InputStream in = null;
			
			if(size == getFileDownloadSize())
			{
				in = download_manager.getFile(img);
			}
			
			if(in != null)
			{
				return in;
			}
			else
			{
				if(size == getFileDownloadSize())
				{
					in = full_cache.getFile(String.valueOf(img.id));
				}
				
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
			        
					full_cache.createOrUpdate(String.valueOf(img.id), new ByteArrayInputStream(raw_file));
			        
			        return new ByteArrayInputStream(raw_file);
				}
			}
		}
		
		return null;
	}
	
	public InputStream getDownloadedImage(Integer id)
	{
		return download_manager.getFile(id);
	}
	
	public void update_tags()
	{
		download_manager.updateMetadata(this);
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
					download_manager.updateMetadata(E621Middleware.this);
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
		
		for(final E621DownloadedImage image : ids)
		{
			Thread t = new Thread(new Runnable()
			{
				@Override
				public void run() {
					try {
						sem.acquire();
						
						File f = new File(path,image.filename);
						
						InputStream in = download_manager.getFile(image.id);
						
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
				img = post__show(Integer.parseInt(file));
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
		
		for(InterruptedSearch interrupted : getAllSearches())
		{
			update_new_image_count(interrupted.search);
		}
		
		backup();
	}
	
	public void backup()
	{
		ArrayList<E621DownloadedImage> downloads = download_manager.search(0, -1, new SearchQuery(""));
		ArrayList<InterruptedSearch> interrupts = interrupt.getAllSearches();
		
		JSONObject backup = new JSONObject();
		JSONArray downloadIDs = new JSONArray();
		
		for(E621DownloadedImage img : downloads)
		{
			downloadIDs.put(img.id);
		}
		
		JSONArray interruptsArray = new JSONArray();
		
		for(InterruptedSearch search : interrupts)
		{
			JSONObject jsonSearch = new JSONObject();
			
			try
			{
				jsonSearch.put("search",search.search);
				jsonSearch.put("min_id",search.min_id);
				jsonSearch.put("max_id",search.max_id);
			}
			catch (JSONException e)
			{
				return;
			}
			
			interruptsArray.put(jsonSearch);
		}
		
		try {
			backup.put("downloads",downloadIDs);
			backup.put("interrupts",interruptsArray);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
		
		BackupManager nk = new BackupManager(backup_path,
				new long[]{
					AlarmManager.INTERVAL_HOUR*24,
					AlarmManager.INTERVAL_HOUR*24*7,
				});
		
		InputStream in = null;
		try {
			in = new ByteArrayInputStream(backup.toString().getBytes("UTF-8"));
			nk.backup(in);
			in.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		android.util.Log.d(E621Middleware.LOG_TAG + "_Backup",nk.toString());
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
		
		if(ret != null && ret.success && e621ImageCache.containsKey(id))
		{
			E621Image img = e621ImageCache.get(id);
			img.score = ret.score;
			e621ImageCache.put(id, img);
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
	
	private void update_new_image_count(String search)
	{
		InterruptedSearch interrupted = interrupt.getSearch(search);
		
		if(interrupted.is_valid())
		{
			String search_new = search + " id:>" + interrupted.max_id + " order:id";
			String search_old = search + " id:<" + interrupted.min_id;
			
			try
			{
				int total_new = getSearchResultsCountForce(search_new);
				int total_old = getSearchResultsCountForce(search_old);
				
				interrupt.update_new_image_count(search, total_new + total_old);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			try {
				interrupt.update_new_image_count(search, getSearchResultsCountForce(search));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void continue_later(String search, String seen_past, String seen_until)
	{
		search= prepareQuery(search);
		
		interrupt.addOrUpdateSearch(search, seen_past, seen_until);
		
		update_new_image_count(search);
	}
	
	public InterruptedSearch get_continue_ids(String search)
	{
		search = prepareQuery(search);
		
		return interrupt.getSearch(search);
	}
	
	public ArrayList<InterruptedSearch> getAllSearches()
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
		InterruptedSearch pair = interrupt.getSearch(search);
		
		String search_new = search + " id:>" + pair.max_id + " order:id";
		String search_old = search + " id:<" + pair.min_id;
		
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
				
				first_images.addAll(second.images.subList(0, Math.min(second.images.size(),limit - first_images.size())));
				
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
	
	public ArrayList<E621Tag> getTags(String[] names)
	{
		return download_manager.getTags(names);
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
	
	public class InterruptedSearch
	{
		public String search;
		public Integer min_id;
		public Integer max_id;
		public int new_images;
		
		public InterruptedSearch(String search, Integer min_id, Integer max_id, int new_images)
		{
			this.search = search;
			this.min_id = min_id;
			this.max_id = max_id;
			this.new_images = new_images;
		}
		
		public boolean is_valid()
		{
			return min_id!=null && max_id!=null;
		}
	}
	
	private class InterruptedSearchManager
	{
		protected int version = 1;
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
		
		private synchronized SQLiteDatabase get_db()
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
				update_db(db, db.getVersion()+1);
				
				db.setVersion(db.getVersion()+1);
			}
			
			return db;
		}
		
		protected synchronized void update_db(SQLiteDatabase db, int version)
		{
			switch(version)
			{
				case 1:
					update_0_1(db);
					break;
				default:
					break;
			}
		}
		
		protected synchronized void update_0_1(SQLiteDatabase db)
		{
			db.execSQL("ALTER TABLE search ADD COLUMN new_images INTEGER DEFAULT 0;");
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
		
		public void update_new_image_count(String search, int new_image_count)
		{
			SQLiteDatabase db = get_db();
			
			update_new_image_count(search,new_image_count,db);
			
			db.close();
		}
		
		private void update_new_image_count(String search, int new_image_count, SQLiteDatabase db)
		{
			ContentValues values = new ContentValues();
			values.put("new_images", new_image_count);
			
			db.update("search", values, "search_query = ?", new String[]{search});
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
			
			InterruptedSearch current = getSearch(search,db);
			
			if(current != null && current.is_valid())
			{
				if(seen_past != null)
				{
					seen_past = String.valueOf(Math.min(Integer.parseInt(seen_past),current.min_id));
				}
				else
				{
					seen_past = String.valueOf(current.min_id);
				}
				
				if(seen_until != null)
				{
					seen_until = String.valueOf(Math.max(Integer.parseInt(seen_until),current.max_id));
				}
				else
				{
					seen_until = String.valueOf(current.max_id);
				}
			}
			
			ContentValues values = new ContentValues();
			values.put("seen_past", seen_past);
			values.put("seen_until", seen_until);
			
			db.update("search", values, "search_query = ?", new String[]{search});
		}
		
		public InterruptedSearch getSearch(String search)
		{
			SQLiteDatabase db = get_db();
			
			InterruptedSearch ret = getSearch(search,db);
			
			db.close();
			
			return ret;
		}
		
		private InterruptedSearch getSearch(String search, SQLiteDatabase db)
		{
			InterruptedSearch ret = null;
			
			search = search.trim();
			
			Cursor c = db.rawQuery("SELECT seen_past, seen_until, new_images FROM search WHERE search_query = ? LIMIT 1", new String[]{search});
			
			if(c != null && c.moveToFirst())
			{
				if(c.getCount() > 0)
				{
					String seen_past = c.getString(c.getColumnIndex("seen_past"));
					String seen_until = c.getString(c.getColumnIndex("seen_until"));
					
					ret = new InterruptedSearch(
							search,
							(seen_past==null||seen_past.equals("null"))?null:Integer.parseInt(seen_past),
							(seen_until==null||seen_until.equals("null"))?null:Integer.parseInt(seen_until),
							c.getInt(c.getColumnIndex("new_images")));
				}
				
				c.close();
			}
			
			return ret;
		}
	
		public ArrayList<InterruptedSearch> getAllSearches()
		{
			ArrayList<InterruptedSearch> searches = new ArrayList<InterruptedSearch>();
			
			SQLiteDatabase db = get_db();
			
			Cursor c = db.rawQuery("SELECT search_query, seen_past, seen_until, new_images FROM search ORDER BY -new_images, search_query;", null);
			
			if(!(c != null && c.moveToFirst()))
			{
				return searches;
			}
			else
			{
				while(!c.isAfterLast())
				{
					String seen_past = c.getString(c.getColumnIndex("seen_past"));
					String seen_until = c.getString(c.getColumnIndex("seen_until"));
					
					searches.add(new InterruptedSearch(
							c.getString(c.getColumnIndex("search_query")),
							(seen_past==null||seen_past.equals("null"))?null:Integer.parseInt(seen_past),
							(seen_until==null||seen_until.equals("null"))?null:Integer.parseInt(seen_until),
							c.getInt(c.getColumnIndex("new_images"))));
					
					c.moveToNext();
				}
				
				c.close();
			}
			
			db.close();
			
			return searches;
		}
	}
	
	public HashMap<String,Mascot> getAllMascots()
	{
		HashMap<String,Mascot> ret = new HashMap<String,Mascot>();
		ret.put("Keishinkae", new Mascot(R.drawable.mascot1,R.drawable.mascot1_blur,"Keishinkae","http://www.furaffinity.net/user/keishinkae"));
		ret.put("Keishinkae2", new Mascot(R.drawable.mascot2,R.drawable.mascot2_blur,"Keishinkae","http://www.furaffinity.net/user/keishinkae"));
		ret.put("darkdoomer", new Mascot(R.drawable.mascot3,R.drawable.mascot3_blur,"darkdoomer","http://nowhereincoming.net/"));
		ret.put("Narse", new Mascot(R.drawable.mascot4,R.drawable.mascot4_blur,"Narse","http://www.furaffinity.net/user/narse"));
		ret.put("chizi", new Mascot(R.drawable.mascot0,R.drawable.mascot0_blur,"chizi","http://www.furaffinity.net/user/chizi"));
		ret.put("wiredhooves", new Mascot(R.drawable.mascot5,R.drawable.mascot5_blur,"wiredhooves","http://www.furaffinity.net/user/wiredhooves"));
		ret.put("ECMajor", new Mascot(R.drawable.mascot6,R.drawable.mascot6_blur,"ECMajor","http://www.horsecore.org/"));
		ret.put("evalion", new Mascot(R.drawable.mascot7,R.drawable.mascot7_blur,"evalion","http://www.furaffinity.net/user/evalion"));
		
		return ret;
	}
	
	public Mascot[] getMascots()
	{
		HashMap<String,Mascot> allMascots = getAllMascots();
		ArrayList<String> disallowedMascots = getDisallowedMascots();
		
		for(String key : disallowedMascots)
		{
			if(allMascots.containsKey(key))
			{
				allMascots.remove(key);
			}
		}
		
		Mascot[] ret = allMascots.values().toArray(new Mascot[allMascots.size()]);
		
		return ret;
	}
	
	public void setDisallowedMascots(ArrayList<String> ids)
	{
		settings.edit().putStringSet("mascots", new HashSet<String>(ids)).commit();
	}
	
	public ArrayList<String> getDisallowedMascots()
	{
		return new ArrayList<String>(settings.getStringSet("mascots", new HashSet<String>()));
	}
	
	public static class Mascot
    {
    	public int image;
    	public int blur;
    	public String artistName;
    	public String artistUrl;
    	
    	public Mascot(int image, int blur, String artistName, String artistUrl)
    	{
    		this.image = image;
    		this.blur = blur;
    		this.artistName = artistName;
    		this.artistUrl = artistUrl;
    	}
    	
    	@Override
    	public boolean equals(Object that)
    	{
    		if(that instanceof Mascot)
    		{
    			return this.image == ((Mascot)that).image;
    		}
    		
    		return false;
    	}
    }
}
