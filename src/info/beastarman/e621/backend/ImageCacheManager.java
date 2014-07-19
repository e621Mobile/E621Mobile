package info.beastarman.e621.backend;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.IOUtils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class ImageCacheManager
{
	protected File base_path;
	protected File cache_file;
	
	protected int version = 0;
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()); 
	
	public long max_size;
	
	public File get_cache_file()
	{
		return this.cache_file;
	}
	
	public ImageCacheManager(File base_path, long max_size)
	{
		this.base_path = base_path;
		this.max_size = max_size;
		
		cache_file = new File(base_path, ".cache.sqlite3");
		
		getDB().close();
		
		clean();
	}
	
	protected SQLiteDatabase getDB()
	{
		SQLiteDatabase db;
		
		try
		{
			db = SQLiteDatabase.openDatabase(cache_file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
		}
		catch(SQLiteException e)
		{
			db = SQLiteDatabase.openOrCreateDatabase(cache_file, null);
			new_db(db);
		}
		
		setVersion(version,db);
		
		return db;
	}
	
	protected synchronized void new_db(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE images (" +
						"id TEXT PRIMARY KEY" +
						", " +
						"file_size UNSIGNED BIG INT" +
						", " +
						"last_access DATETIME DEFAULT CURRENT_TIMESTAMP" +
					");"
				);
	}
	
	protected void setVersion(int version, SQLiteDatabase db)
	{
		if(version < this.version)
		{
			return;
		}
		
		this.version = version;
		
		while(db.getVersion() < version)
		{
			update_db(db.getVersion()+1, db);
			
			db.setVersion(db.getVersion()+1);
		}
	}
	
	protected synchronized void update_db(int version, SQLiteDatabase db)
	{
	}
	
	public synchronized boolean hasFile(String id)
	{
		InputStream in = getFile(id);
		
		if(in == null)
		{
			return false;
		}
		else
		{
			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return true;
		}
	}
	
	public synchronized InputStream getFile(String id)
	{
		String[] query_params = new String[]{id};
		
		ContentValues updateMap = new ContentValues();
		updateMap.put("last_access", dateFormat.format(new Date()));
		
		SQLiteDatabase db = getDB();
		
		try
		{
			if(db.update("images", updateMap, "(SELECT id FROM images WHERE id = ?)", query_params) > 0)
			{
				try {
					return new FileInputStream(new File(base_path,id));
				} catch (FileNotFoundException e) {
					db.delete("images", "id = ?", query_params);
					return null;
				}
			}
			else
			{
				return null;
			}
		}
		finally
		{
			db.close();
		}
	}
	
	public synchronized void createOrUpdate(String id, InputStream in)
	{
		byte[] data;
		
		try {
			data = IOUtils.toByteArray(in);
		} catch (IOException e) {
			return;
		}
		
		ContentValues values = new ContentValues();
		values.put("id", id);
		values.put("file_size", data.length);
		values.put("last_access", dateFormat.format(new Date()));

		String[] query_params = new String[]{id};
		
		SQLiteDatabase db = getDB();
		
		try
		{
			try
			{
				db.insert("images", null, values);
			}
			catch(SQLiteException e)
			{
				values.remove("id");
				db.update("images", values, "(SELECT id FROM images WHERE id = ?)", query_params);
			}
			
			try {
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(base_path,id)));
				out.write(data);
				out.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		finally
		{
			db.close();
			
			clean();
		}
	}
	
	public synchronized void removeFile(String id)
	{
		String[] query_params = new String[]{id};
		
		SQLiteDatabase db = getDB();
		
		try
		{
			if(db.delete("images", "id = ?", query_params) > 0)
			{
				new File(base_path,id).delete();
			}
		}
		finally
		{
			db.close();
		}
	}
		
	public synchronized void clean()
	{
		if(max_size <= 0)
		{
			return;
		}
		
		ArrayList<String> remove_ids = new ArrayList<String>();
		
		SQLiteDatabase db = getDB();
		
		try
		{
			long to_remove = totalSize() - max_size;
			
			Cursor c = db.rawQuery("SELECT * FROM images ORDER BY last_access, file_size;", null);
			
			if(!(c != null && c.moveToFirst()))
			{
				return;
			}
			
			while(to_remove > 0)
			{
				remove_ids.add(c.getString(c.getColumnIndex("id")));
				to_remove -= c.getLong(c.getColumnIndex("file_size"));
				
				if(!c.moveToNext())
				{
					break;
				}
			}
			
			c.close();
		}
		finally
		{
			db.close();
		}
		
		for(String s : remove_ids)
		{
			removeFile(s);
		}
	}
	
	public synchronized long totalSize()
	{
		SQLiteDatabase db = getDB();
		
		try
		{
			Cursor c = db.rawQuery("SELECT SUM(file_size) as size FROM images;",null);
			
			long l = 0;
			
			if(c != null && c.moveToFirst())
			{
				l = c.getLong(c.getColumnIndex("size"));
				c.close();
			}
			
			return l;
		}
		finally
		{
			db.close();
		}
	}
}
