package info.beastarman.e621.middleware;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621;
import info.beastarman.e621.api.E621Comment;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.api.E621Tag;
import info.beastarman.e621.api.E621Vote;
import info.beastarman.e621.backend.BackupManager;
import info.beastarman.e621.backend.DirectImageCacheManager;
import info.beastarman.e621.backend.DonationManager;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.backend.FileName;
import info.beastarman.e621.backend.GTFO;
import info.beastarman.e621.backend.ImageCacheManagerInterface;
import info.beastarman.e621.backend.ObjectStorage;
import info.beastarman.e621.backend.Pair;
import info.beastarman.e621.backend.PendingTask;
import info.beastarman.e621.backend.PersistentHttpClient;
import info.beastarman.e621.backend.ReadWriteLockerWrapper;
import info.beastarman.e621.middleware.AndroidAppUpdater.AndroidAppVersion;
import info.beastarman.e621.views.MediaInputStreamPlayer;
import info.beastarman.e621.views.StepsProgressDialog;

public class E621Middleware extends E621 {
    HashMap<Integer, E621Image> e621ImageCache = new HashMap<Integer, E621Image>();
    HashMap<String, Integer> searchCount = new HashMap<String, Integer>();

    File cache_path = null;
    File full_cache_path = null;
    File sd_path = null;
    File download_path = null;
    File webm_thumbnail_path = null;
    File export_path = null;
    File report_path = null;
    File interrupted_path = null;
    File backup_path = null;
    File emergency_backup = null;
    FailedDownloadManager failed_download_manager = null;

    InterruptedSearchManager interrupt;

    public static final String PREFS_NAME = "E621MobilePreferences";

	SharedPreferences settings;
    SharedPreferences.OnSharedPreferenceChangeListener settingsListener;

    HashSet<String> allowedRatings = new HashSet<String>();

    ImageCacheManagerInterface thumb_cache;
    ImageCacheManagerInterface full_cache;
    E621DownloadedImages download_manager;
    ImageCacheManagerInterface webm_thumbnails;

    BackupManager backupManager;

    private Semaphore updateTagsSemaphore = new Semaphore(1);
    private ObjectStorage<Object> searchStorage = new ObjectStorage<Object>();

    private static E621Middleware instance;

    private static String DIRECTORY_SYNC = "sync/";
    private static String DIRECTORY_MISC = "misc/";

    private String login = null;
    private String password_hash = null;

    private Long timeSinceFirstRun = null;

    public static final String LOG_TAG = "E621MLogging";

    Context ctx;

    private static String CLIENT = "E621AndroidAppBstrm";

    protected E621Middleware(Context new_ctx) {
        super(CLIENT);

        if (new_ctx != null) {
            this.ctx = new_ctx;
        }

        new MediaInputStreamPlayer();

        cache_path = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "cache/");
        full_cache_path = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "full_cache/");
        sd_path = new File(Environment.getExternalStorageDirectory(), "e621/");
        download_path = new File(sd_path, "e621 Images/");
        webm_thumbnail_path = new File(sd_path, "Webm Thumbs/");
        export_path = new File(sd_path, "export/");
        report_path = new File(ctx.getExternalFilesDir(DIRECTORY_SYNC), "reports/");
        interrupted_path = new File(sd_path, "interrupt/");
        backup_path = new File(sd_path, "backups/");
        emergency_backup = new File(ctx.getExternalFilesDir(DIRECTORY_MISC), "emergency.json");

        backupManager = new BackupManager(backup_path,
                new long[]{
                        AlarmManager.INTERVAL_HOUR * 24,
                        AlarmManager.INTERVAL_HOUR * 24 * 7,
                        AlarmManager.INTERVAL_HOUR * 24 * 30,
                });

        settings = ctx.getSharedPreferences(PREFS_NAME, 0);

        settingsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                setup();
            }
        };

        settings.registerOnSharedPreferenceChangeListener(settingsListener);

        setup();
    }

    public static E621Middleware getInstance() {
        return getInstance((Context) null);
    }

    public static synchronized E621Middleware getInstance(Context ctx) {
        if (instance == null) {
            instance = new E621Middleware(ctx);
        }

        return instance;
    }

    public void setup() {
        if (!cache_path.exists()) {
            cache_path.mkdirs();
        } else {
            if (new File(cache_path, ".cache.sqlite3").exists()) {
                for (File f : cache_path.listFiles()) {
                    f.delete();
                }
            }
        }

        if (!full_cache_path.exists()) {
            full_cache_path.mkdirs();
        } else {
            if (new File(full_cache_path, ".cache.sqlite3").exists()) {
                for (File f : full_cache_path.listFiles()) {
                    f.delete();
                }
            }
        }

        if (!sd_path.exists()) {
            sd_path.mkdirs();
        }

        if (!download_path.exists()) {
            download_path.mkdirs();
        }

        if (!webm_thumbnail_path.exists())
        {
            webm_thumbnail_path.mkdirs();
        }

        File webm_nomedia = new File(webm_thumbnail_path,".nomedia");

        if(!webm_nomedia.exists())
        {
            try {
                webm_nomedia.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!export_path.exists()) {
            export_path.mkdirs();
        }

        if (!report_path.exists()) {
            report_path.mkdirs();
        }

        if (!interrupted_path.exists()) {
            interrupted_path.mkdirs();
        }

        if (!backup_path.exists()) {
            backup_path.mkdirs();
        }

        if (settings.getBoolean("hideDownloadFolder", true)) {
            File no_media = new File(download_path, ".nomedia");

            try {
                no_media.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            File no_media = new File(download_path, ".nomedia");

            no_media.delete();
        }

        if (thumb_cache == null) {
            thumb_cache = new DirectImageCacheManager(cache_path, 0);
        }

        thumb_cache.setMaxSize(1024L * 1024 * settings.getInt("thumbnailCacheSize", 5));
        thumb_cache.clean();

        if (full_cache == null) {
            full_cache = new DirectImageCacheManager(full_cache_path, 0);
        }

        full_cache.setMaxSize(1024L * 1024 * settings.getInt("fullCacheSize", 10));
        full_cache.clean();

        if (download_manager == null) {
            download_manager = new E621DownloadedImages(ctx, download_path);
        }

        if (webm_thumbnails == null) {
            webm_thumbnails = new DirectImageCacheManager(webm_thumbnail_path, 0);
        }

        File failed_download_file = new File(ctx.getExternalFilesDir(DIRECTORY_SYNC), "failed_downloads.txt");

        if (!failed_download_file.exists()) {
            failed_download_file.getParentFile().mkdirs();
            try {
                failed_download_file.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (failed_download_manager == null) {
            failed_download_manager = new FailedDownloadManager(failed_download_file);
        }

        HashSet<String> allowedRatingsTemp = (HashSet<String>) settings.getStringSet("allowedRatings", new HashSet<String>());
        allowedRatings.clear();

        if (allowedRatingsTemp.contains(E621Image.SAFE)) {
            allowedRatings.add(E621Image.SAFE);
        }
        if (allowedRatingsTemp.contains(E621Image.QUESTIONABLE)) {
            allowedRatings.add(E621Image.QUESTIONABLE);
        }
        if (allowedRatingsTemp.contains(E621Image.EXPLICIT)) {
            allowedRatings.add(E621Image.EXPLICIT);
        }

        Intent intent = new Intent(ctx, E621SyncReciever.class);

        if (PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_NO_CREATE) == null || getSyncFrequency()==0)
        {
            setupSync();
        }

        interrupt = new InterruptedSearchManager(interrupted_path);

        String savedLogin = settings.getString("userLogin", null);
        String savedPasswordHash = settings.getString("userPasswordHash", null);

        if (savedLogin != null && savedPasswordHash != null) {
            login = savedLogin;
            password_hash = savedPasswordHash;
        }

        createExportFileObserver(export_path.getAbsolutePath());

        if (emergency_backup.exists()) {
            restoreEmergencyBackup();
        }

        if (timeSinceFirstRun == null) {
            long now = (new Date()).getTime();
            long firstRun = settings.getLong("firstRunTime", now);
            timeSinceFirstRun = now - firstRun;

            if (timeSinceFirstRun == 0)
            {
                boolean f = settings.getBoolean("firstRun", true);

                settings.edit().putLong("firstRunTime", now).commit();

                if (!f)
                {
                    timeSinceFirstRun = 666l;
                }
            }
        }
    }

    private void setupSync()
    {
        Intent intent = new Intent(ctx, E621SyncReciever.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);

        AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        alarmMgr.cancel(alarmIntent);

        long interval = AlarmManager.INTERVAL_HOUR * getSyncFrequency();

        if(interval > 0) {
            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + interval,
                    interval, alarmIntent);
        }
    }

	private final String updateVideosWebmMp4 = "updateVideosWebmMp4";

	public ArrayList<PendingTask> getPendingTasks()
	{
		ArrayList<PendingTask> tasks = new ArrayList<PendingTask>();

		if(!settings.getBoolean(updateVideosWebmMp4,false))
		{
			tasks.add(new PendingTaskUpdateVideosWebmMp4(this)
			{
				@Override
				protected void onComplete(EventManager eventManager)
				{
					settings.edit().putBoolean(updateVideosWebmMp4,true).commit();
					super.onComplete(eventManager);
				}
			});
		}

		return tasks;
	}

    public int isNewVersion()
    {
        PackageManager manager = ctx.getPackageManager();
        PackageInfo info = null;

        try
        {
            info = manager.getPackageInfo (ctx.getPackageName(), 0);
        }
        catch(PackageManager.NameNotFoundException e)
        {
        }

        if(info != null)
        {
            int currentVersion = info.versionCode;

            int lastRunningVersion = settings.getInt("lastRunningVersion",-1);

            if(currentVersion > lastRunningVersion)
            {
                settings.edit().putInt("lastRunningVersion",currentVersion).commit();

                return currentVersion;
            }
        }

        return -1;
    }

    public long getTimeSinceFirstRun()
    {
        return timeSinceFirstRun==null?0:timeSinceFirstRun;
    }

    private final long WEEK = 604800000;

    public boolean showDonatePopup()
    {
        if (getTimeSinceFirstRun() > WEEK)
        {
            if(settings.getBoolean("showDonate",true))
            {
                settings.edit().putBoolean("showDonate",false).commit();

                return true;
            }
            return false;
        }

        return false;
    }

    public File noMediaFile()
    {
        File f = download_path.getParentFile();

        while(f != null && f.canRead())
        {
            File nomedia = new File(f,".nomedia");

            if(nomedia.exists()) return nomedia;

            f = f.getParentFile();
        }

        return null;
    }

    public boolean testNoMediaFile()
    {
        boolean b = settings.getBoolean("testNoMediaFile", true);

        return b;
    }

    public void stopTestingMediaFile()
    {
        settings.edit().putBoolean("testNoMediaFile",false).commit();
    }

    public void removeNoMediaFile()
    {
        File f = noMediaFile();

        if(f != null && f.exists())
        {
            f.delete();

            settings.edit().putBoolean("testNoMediaFile",true).commit();
        }
    }

	public int getOnlinePosts() throws IOException
	{
		Integer onlinePosts = settings.getInt("onlinePosts", 0);

		if(onlinePosts == 0)
		{
			onlinePosts = getSearchResultsCountForce("");

			if(onlinePosts != null) settings.edit().putInt("onlinePosts",onlinePosts).commit();
		}

		return onlinePosts;
	}

	public void updateOnlinePosts() throws IOException
	{
		Integer onlinePosts = getSearchResultsCountForce("");

		if(onlinePosts != null) settings.edit().putInt("onlinePosts",onlinePosts).commit();
	}

	public long getOfflinePostsSize()
	{
		return download_manager.totalSize();
	}

	@Override
	protected HttpResponse tryHttpGet(String url, Integer tries) throws ClientProtocolException, IOException
	{
		if(url.contains("password"))
		{
			android.util.Log.i(LOG_TAG + "_Request","GET password containing request");
		}
		else
		{
			android.util.Log.i(LOG_TAG + "_Request","GET " + url);
		}
		
		return super.tryHttpGet(url, tries);
	}
	
	@Override
	protected HttpResponse tryHttpPost(String url, List<NameValuePair> pairs, Integer tries) throws ClientProtocolException, IOException
	{
		android.util.Log.i(LOG_TAG + "_Request","POST " + url);
		
		return super.tryHttpPost(url, pairs, tries);
	}

	public boolean showStatisticsInHome()
	{
		return settings.getBoolean("showStatisticsInHome", true);
	}

	public boolean showStatisticsInHome(boolean newValue)
	{
		settings.edit().putBoolean("showStatisticsInHome",newValue).commit();

		return newValue;
	}

	public boolean playGifs()
	{
		return settings.getBoolean("playGifs", true);
	}

	public int updateBreak()
	{
		return settings.getInt("updateBreak", 0);
	}

	public void setUpdateBreak(int i)
	{
		settings.edit().putInt("updateBreak", i).commit();
	}

    public void setSyncFrequency(int frequency)
    {
        settings.edit().putInt("syncFrequency",Math.max(0,frequency)).commit();

        setupSync();
    }

    public int getSyncFrequency()
    {
        return settings.getInt("syncFrequency",3);
    }

	public static enum BlacklistMethod
	{
		DISABLED(0),
		FLAG(1),
		HIDE(2),
		QUERY(3);

		private final int value;

		public int asInt()
		{
			return value;
		}

		BlacklistMethod(int value)
		{
			this.value = value;
		}
	}

	public BlacklistMethod blacklistMethod()
	{
		switch(settings.getInt("blacklistMethod",1))
		{
			case 0:
				return BlacklistMethod.DISABLED;
			case 1:
				return BlacklistMethod.FLAG;
			case 2:
				return BlacklistMethod.HIDE;
			default:
				return BlacklistMethod.QUERY;
		}
	}

	public ArrayList<String> isBlacklisted(E621Image image)
	{
		Set<String> list = blacklist().getEnabled();
		ArrayList<String> matches = new ArrayList<String>();

		for(String s : list)
		{
			if(mathces(image,new SearchQuery(s)))
			{
				matches.add(s);
			}
		}

		return matches;
	}

	private BlackList _blacklist = null;
	public BlackList blacklist()
	{
		if(_blacklist == null)
		{
			_blacklist = new E621BlackList(settings,this);
		}

		return _blacklist;
	}

	public ArrayList<String> isHighlighted(E621Image image)
	{
		Set<String> list = highlight().getEnabled();
		ArrayList<String> matches = new ArrayList<String>();

		for(String s : list)
		{
			if(mathces(image,new SearchQuery(s)))
			{
				matches.add(s);
			}
		}

		return matches;
	}

	private BlackList _highlight= null;
	public BlackList highlight()
	{
		if(_highlight == null)
		{
			_highlight = new E621BlackList(settings,"enabledHighlight","disabledHighlight",this);

			if(settings.getBoolean("firstHighlight",true))
			{
				_highlight.enable("animated");
			}

			settings.edit().putBoolean("firstHighlight",false).apply();
		}

		return _highlight;
	}

	public boolean mathces(E621Image image, SearchQuery query)
	{
		for(String tag : query.ands)
		{
			if(!image.tags.contains(new E621Tag(tag,0)))
			{
				return false;
			}
		}

		for(String tag : query.nots)
		{
			if(image.tags.contains(new E621Tag(tag,0)))
			{
				return false;
			}
		}

		if(query.ors.size() == 0)
		{
			return true;
		}

		for(String tag : query.ors)
		{
			if(image.tags.contains(new E621Tag(tag,0)))
			{
				return true;
			}
		}

		return false;
	}
	
	public boolean downloadInSearch()
	{
		return settings.getBoolean("downloadInSearch", true);
	}
	
	public boolean lazyLoad()
	{
		return settings.getBoolean("lazyLoad", true);
	}

	public boolean betaReleases()
	{
		return settings.getBoolean("betaReleases", false);
	}

	public int getFileThummbnailSize(E621Image img)
	{
		if(img.file_ext.equals("gif") || img.file_ext.equals("swf")) return E621Image.PREVIEW;

		int ret = settings.getInt("prefferedFilePreviewSize", E621Image.PREVIEW);

        if(img.file_ext.equals("webm") && ret == E621Image.FULL) ret = E621Image.SAMPLE;

        return ret;
	}
	public int getFileDownloadSize(String ext)
	{
        if(ext.equals("webm"))
        {
            return E621Image.FULL;
        }

		return settings.getInt("prefferedFileDownloadSize", E621Image.SAMPLE);
	}
	
	public boolean isFirstRun()
	{
		return timeSinceFirstRun == 0;
	}
	
	public int resultsPerPage()
	{
		return settings.getInt("resultsPerPage", 2)*10;
	}
	
	public int mostRecentKnownVersion()
	{
		return settings.getInt("mostRecentKnownVersion", -1);
	}
	
	public void updateMostRecentVersion(AndroidAppVersion version)
	{
		if(version == null) return;
		
		settings.edit().putInt("mostRecentKnownVersion",version.versionCode).apply();
	}
	
	public boolean syncOnlyOnWiFi()
	{
		return settings.getBoolean("syncOnlyOnWiFi", true);
	}
	
	public boolean antecipateOnlyOnWiFi()
	{
		return settings.getBoolean("antecipateOnlyOnWiFi", true);
	}

	public static final int DATE_ASC = 1;
	public static final int DATE_DESC = 2;
	public static final int SCORE = 3;

	public int commentsSorting()
	{
		return settings.getInt("commentsSorting", DATE_ASC);
	}

    private String getWebmPreviewUrl(int id)
    {
        return "http://beastarman.info/media/E621Webm/thumb/"+id+".jpg";
    }

	private String getWebmSampleUrl(int id)
	{
		return "http://beastarman.info/media/E621Webm/image/"+id+".jpg";
	}

	private String getWebmVideoUrl(int id)
	{
		return "http://beastarman.info/media/E621Webm/video/"+id+".mp4";
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

            if(img.file_ext.equals("webm"))
            {
                img.preview_url = getWebmPreviewUrl(img.id);
                img.preview_height = 120;
                img.preview_width = 120;

                img.sample_url = getWebmSampleUrl(img.id);
                img.sample_height = 480;
                img.sample_width = 480;

				img.file_url = getWebmVideoUrl(img.id);

				img.file_ext = "mp4";
            }
			
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
                if(img.file_ext.equals("webm"))
                {
					img.preview_url = getWebmPreviewUrl(img.id);
					img.preview_height = 120;
					img.preview_width = 120;

					img.sample_url = getWebmSampleUrl(img.id);
                    img.sample_height = 480;
                    img.sample_width = 480;

					img.file_url = getWebmVideoUrl(img.id);

					img.file_ext = "mp4";
                }

				e621ImageCache.put(img.id, img);
			}
			
			searchCount.put(tags, ret.count);
		}
		
		return ret;
	}
	
	public void clearCache()
	{
		full_cache.clear();
		thumb_cache.clear();
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
			final ArrayList<Integer> suspicious_counts = new ArrayList<Integer>();
			suspicious_counts.add(10);
			
			int temp = post__index(tags,0,1).count;
			
			if(suspicious_counts.contains(temp))
			{
				temp = post__index(tags,0,1).count;
			}
			
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
		
		if(!pair.is_valid())
		{
			return getSearchResultsPages(tags,results_per_page);
		}
		
		String search_new = tags + " id:>" + pair.max_id + " order:id";
		String search_old = tags + " id:<" + pair.min_id;
		
		Integer count_new;
		Integer count_old;
		
		count_new = getSearchResultsCount(search_new);
		count_old = getSearchResultsCount(search_old);
		
		if(count_new == null || count_old == null)
		{
			return null;
		}
		
		int count = count_new + count_old;
		
		return (int) Math.ceil(((double)count)/((double)results_per_page));
	}

	private AlphaFeatures alphaFeatures = null;
	public AlphaFeatures alpha()
	{
		if(alphaFeatures == null)
		{
			HashMap<String, String> features = new HashMap<String, String>();

			alphaFeatures = new AlphaFeatures(settings,features);
		}

		return alphaFeatures;
	}

	public static enum DownloadStatus
	{
		DOWNLOADING,
		DOWNLOADED,
		DELETING,
		DELETED
	}
	
	private Map<Integer,Set<EventManager>> downloads = Collections.synchronizedMap(new HashMap<Integer,Set<EventManager>>());
	private Map<Integer,DownloadStatus> ongoing = Collections.synchronizedMap(new HashMap<Integer,DownloadStatus>());
	private Set<Integer> cancel = Collections.synchronizedSet(new HashSet<Integer>());
	
	public void bindDownloadState(final Integer id, final EventManager event)
	{
		synchronized(downloads)
		{
			if(downloads.containsKey(id))
			{
				downloads.get(id).add(event);
			}
			else
			{
				Set<EventManager> set = new HashSet<EventManager>();
				set.add(event);
				
				downloads.put(id, set);
			}
		}
		
		if(ongoing.containsKey(id))
		{
			event.trigger(ongoing.get(id));
		}
		else
		{
			new Thread(new Runnable()
			{
				public void run()
				{
					if(download_manager.hasFile(id))
					{
						event.trigger(DownloadStatus.DOWNLOADED);
					}
					else
					{
						event.trigger(DownloadStatus.DELETED);
					}
				}
			}).start();
		}
	}
	
	public void unbindDownloadState(Integer id, EventManager event)
	{
		synchronized(downloads)
		{
			if(downloads.containsKey(id))
			{
				downloads.get(id).remove(event);
				
				if(downloads.get(id).size() == 0)
				{
					downloads.remove(id);
				}
			}
		}
	}
	
	public boolean saveImage(final Integer id)
	{
		synchronized(ongoing)
		{
			if(ongoing.containsKey(id))
			{
				if(ongoing.get(id)==DownloadStatus.DELETING)
				{
					if(cancel.contains(id))
					{
						cancel.remove(id);
					}
					else
					{
						cancel.add(id);
					}
					
					ongoing.put(id,DownloadStatus.DOWNLOADING);
					
					synchronized(downloads)
					{
						if(downloads.containsKey(id))
						{
							for(EventManager event : downloads.get(id))
							{
								event.trigger(DownloadStatus.DOWNLOADING);
							}
						}
						else
						{
							ongoing.put(id,DownloadStatus.DOWNLOADING);
						}
					}
				}
				
				return true;
			}
		}

		synchronized(downloads)
		{
			if(downloads.containsKey(id))
			{
				for(EventManager event : downloads.get(id))
				{
					event.trigger(DownloadStatus.DOWNLOADING);
				}
			}
			else
			{
				ongoing.put(id,DownloadStatus.DOWNLOADING);
			}
		}
		
		try
		{
			E621Image img = post__show(id);
			
			if(img == null)
			{
				synchronized(downloads)
				{
					if(downloads.containsKey(id))
					{
						for(EventManager event : downloads.get(id))
						{
							event.trigger(DownloadStatus.DELETED);
						}
					}
				}
				
				cancel.remove(id);
				ongoing.remove(id);
				
				return false;
			}
			
			return saveImageSkipTrigger(img);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean saveImage(final E621Image img)
	{
		if(img == null) return false;
		
		int id = img.id;
		
		synchronized(ongoing)
		{
			if(ongoing.containsKey(id))
			{
				if(ongoing.get(id)==DownloadStatus.DELETING)
				{
					if(cancel.contains(id))
					{
						cancel.remove(id);
					}
					else
					{
						cancel.add(id);
					}
					
					ongoing.put(id,DownloadStatus.DOWNLOADING);
					
					synchronized(downloads)
					{
						if(downloads.containsKey(id))
						{
							for(EventManager event : downloads.get(id))
							{
								event.trigger(DownloadStatus.DOWNLOADING);
							}
						}
					}
				}
				
				return true;
			}
			else
			{
				ongoing.put(id,DownloadStatus.DOWNLOADING);
			}
		}
		
		synchronized(downloads)
		{
			if(downloads.containsKey(id))
			{
				for(EventManager event : downloads.get(id))
				{
					event.trigger(DownloadStatus.DOWNLOADING);
				}
			}
		}
		
		return saveImageSkipTrigger(img);
	}	
	
	public boolean saveImageSkipTrigger(final E621Image img)
	{
		if(img.status == E621Image.DELETED)
		{
			failed_download_manager.removeFile(String.valueOf(img.id));
			
			synchronized(downloads)
			{
				if(downloads.containsKey(img.id))
				{
					for(EventManager event : downloads.get(img.id))
					{
						event.trigger(DownloadStatus.DELETED);
					}
				}
			}
			
			ongoing.remove(img.id);
			
			return false;
		}
		
		failed_download_manager.addFile(String.valueOf(img.id));
		
		final InputStream in;

        if(img.file_ext.equals("webm"))
        {
            in = getVideo(img.id);
        }
        else
        {
            in = getImage(img, getFileDownloadSize(img.file_ext));
        }
		
		if(in != null)
		{
			String ext = img.file_ext;
			
			if(getFileDownloadSize(img.file_ext) == E621Image.SAMPLE)
			{
				String[] temp = img.sample_url.split("\\.");
				ext = temp[temp.length-1];
			}
			
			if(!download_manager.createOrUpdate(img, in, ext))
            {
                for(EventManager event : downloads.get(img.id))
                {
                    event.trigger(DownloadStatus.DELETED);
                }

                return false;
            }
			
			failed_download_manager.removeFile(String.valueOf(img.id));
			
			ongoing.remove(img.id);
			
			if(cancel.contains(img.id))
			{
				cancel.remove(img.id);
				
				deleteImage(img.id);
				
				return false;
			}
			
			synchronized(downloads)
			{
				if(downloads.containsKey(img.id))
				{
					for(EventManager event : downloads.get(img.id))
					{
						event.trigger(DownloadStatus.DOWNLOADED);
					}
				}
			}

            if(img.file_ext.equals("webm")) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        generateWebmThumbnail(img.id);
                    }
                }).start();
            }
			
			return true;
		}
		else
		{
			synchronized(downloads)
			{
				if(downloads.containsKey(img.id))
				{
					for(EventManager event : downloads.get(img.id))
					{
						event.trigger(DownloadStatus.DELETED);
					}
				}
			}
			
			ongoing.remove(img.id);
			
			return false;
		}
	}

	public void deleteImage(E621Image img)
	{
		deleteImage(img.id);
	}

	public void deleteImage(Integer id)
	{
		synchronized(ongoing)
		{
			if(ongoing.containsKey(id))
			{
				if(ongoing.get(id)==DownloadStatus.DOWNLOADING)
				{
					if(cancel.contains(id))
					{
						cancel.remove(id);
					}
					else
					{
						cancel.add(id);
					}
					
					ongoing.put(id,DownloadStatus.DELETING);
					
					synchronized(downloads)
					{
						if(downloads.containsKey(id))
						{
							for(EventManager event : downloads.get(id))
							{
								event.trigger(DownloadStatus.DELETING);
							}
						}
					}
				}
				
				return;
			}
			else
			{
				ongoing.put(id,DownloadStatus.DELETING);
			}
		}
		
		synchronized(downloads)
		{
			if(downloads.containsKey(id))
			{
				for(EventManager event : downloads.get(id))
				{
					event.trigger(DownloadStatus.DELETING);
				}
			}
		}
		
		download_manager.removeFile(id);
		
		failed_download_manager.removeFile(String.valueOf(id));
		
		ongoing.remove(id);
		
		if(cancel.contains(id))
		{
			cancel.remove(id);
			
			saveImage(id);
			
			return;
		}
		
		synchronized(downloads)
		{
			if(downloads.containsKey(id))
			{
				for(EventManager event : downloads.get(id))
				{
					event.trigger(DownloadStatus.DELETED);
				}
			}
		}

        webm_thumbnails.removeFile(String.valueOf(id));
	}
	
	Semaphore getImageSemaphore = new Semaphore(10);
	
	private InputStream getImageFromInternet(String url)
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
				try
				{
					return response.getEntity().getContent();
				}
				catch (IOException e)
				{
					e.printStackTrace();

					return null;
				}
		    }
		    else
		    {
				Log.d(LOG_TAG,"Return code for " + url + ":" + statusLine.getStatusCode());

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

    public static Bitmap decodeFileKeepRatio(InputStream in, int width, int height)
    {
        byte[] bytes = null;

        try {
            bytes = IOUtils.toByteArray(in);
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }

        in = new ByteArrayInputStream(bytes);

        //Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, o);

        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }

        //Find the correct scale value. It should be the power of 2.
        int scale = 1;
        while (o.outWidth / scale / 2 >= width && o.outHeight / scale / 2 >= height) {
            scale *= 2;
        }

        in = new ByteArrayInputStream(bytes);

        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        Bitmap bitmap_temp = BitmapFactory.decodeStream(in, null, o2);

        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        bytes = null;
        System.gc();

        if (bitmap_temp == null)
        {
            return null;
        }
        else if(width == bitmap_temp.getWidth() && height == bitmap_temp.getHeight())
        {
            return bitmap_temp;
        }
        else
        {
            float ratio = ((float)bitmap_temp.getWidth())/width;
            height = (int)(bitmap_temp.getHeight()/ratio);

            Bitmap ret = Bitmap.createScaledBitmap(bitmap_temp,width,height,false);

            bitmap_temp.recycle();

            return ret;
        }
    }

	public static Bitmap decodeFile(InputStream in, int width, int height)
    {
        byte[] bytes = null;

        try {
            bytes = IOUtils.toByteArray(in);
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }

        in = new ByteArrayInputStream(bytes);

        //Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, o);

        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }

        //Find the correct scale value. It should be the power of 2.
        int scale = 1;
        while (o.outWidth / scale / 2 >= width && o.outHeight / scale / 2 >= height) {
            scale *= 2;
        }

        in = new ByteArrayInputStream(bytes);

        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        Bitmap bitmap_temp = BitmapFactory.decodeStream(in, null, o2);

        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        bytes = null;
        System.gc();

        if (bitmap_temp == null)
        {
            return null;
        }
		else if(width == bitmap_temp.getWidth() && height == bitmap_temp.getHeight())
		{
			return bitmap_temp;
		}
		else
		{
			Bitmap ret = Bitmap.createScaledBitmap(bitmap_temp,width,height,false);

			bitmap_temp.recycle();

			return ret;
		}
	}

	public Bitmap getThumbnail(final int id, final int width, final int height)
	{
		final GTFO<Bitmap> in = new GTFO<Bitmap>();
		final GTFO<Boolean> storeInCache = new GTFO<Boolean>();
		storeInCache.obj = true;

		final Object lock = new Object();

		List<Thread> threads = Collections.synchronizedList(new ArrayList<Thread>());

		threads.add(new Thread(new Runnable()
		{
			public void run()
			{
				InputStream inTemp = download_manager.getFile(id);

				Bitmap bmp = decodeFileKeepRatio(inTemp, width, height);

				if(inTemp != null)
				{
					synchronized(lock)
					{
						if(in.obj == null)
						{
							in.obj = bmp;
						}
					}
				}
			}
		}));

		threads.add(new Thread(new Runnable()
		{
			public void run()
			{
				InputStream inTemp = full_cache.getFile(String.valueOf(id));

				Bitmap bmp = decodeFileKeepRatio(inTemp, width, height);

				if(inTemp != null)
				{
					synchronized(lock)
					{
						if(in.obj == null)
						{
							in.obj = bmp;
						}
					}
				}
			}
		}));

		threads.add(new Thread(new Runnable() {
            public void run() {
                InputStream inTemp = thumb_cache.getFile(String.valueOf(id));

                Bitmap bmp = decodeFileKeepRatio(inTemp, width, height);

                if (inTemp != null) {
                    synchronized (lock) {
                        if (in.obj == null) {
                            storeInCache.obj = false;
                            in.obj = bmp;
                        }
                    }
                }
            }
        }));

		if(!(antecipateOnlyOnWiFi() && !isWifiConnected()))
		{
			threads.add(new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						Thread.sleep(3000);

						synchronized (lock)
						{
							if (in.obj != null)
							{
								return;
							}
						}
					} catch (InterruptedException e)
					{
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}

					String url;

					E621Image img = null;

					try
					{
						img = post__show(id);
					} catch (IOException e)
					{
						e.printStackTrace();
						return;
					}

					int size = getFileThummbnailSize(img);

					switch (size)
					{
						case E621Image.PREVIEW:
							url = img.preview_url;
							break;
						case E621Image.SAMPLE:
							url = img.sample_url;
							break;
						case E621Image.FULL:
						default:
                            url = img.file_url;
							break;
					}

					InputStream inputStream = getImageFromInternet(url);

					if (in == null || inputStream == null)
					{
						return;
					}

					Bitmap bmp = decodeFileKeepRatio(inputStream, width, height);

					synchronized (lock)
					{
						if (in.obj == null)
						{
							in.obj = bmp;
						}
					}
				}
			}));
		}

		if(in.obj != null)
		{
			if(storeInCache.obj)
			{
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				in.obj.compress(Bitmap.CompressFormat.JPEG, 90, bos);
				byte[] bitmapdata = bos.toByteArray();
				ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);

				thumb_cache.createOrUpdate(String.valueOf(id),bs);
			}

			return in.obj;
		}
		else
		{
			String url;

			E621Image img = null;

			try
			{
				img = post__show(id);
			} catch (IOException e)
			{
				e.printStackTrace();
				return null;
			}

			int size = getFileThummbnailSize(img);

			switch (size)
			{
				case E621Image.PREVIEW:
					url = img.preview_url;
					break;
				case E621Image.SAMPLE:
					url = img.sample_url;
					break;
				case E621Image.FULL:
				default:
					url = img.file_url;
					break;
			}

			int tries = 5;
			Bitmap bmp = null;

			while(bmp == null && tries>0)
			{
				InputStream inputStream = getImageFromInternet(url);

				if (in == null || inputStream == null)
				{
					return null;
				}

				bmp = decodeFileKeepRatio(inputStream,width,height);

				tries--;
			}

			return bmp;
		}
	}

    public InputStream getImage(final int img, final int size)
    {
        final GTFO<InputStream> in = new GTFO<InputStream>();

        final Object lock = new Object();

        List<Thread> threads = Collections.synchronizedList(new ArrayList<Thread>());

        threads.add(new Thread(new Runnable()
        {
            public void run()
            {
                InputStream inTemp = download_manager.getFile(img);

                if(inTemp != null)
                {
                    synchronized(lock)
                    {
                        if(in.obj == null)
                        {
                            in.obj = inTemp;
                        }
                    }
                }
            }
        }));

        threads.add(new Thread(new Runnable()
        {
            public void run()
            {
                InputStream inTemp = full_cache.getFile(String.valueOf(img));

                if(inTemp != null)
                {
                    synchronized(lock)
                    {
                        if(in.obj == null)
                        {
                            in.obj = inTemp;
                        }
                    }
                }
            }
        }));

        if(size == E621Image.PREVIEW)
        {
            threads.add(new Thread(new Runnable()
            {
                public void run()
                {
                    InputStream inTemp = thumb_cache.getFile(String.valueOf(img));

                    if(inTemp != null)
                    {
                        synchronized(lock)
                        {
                            if(in.obj == null)
                            {
                                in.obj = inTemp;
                            }
                        }
                    }
                }
            }));
        }

        if(size == E621Image.FULL && getFileDownloadSize("jpg") == E621Image.SAMPLE)
        {
            threads.clear();
        }

        if(!(antecipateOnlyOnWiFi() && !isWifiConnected()))
        {
            threads.add(new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        Thread.sleep(3000);

                        synchronized (lock)
                        {
                            if (in.obj != null)
                            {
                                return;
                            }
                        }
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }

                    String url;
                    E621Image eImg;

                    try
                    {
                        eImg = post__show(img);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        return;
                    }

                    switch (size)
                    {
                        case E621Image.PREVIEW:
                            url = eImg.preview_url;
                            break;
                        case E621Image.SAMPLE:
                            url = eImg.sample_url;
                            break;
                        case E621Image.FULL:
                        default:
                            url = eImg.file_url;
                            break;
                    }

                    InputStream inputStream = getImageFromInternet(url);

                    if (in == null || inputStream == null)
                    {
                        return;
                    }

                    byte[] byteArray;

                    try
                    {
                        byteArray = IOUtils.toByteArray(inputStream);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        return;
                    }
                    finally
                    {
                        try
                        {
                            inputStream.close();
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    byte[] b2 = byteArray.clone();

                    inputStream = new ByteArrayInputStream(byteArray);

                    if (size == E621Image.PREVIEW)
                    {
                        thumb_cache.createOrUpdate(String.valueOf(img), inputStream);
                    }
                    else
                    {
                        full_cache.createOrUpdate(String.valueOf(img), inputStream);
                    }

                    inputStream = new ByteArrayInputStream(b2);

                    synchronized (lock)
                    {
                        if (in.obj == null)
                        {
                            in.obj = inputStream;
                        }
                    }
                }
            }));
        }

        for(Thread t : threads)
        {
            t.start();
        }

        while(in.obj == null)
        {
            int i=0;

            for(i=threads.size(); i>0; i--)
            {
                if(!threads.get(i-1).isAlive())
                {
                    threads.remove(i-1);
                }
            }

            if(threads.size() == 0)
            {
                break;
            }
        }

        if(in.obj == null)
        {
            String url;
            E621Image eImg;

            try
            {
                eImg = post__show(img);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }

            switch (size)
            {
                case E621Image.PREVIEW:
                    url = eImg.preview_url;
                    break;
                case E621Image.SAMPLE:
                    url = eImg.sample_url;
                    break;
                case E621Image.FULL:
                default:
                    url = eImg.file_url;
                    break;
            }

            InputStream inputStream = getImageFromInternet(url);

            if (in == null || inputStream == null)
            {
                return null;
            }

            byte[] byteArray;

            try
            {
                byteArray = IOUtils.toByteArray(inputStream);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }
            finally
            {
                try
                {
                    inputStream.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            byte[] b2 = byteArray.clone();

            inputStream = new ByteArrayInputStream(byteArray);

            if (size == E621Image.PREVIEW)
            {
                thumb_cache.createOrUpdate(String.valueOf(img), inputStream);
            }
            else
            {
                full_cache.createOrUpdate(String.valueOf(img), inputStream);
            }

            inputStream = new ByteArrayInputStream(b2);

            in.obj = inputStream;
        }

        return in.obj;
    }

    public InputStream getVideo(final int img) {
        InputStream ret = download_manager.getFile(img);

        if(ret == null)
        {
            try {
                return getImageFromInternet(post__show(img).file_url);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

	public InputStream getImage(final E621Image img, final int size)
	{
		final GTFO<InputStream> in = new GTFO<InputStream>();
		
		final Object lock = new Object();
		
		List<Thread> threads = Collections.synchronizedList(new ArrayList<Thread>());
		
		threads.add(new Thread(new Runnable()
		{
			public void run()
			{
				InputStream inTemp = download_manager.getFile(img);

				if(inTemp != null)
				{
					synchronized(lock)
					{
						if(in.obj == null)
						{
							in.obj = inTemp;
						}
					}
				}
			}
		}));
		
		threads.add(new Thread(new Runnable()
		{
			public void run()
			{
				InputStream inTemp = full_cache.getFile(String.valueOf(img.id));
				
				if(inTemp != null)
				{
					synchronized(lock)
					{
						if(in.obj == null)
						{
							in.obj = inTemp;
						}
					}
				}
			}
		}));
		
		if(size == E621Image.PREVIEW)
		{
			threads.add(new Thread(new Runnable()
			{
				public void run()
				{
					InputStream inTemp = thumb_cache.getFile(String.valueOf(img.id));
					
					if(inTemp != null)
					{
						synchronized(lock)
						{
							if(in.obj == null)
							{
								in.obj = inTemp;
							}
						}
					}
				}
			}));
		}
		
		if(size == E621Image.FULL && getFileDownloadSize("jpg") == E621Image.SAMPLE)
		{
			threads.clear();
		}

		if(!(antecipateOnlyOnWiFi() && !isWifiConnected()))
		{
			threads.add(new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						Thread.sleep(3000);

						synchronized (lock)
						{
							if (in.obj != null)
							{
								return;
							}
						}
					} catch (InterruptedException e)
					{
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}

					String url;

					switch (size)
					{
						case E621Image.PREVIEW:
							url = img.preview_url;
							break;
						case E621Image.SAMPLE:
							url = img.sample_url;
							break;
						case E621Image.FULL:
						default:
							url = img.file_url;
							break;
					}

					InputStream inputStream = getImageFromInternet(url);

					if (in == null || inputStream == null)
					{
						return;
					}

					byte[] byteArray;

					try
					{
						byteArray = IOUtils.toByteArray(inputStream);
					}
					catch (IOException e)
					{
						e.printStackTrace();
						return;
					}
					finally
					{
						try
						{
							inputStream.close();
						} catch (IOException e)
						{
							e.printStackTrace();
						}
					}

					byte[] b2 = byteArray.clone();

					inputStream = new ByteArrayInputStream(byteArray);

					if (size == E621Image.PREVIEW)
					{
						thumb_cache.createOrUpdate(String.valueOf(img.id), inputStream);
					}
					else
					{
						full_cache.createOrUpdate(String.valueOf(img.id), inputStream);
					}

					inputStream = new ByteArrayInputStream(b2);

					synchronized (lock)
					{
						if (in.obj == null)
						{
							in.obj = inputStream;
						}
					}
				}
			}));
		}
		
		for(Thread t : threads)
		{
			t.start();
		}
		
		while(in.obj == null)
		{
			int i=0;
			
			for(i=threads.size(); i>0; i--)
			{
				if(!threads.get(i-1).isAlive())
				{
					threads.remove(i-1);
				}
			}
			
			if(threads.size() == 0)
			{
				break;
			}
		}

		if(in.obj == null)
		{
			String url;

			switch (size)
			{
				case E621Image.PREVIEW:
					url = img.preview_url;
					break;
				case E621Image.SAMPLE:
					url = img.sample_url;
					break;
				case E621Image.FULL:
				default:
					url = img.file_url;
					break;
			}

			InputStream inputStream = getImageFromInternet(url);

			if (in == null || inputStream == null)
			{
				return null;
			}

			byte[] byteArray;

			try
			{
				byteArray = IOUtils.toByteArray(inputStream);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return null;
			}
			finally
			{
				try
				{
					inputStream.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}

			byte[] b2 = byteArray.clone();

			inputStream = new ByteArrayInputStream(byteArray);

			if (size == E621Image.PREVIEW)
			{
				thumb_cache.createOrUpdate(String.valueOf(img.id), inputStream);
			}
			else
			{
				full_cache.createOrUpdate(String.valueOf(img.id), inputStream);
			}

			inputStream = new ByteArrayInputStream(b2);

			in.obj = inputStream;
		}
		
		return in.obj;
	}

    public void generateWebmThumbnail(int id)
    {
        webm_thumbnails.createOrUpdate(String.valueOf(id), getImageFromInternet(getWebmSampleUrl(id)));
    }

    public InputStream getDownloadedImageThumb(final E621DownloadedImage id)
    {
        if(id.getType().equals("jpg") || id.getType().equals("png") || id.getType().equals("gif"))
        {
            return getDownloadedImage(id);
        }
        else if(id.getType().equals("webm"))
        {
            InputStream ret = webm_thumbnails.getFile(String.valueOf(id.id));

            if(ret == null && isWifiConnected())
            {
                generateWebmThumbnail(id.id);

                ret = webm_thumbnails.getFile(String.valueOf(id.id));
            }

            return ret;
        }

        return null;
    }

	public InputStream getDownloadedImage(E621DownloadedImage id)
	{
		return download_manager.getFile(id);
	}

	public void update_tags(EventManager em)
	{
		download_manager.updateMetadata(this, em);
	}

	public void update_some_metadata(EventManager em)
	{
		setUpdateBreak(download_manager.updateMetadataPartial(this, updateBreak(), em));
	}

	public void force_update_tags(EventManager em)
	{
		download_manager.updateMetadataForce(this, em);
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

		if(blacklistMethod() == BlacklistMethod.QUERY)
		{
			int query_size = tags.split("\\s+").length;

			for(String tag : blacklist().getEnabled())
			{
				if(tag.trim().split("\\s+").length != 1)
				{
					continue;
				}

				if(query_size >= 6)
				{
					break;
				}

				tags += " -" + tag;

				query_size++;
			}
		}
		
		return tags;
	}

    public E621DownloadedImage localGet(int id)
    {
        ArrayList<E621DownloadedImage> images = download_manager.search(0, 1, new SearchQuery("id:"+id));

        if(images.isEmpty())
        {
            return null;
        }
        else
        {
            return images.get(0);
        }
    }
	
	public ArrayList<E621DownloadedImage> localSearch(int page, int limit, String tags)
	{
		tags = prepareQuery(tags);
		
		return download_manager.search(page, limit, new SearchQuery(tags));
	}
	
	private File getExportFileFromQuery(String search)
	{
		search = prepareQuery(search);
		
		SearchQuery sq = new SearchQuery(search);
		
		if(sq.normalize().length() > 0)
		{
			return new File(export_path,FileName.encodeFileName(sq.normalize().replace(":", "..")));
		}
		else
		{
			return new File(export_path,"all_images_");
		}
	}
	
	public File export(String search)
	{
		SearchQuery sq = new SearchQuery(search);
		
		ArrayList<E621DownloadedImage> ids = download_manager.search(0, Integer.MAX_VALUE, sq);
		
		final Semaphore sem = new Semaphore(10);
		
		final File path = getExportFileFromQuery(search);
		
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

						if(in == null)
						{
							return;
						}
						
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
		
		return path;
	}
	
	public void removeExported(String search)
	{
		final File f = getExportFileFromQuery(search);
		
		if(f.exists())
		{
			for(String s : f.list())
			{
				new File(f,s).delete();
			}
			
			f.delete();
		}
	}
	
	public static enum ExportState
	{
		CREATED,
		REMOVED,
	}
	
	private FileObserver exportObserver;
	
	private void createExportFileObserver(String path)
	{
		exportObserver = new FileObserver(export_path.getAbsolutePath())
		{
			@Override
			public void onEvent(int state, String file)
			{
				if((state & FileObserver.DELETE) > 0)
				{
					synchronized(exportedSearches)
					{
						if(exportedSearches.containsKey(file))
						{
							for(EventManager event : exportedSearches.get(file))
							{
								event.trigger(ExportState.REMOVED);
							}
						}
					}
				}
				else if((state & FileObserver.CREATE) > 0)
				{
					synchronized(exportedSearches)
					{
						if(exportedSearches.containsKey(file))
						{
							for(EventManager event : exportedSearches.get(file))
							{
								event.trigger(ExportState.CREATED);
							}
						}
					}
				}
			}
		};
		
		exportObserver.startWatching();
	}
	
	private Map<String,Set<EventManager>> exportedSearches = Collections.synchronizedMap(new HashMap<String,Set<EventManager>>());
	
	public void bindExportSearchState(String search, EventManager event)
	{
		File f = getExportFileFromQuery(search);
		
		synchronized(exportedSearches)
		{
			if(exportedSearches.containsKey(f.getName()))
			{
				exportedSearches.get(f.getName()).add(event);
			}
			else
			{
				Set<EventManager> set = new HashSet<EventManager>();
				set.add(event);
				
				exportedSearches.put(f.getName(), set);
			}
		}
		
		if(f.exists())
		{
			event.trigger(ExportState.CREATED);
		}
		else
		{
			event.trigger(ExportState.REMOVED);
		}
	}
	
	public void unbindExportSearchState(String search, EventManager event)
	{
		File f = getExportFileFromQuery(search);
		
		synchronized(exportedSearches)
		{
			if(exportedSearches.containsKey(f.getName()))
			{
				exportedSearches.get(f.getName()).remove(event);
				
				if(exportedSearches.get(f.getName()).size() == 0)
				{
					exportedSearches.remove(f.getName());
				}
			}
		}
	}

	public int localSearchCount(String query)
	{
		return download_manager.totalEntries(new SearchQuery(query));
	}
	
	public int pages(int results_per_page, String query)
	{
		query = prepareQuery(query);
		
		return (int) Math.ceil(((double)download_manager.totalEntries(new SearchQuery(query))) / results_per_page);
	}

	public enum FixState
	{
		TAGS,
		CORRUPT,
		FIXING,
	}

	public void fixMe(EventManager eventManager)
	{
		eventManager.trigger(FixState.TAGS);

		download_manager.fixTags(this,eventManager);

		eventManager.trigger(FixState.CORRUPT);

		ArrayList<E621DownloadedImage> images = localSearch(0,-1,"");

		final ArrayList<Integer> redownload = new ArrayList<Integer>();

		int i=0;

		for(final E621DownloadedImage image : images)
		{
			eventManager.trigger(new Pair<String,String>(""+(++i),""+images.size()));

            if(image.getType().equals("webm"))
            {
                InputStream is = getDownloadedImage(image);

                if(is == null)
                {
                    redownload.add(image.getId());
                }
                else
                {
                    MediaInputStreamPlayer player = new MediaInputStreamPlayer();

                    final Semaphore s = new Semaphore(1);

                    try
                    {
                        s.acquire();

                        player.setInputStreamCheckListener(new MediaInputStreamPlayer.InputStreamCheckListener() {
                            @Override
                            public void onCheck(boolean isOk) {
                                if(!isOk)
                                {
                                    redownload.add(image.getId());
                                }
                                s.release();
                            }
                        });
                        player.setVideoInputStream(is);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        redownload.add(image.getId());

                        s.release();
                    }
                    finally {
                    }

                    try {
                        s.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                    s.release();
                }
            }
            else if(image.getType().equals("jpg") || image.getType().equals("png") || image.getType().equals("gif"))
            {
                try {
                    Bitmap bmp = BitmapFactory.decodeStream(getDownloadedImage(image));

                    if (bmp == null || isBadBitmap(bmp)) {
                        redownload.add(image.getId());
                    }

                    if (bmp != null) bmp.recycle();
                } catch (Exception e) {
                    e.printStackTrace();

                    redownload.add(image.getId());
                } catch (Error e) {
                    e.printStackTrace();

                    redownload.add(image.getId());
                }
            }
		}

		eventManager.trigger(FixState.FIXING);

		i=0;

		for(int fix : redownload) {
            Log.d(LOG_TAG, "Fixing " + fix);

			eventManager.trigger(new Pair<String, String>("" + (++i), "" + redownload.size()));

			try
			{
				E621Image img = post__show(fix);

                deleteImage(img);

                saveImage(img);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private boolean isBadBitmap(Bitmap bmp)
	{
		int pixels[] = new int[bmp.getWidth()*bmp.getHeight()];

		bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

		int bad = 0;

		for(int px : pixels)
		{
			if(px == 0xFFFFFF00 || px == 0xFFFFFFFF || px == 0x00000000 || px == 0x000000FF)
			{
				bad++;
			}
			else
			{
				bad = 0;
			}
		}

		return bad > pixels.length*0.05;
	}

	public enum SyncState
	{
		REPORTS,
		FAILED_DOWNLOADS,
		CHECKING_FOR_UPDATES,
		INTERRUPTED_SEARCHES,
		BACKUP,
		FINISHED,
	}
	
	public void sync(EventManager eventManager)
	{
		Log.d(LOG_TAG,"Begin sync");

		eventManager.trigger(SyncState.REPORTS);

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

		eventManager.trigger(SyncState.FAILED_DOWNLOADS);
		
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
				final E621Image img2 = img;
				
				new Thread(new Runnable()
				{
					public void run()
					{
						saveImage(img2);
					}
				}).start();
			}
		}

		eventManager.trigger(SyncState.CHECKING_FOR_UPDATES);
		
		AndroidAppVersion version = getAndroidAppUpdater().getLatestVersionInfo();
		
		if(version != null)
		{
			updateMostRecentVersion(version);
		}

		eventManager.trigger(SyncState.INTERRUPTED_SEARCHES);

        eraseWebmThumbnails();

		syncSearch();

		try
		{
			updateOnlinePosts();
		} catch(IOException e)
		{
			e.printStackTrace();
		}

		eventManager.trigger(SyncState.BACKUP);
		
		backup();

		update_some_metadata(eventManager);

		eventManager.trigger(SyncState.FINISHED);
		
		Log.d(LOG_TAG,"End sync");
	}

    private void eraseWebmThumbnails()
    {
        String[] thumbs = webm_thumbnails.fileList();

        for(String webm : thumbs)
        {
            if(!download_manager.hasFile(Integer.parseInt(webm)))
            {
                webm_thumbnails.removeFile(webm);
            }
        }
    }
	
	Semaphore searchCountSemaphore = new Semaphore(10);
	
	public void syncSearch()
	{
		ArrayList<Thread> threads = new ArrayList<Thread>();
		
		isInterruptTriggerEnabled = false;
		
		for(final InterruptedSearch interrupted : getAllSearches())
		{
			Thread t = new Thread(new Runnable()
			{
				public void run()
				{
					try {
						searchCountSemaphore.acquire();
					} catch (InterruptedException e) {
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}

					update_new_image_count(interrupted.search);

					searchCountSemaphore.release();

					InputStream in = null;

					ArrayList<E621DownloadedImage> images = localSearch(0, 1, interrupted.search);
					int width = 64;
					int height = 64;
					
					if(images.size() > 0)
					{
						in = getDownloadedImageThumb(images.get(0));
						
						if(in == null) return;
						
						byte[] data;
						
						try {
							data = IOUtils.toByteArray(in);
						} catch (IOException e) {
							return;
						}
						
				        //Decode image size
				        BitmapFactory.Options o = new BitmapFactory.Options();
				        o.inJustDecodeBounds = true;
				        BitmapFactory.decodeStream(new ByteArrayInputStream(data),null,o);

				        //Find the correct scale value. It should be the power of 2.
				        int scale=1;
				        while(o.outWidth/scale/2>=width && o.outHeight/scale/2>=height)
				        {
				        	scale*=2;
				        }
				        
				        //Decode with inSampleSize
				        BitmapFactory.Options o2 = new BitmapFactory.Options();
				        o2.inSampleSize=scale;
				        Bitmap bitmap_temp = BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, o2);
				        
				        Bitmap ret = Bitmap.createScaledBitmap(bitmap_temp,width,height,false);
				        
				        bitmap_temp.recycle();
						
						interrupt.addThumbnail(interrupted.search, ret);
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
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
		
		isInterruptTriggerEnabled = true;
		
		triggerInterruptedSearchEvents();
	}
	
	public void backup()
	{
		JSONObject backup = getJSONBackup();
		
		if(backup == null) return;
		
		InputStream in = null;
		try {
			in = new ByteArrayInputStream(backup.toString().getBytes("UTF-8"));
			backupManager.backup(in);
			in.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		android.util.Log.d(E621Middleware.LOG_TAG + "_Backup",backupManager.toString());
	}
	
	public JSONObject getJSONBackup()
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
				return null;
			}
			
			interruptsArray.put(jsonSearch);
		}
		
		try {
			backup.put("downloads",downloadIDs);
			backup.put("interrupts",interruptsArray);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		
		return backup;
	}
	
	public ArrayList<Date> getBackups()
	{
		ArrayList<Long> backups = backupManager.getBackups();
		
		ArrayList<Date> ret = new ArrayList<Date>();
		
		for(Long l : backups)
		{
			ret.add(new Date(l));
		}
		
		return ret;
	}
	
	public static enum BackupStates
	{
		OPENING,
		READING,
		CURRENT,
		SEARCHES,
		SEARCHES_COUNT,
		GETTING_IMAGES,
		DELETING_IMAGES,
		INSERTING_IMAGES,
		DOWNLOADING_IMAGES,
		REMOVE_EMERGENCY,
		UPDATE_TAGS,
		SUCCESS,
		FAILURE,
	}
	
	private void restoreEmergencyBackup()
	{
		final GTFO<StepsProgressDialog> dialogWrapper = new GTFO<StepsProgressDialog>();
		dialogWrapper.obj = new StepsProgressDialog(ctx);
		dialogWrapper.obj.addStep("Emergency Backup found. Restoring it").showStepsMessage();
		dialogWrapper.obj.show();
		
		final BackupHandler handler = new BackupHandler(dialogWrapper.obj);
		
		try
		{
			final InputStream in = new BufferedInputStream(new FileInputStream(emergency_backup));
			
			new Thread(new Runnable()
			{
				public void run()
				{
					restoreBackup(in,true,new EventManager()
			    	{
			    		@Override
						public void onTrigger(Object obj)
			    		{
			    			Log.d(LOG_TAG+"_Backup",String.valueOf(obj));
			    			
			    			if(obj == BackupStates.SUCCESS || obj == BackupStates.FAILURE)
			    			{
			    				Message msg = handler.obtainMessage();
			    				handler.sendMessage(msg);
			    			}
						}
			    	});
					
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					emergency_backup.delete();
				}
			}).start();
		}
		catch(FileNotFoundException e)
		{
			return;
		}
	}
	
	private static class BackupHandler extends Handler
	{
		StepsProgressDialog dialog;
		
		public BackupHandler(StepsProgressDialog dialog)
		{
			this.dialog = dialog;
		}
		
		@Override
		public void handleMessage(Message msg)
		{
			dialog.dismiss();
		}
	}
	
	public boolean restoreBackup(Date date, boolean keep, EventManager event)
	{
		event.trigger(BackupStates.OPENING);
		
		InputStream in = backupManager.getBackup(date.getTime());
		
		if(in == null)
		{
			event.trigger(BackupStates.FAILURE);
			
			return false;
		}
		
		return restoreBackup(in,keep,event);
	}
		
	public boolean restoreBackup(InputStream in, boolean keep, EventManager event)
	{
		event.trigger(BackupStates.READING);
		
		JSONObject json;
		
		try
		{
			byte[] data = IOUtils.toByteArray(in);
			
			json = new JSONObject(new String(data, "UTF-8"));
		}
		catch (IOException e)
		{
			e.printStackTrace();
			event.trigger(BackupStates.FAILURE);
			return false;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			event.trigger(BackupStates.FAILURE);
			return false;
		}
		
		event.trigger(BackupStates.CURRENT);
		
		JSONObject current = getJSONBackup();
		
		try
		{
			emergency_backup.createNewFile();
			OutputStream out = new BufferedOutputStream(new FileOutputStream(emergency_backup));
			
			out.write(current.toString().getBytes("UTF-8"));
			
			out.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			event.trigger(BackupStates.FAILURE);
			return false;
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			event.trigger(BackupStates.FAILURE);
			return false;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			event.trigger(BackupStates.FAILURE);
			return false;
		}
		
		event.trigger(BackupStates.SEARCHES);
		
		int i=0;
		
		JSONArray searches = json.optJSONArray("interrupts");
		if(searches == null)
		{
			event.trigger(BackupStates.FAILURE);
			return false;
		}
		
		ArrayList<InterruptedSearch> interruptedSearches = new ArrayList<InterruptedSearch>();
		for(i=0; i<searches.length(); i++)
		{
			JSONObject obj;
			
			try
			{
				obj = searches.getJSONObject(i);
				
				interruptedSearches.add(new InterruptedSearch(obj.getString("search"),obj.optInt("min_id",-1),obj.optInt("max_id",-1),0));
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		
		event.trigger(BackupStates.GETTING_IMAGES);
		
		JSONArray downloads = json.optJSONArray("downloads");
		if(downloads == null)
		{
			event.trigger(BackupStates.FAILURE);
			return false;
		}
		
		ArrayList<E621DownloadedImage> currentDownloads = download_manager.search(0,-1,new SearchQuery(""));
		
		HashSet<Integer> backupDownloads = new HashSet<Integer>();
		for(i=0; i<downloads.length(); i++)
		{
			try
			{
				backupDownloads.add(downloads.getInt(i));
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		
		for(i=currentDownloads.size()-1; i>=0; i--)
		{
			Integer id = currentDownloads.get(i).getId();

			if(!download_manager.hasFile(currentDownloads.get(i)))
			{
				download_manager.removeFile(id);
				currentDownloads.remove(i);
				
				continue;
			}
			
			if(backupDownloads.contains(id))
			{
				currentDownloads.remove(i);
				backupDownloads.remove(id);
			}
		}
		
		if(!keep)
		{
			event.trigger(BackupStates.DELETING_IMAGES);
			
			for(E621DownloadedImage img : currentDownloads)
			{
				deleteImage(img.getId());
			}
		}
		
		event.trigger(BackupStates.INSERTING_IMAGES);
		
		for(Integer id : backupDownloads)
		{
			failed_download_manager.addFile(String.valueOf(id));
		}
		
		event.trigger(BackupStates.REMOVE_EMERGENCY);
		
		emergency_backup.delete();

		event.trigger(BackupStates.SEARCHES_COUNT);
		
		interrupt.setSearches(interruptedSearches);
		
		syncSearch();
		
		event.trigger(BackupStates.DOWNLOADING_IMAGES);
		
		final Semaphore s = new Semaphore(10);
		ArrayList<Thread> threads = new ArrayList<Thread>();
		
		for(final Integer id : backupDownloads)
		{
			failed_download_manager.addFile(String.valueOf(id));
			
			Thread t = new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						s.acquire();
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}
					
					try
					{
						saveImage(id);
					}
					finally
					{
						s.release();
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
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		event.trigger(BackupStates.UPDATE_TAGS);
		update_tags(event);
		
		event.trigger(BackupStates.SUCCESS);
		return true;
	}
	
	public void sendReport(final String message, final boolean errorReport)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
                sendReport(generateErrorReport(),message,errorReport);
			}
		}).start();
	}



	public void sendReport(final String report, final String message, final boolean errorReport)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				String message_trim = message.trim();

				String r = "";

				if(errorReport)
				{
					r = report;
				}

				if(message_trim.length() > 0)
				{
					r += "\n\n-----------\n\n" + message_trim;
				}

				try
				{
					sendReportOnline(r);
				}
				catch(ClientProtocolException e)
				{
					saveReportForLater(r);
				}
				catch (IOException e)
				{
					saveReportForLater(r);
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

	public ArrayList<E621Comment> comment__index(final Integer post_id)
	{
		final SparseArray<ArrayList<E621Comment>> comments = new SparseArray<ArrayList<E621Comment>>();

		final int STEPS = 5;
		int step = 0;

		do
		{
			Thread[] threads = new Thread[STEPS];

			final GTFO<Boolean> b = new GTFO<Boolean>();
			b.obj = false;

			for(int i=0; i<STEPS; i++)
			{
				final int ii = i+step;

				threads[i] = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						ArrayList<E621Comment> localComments = comment__index(post_id, ii);

						if(localComments == null)
						{
							localComments = new ArrayList<E621Comment>();
						}

						comments.put(ii,localComments);

						if(localComments.size() == 0)
						{
							b.obj = true;
						}
					}
				});

				threads[i].start();
			}

			for(Thread t : threads)
			{
				try
				{
					t.join();
				} catch (InterruptedException e)
				{
					e.printStackTrace();

					Thread.currentThread().interrupt();
				}
			}

			if(b.obj)
			{
				break;
			}

			step+=STEPS;
		}
		while(true);

		ArrayList<E621Comment> retComments = new ArrayList<E621Comment>();

		for(int i=0; i<comments.size(); i++)
		{
			if(comments.get(i) != null) retComments.addAll(comments.get(i));
		}

		return retComments;
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
				editor.apply();
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
		editor.apply();
	}
	
	public String getLoggedUser()
	{
		return login;
	}
	
	public boolean isLoggedIn()
	{
		return getLoggedUser() != null;
	}
	
	private Set<EventManager> continueSearchEvents = Collections.synchronizedSet(new HashSet<EventManager>());

	public void bindContinueSearch(EventManager event)
	{
		continueSearchEvents.add(event);
		
		event.trigger(getAllSearches());
	}
	
	public void unbindContinueSearch(EventManager event)
	{
		continueSearchEvents.remove(event);
	}
	
	private boolean isInterruptTriggerEnabled = true;
	
	public void triggerInterruptedSearchEvents()
	{
		if(!isInterruptTriggerEnabled) return;
		
		ArrayList<InterruptedSearch> searches = getAllSearches();
		
		for(EventManager event : continueSearchEvents)
		{
			event.trigger(searches);
		}
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
				
				triggerInterruptedSearchEvents();
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
				
				triggerInterruptedSearchEvents();
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
		
		triggerInterruptedSearchEvents();
	}
	
	HashMap<Pair<String,Integer>,E621Search> continue_cache = new HashMap<Pair<String,Integer>,E621Search>();
	
	@SuppressWarnings("unchecked")
	public E621Search continue_search(String search, int page, int limit) throws IOException
	{
		InterruptedSearch pair = interrupt.getSearch(search);
		
		if(pair == null || !pair.is_valid())
		{
			return post__index(search,page,limit);
		}
		
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
	
	public Bitmap getContinueSearchThumbnail(String search)
	{
		return interrupt.getThumbnail(search);
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
		
		ReadWriteLockerWrapper lock = new ReadWriteLockerWrapper();
		
		public FailedDownloadManager(File file)
		{
			this.file = file;
		}
		
		private ArrayList<String> getAllFiles()
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

		public ArrayList<String> getFiles()
		{
			final GTFO<ArrayList<String>> ret = new GTFO<ArrayList<String>>();
			
			lock.read(new Runnable()
			{
				public void run()
				{
					ret.obj = getAllFiles();
				}
			});
			
			return ret.obj;
		}
		
		private void setFiles(ArrayList<String> strings)
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
		
		public void addFile(final String file)
		{
			lock.write(new Runnable()
			{
				public void run()
				{
					ArrayList<String> files = getAllFiles();
					
					if(!files.contains(file))
					{
						files.add(file);
						setFiles(files);
					}
				}
			});
		}
		
		public void removeFile(final String file)
		{
			lock.write(new Runnable()
			{
				public void run()
				{
					ArrayList<String> files = getAllFiles();
					
					if(files.contains(file))
					{
						files.remove(file);
						setFiles(files);
					}
				}
			});
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
			this.min_id = (min_id != null && min_id >= 0? min_id : null);
			this.max_id = (max_id != null && max_id >= 0? max_id : null);
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
		protected File thumbnails_path;
		protected File db_path;

		DatabaseHelper dbHelper;
		
		ReadWriteLockerWrapper lock = new ReadWriteLockerWrapper();
		
		public InterruptedSearchManager(File path)
		{
			this.path = path;
			this.thumbnails_path = new File(path,"thumbnails/");
			this.db_path = new File(path,"db.sqlite3");
			
			if(this.thumbnails_path.exists() && this.thumbnails_path.isFile())
			{
				this.thumbnails_path.delete();
			}
			
			if(!this.thumbnails_path.exists())
			{
				this.thumbnails_path.mkdirs();
			}

			dbHelper = new DatabaseHelper();
		}

		private class DatabaseHelper extends SQLiteOpenHelper
		{
			private DatabaseHelper()
			{
				super(ctx, db_path.getAbsolutePath(), null, version);
			}

			@Override
			public void onCreate(SQLiteDatabase db)
			{
				db.execSQL("CREATE TABLE search (" +
								"search_query TEXT PRIMARY KEY" +
								", " +
								"seen_past UNSIGNED BIG INT" +
								", " +
								"seen_until UNSIGNED BIG INT" +
								");"
				);

				onUpgrade(db,0,version);
			}

			@Override
			public void onUpgrade(SQLiteDatabase db, int i, int i2)
			{
				while(i < i2)
				{
					update_db(db, ++i);
				}
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
		}

		public void addOrUpdateSearch(final String search, final String seen_past, final String seen_until)
		{
			lock.write(new Runnable()
			{
				public void run()
				{
					SQLiteDatabase db = dbHelper.getWritableDatabase();
					
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
				}
			});
		}
		
		public void update_new_image_count(final String search, final int new_image_count)
		{
			lock.write(new Runnable()
			{
				public void run()
				{
					SQLiteDatabase db = dbHelper.getWritableDatabase();
					
					update_new_image_count(search,new_image_count,db);
				}
			});
		}
		
		private void update_new_image_count(String search, int new_image_count, SQLiteDatabase db)
		{
			ContentValues values = new ContentValues();
			values.put("new_images", new_image_count);
			
			db.update("search", values, "search_query = ?", new String[]{search});
		}
		
		public void remove(final String search)
		{
			lock.write(new Runnable()
			{
				public void run()
				{
					SQLiteDatabase db = dbHelper.getWritableDatabase();
					
					remove(search,db);
				}
			});
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
			values.put("new_images", 0);
			
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
		
		public InterruptedSearch getSearch(final String search)
		{
			final GTFO<InterruptedSearch> ret = new GTFO<InterruptedSearch>();
			
			lock.read(new Runnable()
			{
				public void run()
				{
					SQLiteDatabase db = dbHelper.getReadableDatabase();
					
					ret.obj = getSearch(search,db);
				}
			});
			
			return ret.obj;
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
			final ArrayList<InterruptedSearch> searches = new ArrayList<InterruptedSearch>();
			
			lock.read(new Runnable()
			{
				public void run()
				{
					SQLiteDatabase db = dbHelper.getReadableDatabase();
					
					Cursor c = db.rawQuery("SELECT search_query, seen_past, seen_until, new_images FROM search ORDER BY -new_images, search_query;", null);
					
					if(!(c != null && c.moveToFirst()))
					{
						return;
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
				}
			});
			
			return searches;
		}
	
		public void setSearches(final ArrayList<InterruptedSearch> searches)
		{
			lock.write(new Runnable()
			{
				public void run()
				{
					SQLiteDatabase db = dbHelper.getWritableDatabase();
					db.beginTransaction();
					
					try
					{
						db.delete("search", "1", null);
						
						for(InterruptedSearch s : searches)
						{
							if(s.is_valid())
							{
								add(s.search,String.valueOf(s.min_id),String.valueOf(s.max_id),db);
							}
							else
							{
								add(s.search,null,null,db);
							}
						}
						
						db.setTransactionSuccessful();
					}
					finally
					{
						db.endTransaction();
					}
				}
			});
		}
		
		public void addThumbnail(String search, Bitmap bmp)
		{
			if(bmp == null) return;
			
			search = FileName.encodeFileName(search);
			
			try
			{
				File file = new File(thumbnails_path,search);
				
				if(file.exists() && file.isDirectory())
				{
					file.delete();
				}
				
				file.createNewFile();
				
				OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
				
				bmp.compress(Bitmap.CompressFormat.PNG, 0, out);
				
				out.close();
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		
		public Bitmap getThumbnail(String search)
		{
			search = FileName.encodeFileName(search);
			
			File file = new File(thumbnails_path,search);
			
			if(!file.exists()) return null;
			
			InputStream in = null;
			
			try
			{
				in = new BufferedInputStream(new FileInputStream(file));
				
				return BitmapFactory.decodeStream(in);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
				return null;
			}
			finally
			{
				if(in != null)
				{
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
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
		settings.edit().putStringSet("mascots", new HashSet<String>(ids)).apply();
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
	
	public AndroidAppUpdater getAndroidAppUpdater()
	{
		try
		{
			AndroidAppUpdater updater = new AndroidAppUpdater(new URL("http://beastarman.info/android/last_json/e621Mobile/"));

			updater.setBeta(betaReleases());

			return updater;
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static enum UpdateState
	{
		START,
		DOWNLOADED,
		SUCCESS,
		FAILURE,
	}

	public void updateApp(final AndroidAppVersion version, final EventManager event)
	{
		event.trigger(UpdateState.START);
		
		final GTFO<File> temp = new GTFO<File>();
		
		temp.obj = new File(sd_path, "e621Mobile_" + version.versionName + ".apk");
		
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					final HttpParams httpParams = new BasicHttpParams();
				    HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
					HttpClient client = new PersistentHttpClient(new DefaultHttpClient(),5);
					
					HttpResponse response = null;
					
					response = client.execute(new HttpGet(version.getFullApkURL()));
					
					StatusLine statusLine = response.getStatusLine();
					
				    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
				    {
				    	OutputStream out = new BufferedOutputStream(new FileOutputStream(temp.obj));
				    	
				    	response.getEntity().writeTo(out);
				    	
				    	out.close();
				    	
				    	event.trigger(UpdateState.DOWNLOADED);
				    	
				    	Intent i = new Intent();
				        i.setAction(Intent.ACTION_VIEW);
						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				        i.setDataAndType(Uri.fromFile(temp.obj),"application/vnd.android.package-archive");
				        ctx.startActivity(i);
				        
				        event.trigger(UpdateState.SUCCESS);
				    }
				    else
				    {
				    	event.trigger(UpdateState.FAILURE);
				    }
				}
				catch (IOException e)
				{
					event.trigger(UpdateState.FAILURE);
				}
			}
		}).start();
	}
	
	public ObjectStorage<Object> getStorage()
	{
		return searchStorage;
	}
	
	public boolean isWifiConnected()
	{
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return ((netInfo != null) && netInfo.isConnected());
    }

    public String getErrorReportHeader()
    {
        String log = "";

        PackageManager manager = ctx.getPackageManager();
        PackageInfo info = null;

        try
        {
            info = manager.getPackageInfo (ctx.getPackageName(), 0);
        }
        catch(PackageManager.NameNotFoundException e2)
        {

        }

        String model = Build.MODEL;

        if (!model.startsWith(Build.MANUFACTURER))
        {
            model = Build.MANUFACTURER + " " + model;
        }

        log +=	"Android version: " +  Build.VERSION.SDK_INT + "\n" +
                "Model: " + model + "\n" +
                "App version: " + (info == null ? "(null)" : info.versionCode);

        return log;
    }

    public String getErrorReportSettings()
    {
        String log = "";

        for(String key : settings.getAll().keySet())
        {
            if(key.equals("userPasswordHash"))
            {
                continue;
            }

            Object obj = settings.getAll().get(key);

            if(log.length() > 0)
            {
                log += "\n";
            }

            if(obj != null)
            {
                log += key + " = " + obj.toString();
            }
            else
            {
                log += key + " = null";
            }
        }

        return log;
    }

    public String getErrorReportLog()
    {
        String log = "";

        try {
            String[] get_log = {
                    "sh",
                    "-c",
                    "logcat -d -v time 2> /dev/null"
            };

            Process process = Runtime.getRuntime().exec(get_log);
            log += IOUtils.toString(process.getInputStream());
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }

        return log;
    }

    public String generateErrorReport()
    {
        String log = getErrorReportHeader() + "\n\n";

        log += getErrorReportSettings() + "\n\n";

        log += getErrorReportLog();

        return log;
    }

	public String resolveAlias(String query)
	{
		query = query.trim();

		String[] tags = query.split("\\s+");

		query = "";

		for(int i=0; i<tags.length; i++)
		{
			if(tags[0].contains(":"))
			{
				continue;
			}
			else if(tags[0].startsWith("-"))
			{
				tags[i] = "-" + download_manager.tags.tryResolveAlias(tags[i].substring(1,tags[0].length()));
			}
			else
			{
				tags[i] = download_manager.tags.tryResolveAlias(tags[i]);
			}

			query += " " + tags[i];
		}

		return query.trim();
	}

	public boolean hasMetadata()
	{
		return download_manager.hasTags();
	}

	DonationManager donationManager = new DonationManager(Uri.parse("http://beastarman.info/donations/ong/e621/"));

	public DonationManager getDonationManager()
	{
		return donationManager;
	}
}