package info.beastarman.e621.middleware;

import info.beastarman.e621.api.E621;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.backend.ImageCacheManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

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
	ImageCacheManager download_manager;
	
	public E621Middleware(Context ctx)
	{
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
			download_manager = new ImageCacheManager(download_path,0);
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
	public ArrayList<E621Image> post__index(String tags, Integer page, Integer limit) throws IOException
	{
		ArrayList<E621Image> ret = super.post__index(tags, page, limit);
		
		if(ret != null)
		{
			for(E621Image img : ret)
			{
				e621ImageCache.put(img.id, img);
			}
		}
		
		return ret;
	}
	
	public boolean isSaved(E621Image img)
	{
		return download_manager.hasFile(img.id + "." + img.file_ext);
	}
	
	public void saveImage(E621Image img)
	{
		InputStream in = getImage(img,getFileDownloadSize());
		
		if(in != null)
		{
			download_manager.createOrUpdate(img.id + "." + img.file_ext, in);
		}
	}
	
	public void deleteImage(E621Image img)
	{
		download_manager.removeFile(img.id + "." + img.file_ext);
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
			InputStream in = download_manager.getFile(img.id + "." + img.file_ext);
			
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
			InputStream in = download_manager.getFile(img.id + "." + img.file_ext);
			
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
}
