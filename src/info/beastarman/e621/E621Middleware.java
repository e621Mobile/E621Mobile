package info.beastarman.e621;

import info.beastarman.e621.api.E621;
import info.beastarman.e621.api.E621Image;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class E621Middleware extends E621
{
	HashMap<String,E621Image> e621ImageCache = new HashMap<String,E621Image>();
	
	File cache_path = null;
	File download_path = null;
	
	public static final String PREFS_NAME = "E621MobilePreferences";
	
	SharedPreferences settings;
	SharedPreferences.OnSharedPreferenceChangeListener settingsListener;
	
	public E621Middleware(Context ctx)
	{
		cache_path = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES),"cache/");
		download_path = new File(Environment.getExternalStorageDirectory(),"e621/");
		
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
		File file_path = new File(download_path,"e612 Images/" + img.id + "." + img.file_ext);
		
		return file_path.exists();
	}
	
	public void saveImage(E621Image img)
	{
		int size = getFileDownloadSize();
		
		File save_path = new File(download_path,"e612 Images/" + img.id + "." + img.file_ext);
		
		InputStream in = getImage(img,size);
		
		save_path.getParentFile().mkdirs();
		try {
			save_path.createNewFile();
			BufferedOutputStream file;
			file = new BufferedOutputStream(new FileOutputStream(save_path));
			file.write(IOUtils.toByteArray(in));
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void deleteImage(E621Image img)
	{
		File file_path = new File(download_path,"e612_images/" + img.id + "." + img.file_ext);
		
		if(file_path.exists())
		{
			file_path.delete();
		}
	}
	
	public InputStream getImage(E621Image img, int size)
	{
		if(size != E621Image.PREVIEW)
		{
		    HttpResponse response = null;
			try {
				if(size == E621Image.FULL)
				{
					response = tryHttpGet(img.file_url,5);
				}
				else
				{
					response = tryHttpGet(img.sample_url,5);
				}
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
					return new ByteArrayInputStream(out.toByteArray());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return null;
				}
		    }
		}
		
		File cache_local = new File(cache_path, img.id + "." + img.file_ext);
		
		try {
			return new FileInputStream(cache_local);
		} catch (FileNotFoundException e) {
		    HttpResponse response = null;
			try {
				response = tryHttpGet(img.preview_url,5);
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
		        
		        byte[] raw_file = out.toByteArray();
		        
		        try {
		        	cache_local.getParentFile().mkdirs();
		        	cache_local.createNewFile();
					BufferedOutputStream file;
					file = new BufferedOutputStream(new FileOutputStream(cache_local));
					file.write(raw_file);
					file.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return null;
				}
		        
		        return new ByteArrayInputStream(raw_file);
		    }
		}
		
		return null;
	}
}
