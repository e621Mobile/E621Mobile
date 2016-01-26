package info.beastarman.e621.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ImageCacheManager implements ImageCacheManagerInterface
{
	File base_path;
	File cache_file;
	File access_file;

	int version=1;

	public long max_size;
	public ReadWriteLockerWrapper lock = new ReadWriteLockerWrapper();

	private AccessWatcher accessWatcher;

	ImageDatabaseHelper imageDbHelper;

	SingleUseFileStorage singleUseFileStorage;

	private class ImageDatabaseHelper extends SQLiteOpenHelper
	{
		private ImageDatabaseHelper(Context ctx, File f)
		{
			super(ctx, f.getAbsolutePath(), null, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			try
			{
				db.execSQL("CREATE TABLE images (" +
								"id TEXT PRIMARY KEY" +
								", " +
								"file_size UNSIGNED BIG INT" +
								");");
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

			onUpgrade(db,0,version);
		}

		@Override
		public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2)
		{
		}
	}

	public ImageCacheManager(Context ctx, File base_path, long max_size)
	{
		this.max_size = max_size;
		this.base_path = base_path;

		cache_file = new File(base_path, ".cache.sqlite3");

		accessWatcher = new AccessWatcher(ctx, new File(base_path, ".access.sqlite3"));

		imageDbHelper = new ImageDatabaseHelper(ctx,cache_file);

		File fTemp = new File(base_path,"singleUseCache/");
		fTemp.mkdirs();
		singleUseFileStorage = new SingleUseFileStorage(fTemp);
	}

	@Override
	public boolean hasFile(final String id)
	{
		final GTFO<Boolean> ret = new GTFO<Boolean>();

		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = imageDbHelper.getReadableDatabase();
				Cursor c = null;

				try
				{
					c = db.rawQuery("SELECT 1 FROM images WHERE id=?", new String[]{id});

					if(c.getCount() == 0)
					{
						ret.obj = false;
						return;
					}

					ret.obj = new File(base_path, id).exists();
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});

		if(ret.obj)
		{
			if(max_size > 0) accessWatcher.insert(id);
		}

		return ret.obj;
	}

	@Override
	public InputStream getFile(final String id)
	{
		final GTFO<InputStream> ret = new GTFO<InputStream>();

		lock.read(new Runnable()
		{
			public void run()
			{
				if(hasFile(id))
				{
					InputStream is = null;

					try
					{
						is = new BufferedInputStream(new FileInputStream(new File(base_path, id)));
						ret.obj = singleUseFileStorage.store(is);
						is.close();
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
					finally
					{
						if(is != null)
						{
							try
							{
								is.close();
							}
							catch(IOException e)
							{
								e.printStackTrace();
							}
						}
					}
				}
			}
		});

		return ret.obj;
	}

	@Override
	public boolean createOrUpdate(final String id, final InputStream in)
	{
		if(!hasSpaceLeft()) return false;

		final boolean[] ret = new boolean[]{false};
		final File outputFile = new File(base_path, id);

		BufferedOutputStream out = null;
		try
		{
			out = new BufferedOutputStream(new FileOutputStream(outputFile));
			IOUtils.copy(in, out);
			out.close();
		}
		catch(Throwable e)
		{
			e.printStackTrace();
			outputFile.delete();
			return false;
		}

		lock.write(new Runnable()
		{
			public void run()
			{

				SQLiteDatabase db = imageDbHelper.getWritableDatabase();

				try
				{
					ContentValues values = new ContentValues();
					values.put("id", id);
					values.put("file_size", outputFile.length() + 4096);

					String[] query_params = new String[]{id};

					try
					{
						db.insert("images", null, values);
					}
					catch(SQLiteException e)
					{
						values.remove("id");
						db.update("images", values, "(SELECT id FROM images WHERE id = ?)", query_params);
					}

					ret[0] = true;
				}
				catch(Throwable throwable)
				{
					throwable.printStackTrace();
					outputFile.delete();
				}
			}
		});

		if(max_size > 0) accessWatcher.insert(id);

		return ret[0];
	}

	@Override
	public void removeFile(final String id)
	{
		lock.write(new Runnable()
		{
			public void run()
			{
				String[] query_params = new String[]{id};

				SQLiteDatabase db = imageDbHelper.getWritableDatabase();

				if(db.delete("images", "id = ?", query_params) > 0)
				{
					new File(base_path,id).delete();
				}
			}
		});

		if(max_size > 0) accessWatcher.remove(new String[]{id});
	}

	@Override
	public boolean hasSpaceLeft()
	{
		if(max_size < 1)
		{
			return true;
		}

		return totalSize() < max_size;
	}

	public void removeFiles(final String[] ids)
	{
		lock.write(new Runnable() {
			public void run() {
				SQLiteDatabase db = imageDbHelper.getWritableDatabase();
				db.beginTransaction();

				try {
					for (String id : ids) {
						if (db.delete("images", "id = ?", new String[]{id}) > 0) {
							new File(base_path, id).delete();
						}
					}

					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}
			}
		});

		if(max_size > 0) accessWatcher.remove(ids);
	}

	private HashMap<String,Long> getAllFiles()
	{
		final GTFO<HashMap<String,Long>> ret = new GTFO<HashMap<String,Long>>();
		ret.obj = new HashMap<String,Long>();

		lock.read(new Runnable() {
			public void run() {
				SQLiteDatabase db = imageDbHelper.getReadableDatabase();
				Cursor c = null;

				try {
					c = db.rawQuery("SELECT id, file_size FROM images", null);

					if (c == null || !c.moveToFirst()) {
						return;
					}

					while (!c.isAfterLast()) {
						ret.obj.put(
								c.getString(c.getColumnIndex("id")),
								c.getLong(c.getColumnIndex("file_size"))
						);

						c.moveToNext();
					}
				} finally {
					if (c != null) c.close();
				}
			}
		});

		return ret.obj;
	}

	@Override
	public void clear()
	{
		final HashMap<String,Long> files = getAllFiles();

		final ArrayList<String> toRemove = new ArrayList<String>();

		for(String id : files.keySet())
		{
			toRemove.add(id);
		}

		removeFiles((String[])toRemove.toArray(new String[toRemove.size()]));
	}

	@Override
	public void clean()
	{
		if(max_size < 1)
		{
			return;
		}

		final long size = totalSize();
		long local_max_size = (long) Math.floor(max_size*1.0);

		if(size > local_max_size)
		{
			final long remove_until = (long) Math.floor(max_size*0.8);

			new Thread(new Runnable()
			{
				public void run()
				{
					final HashMap<String,Long> files = getAllFiles();

					ArrayList<String> ids = accessWatcher.getIds();

					final ArrayList<String> toRemove = new ArrayList<String>();

					long ssize = size;

					while(ssize > remove_until && ids.size()>0)
					{
						String id = ids.get(0);

						if(files.containsKey(id))
						{
							toRemove.add(id);
							ids.remove(0);

							ssize -= files.get(id);
						}
					}

					removeFiles((String[])toRemove.toArray(new String[toRemove.size()]));
				}
			}).start();
		}
	}

	public long totalSize(final SQLiteDatabase db)
	{
		Cursor c = null;

		try
		{
			c = db.rawQuery("SELECT SUM(file_size) as size FROM images;",null);

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
			if(c!=null) c.close();
		}
	}

	@Override
	public long totalSize()
	{
		final GTFO<Long> ret = new GTFO<Long>();

		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = imageDbHelper.getReadableDatabase();

				ret.obj = totalSize(db);
			}
		});

		return ret.obj;
	}

	@Override
	public String[] fileList() {
		HashMap<String,Long> files = getAllFiles();

		return files.keySet().toArray(new String[files.keySet().size()]);
	}

	private class AccessWatcher
	{
		File database_file;
		ReadWriteLockerWrapper lock = new ReadWriteLockerWrapper();

		DatabaseHelper dbHelper;

		private class DatabaseHelper extends SQLiteOpenHelper
		{
			private DatabaseHelper(Context context, File f)
			{
				super(context, f.getAbsolutePath(), null, version);
			}

			@Override
			public void onCreate(SQLiteDatabase db)
			{
				try
				{
					db.execSQL("CREATE TABLE access (" +
									"id TEXT PRIMARY KEY" +
									", " +
									"last_access DATETIME DEFAULT CURRENT_TIMESTAMP" +
									");"
					);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}

				onUpgrade(db,0,version);
			}

			@Override
			public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2)
			{
			}
		}

		public AccessWatcher(Context ctx, File database_file)
		{
			this.database_file = database_file;

			dbHelper = new DatabaseHelper(ctx,database_file);
		}

		public void insert(final String id)
		{
			lock.writeAsync(new Runnable()
			{
				public void run()
				{
					final SQLiteDatabase db = dbHelper.getWritableDatabase();

					ContentValues values = new ContentValues();
					values.put("id", id);
					values.put("last_access", dateFormat.format(new Date()));

					if(db.insert("access", null, values) == -1)
					{
						values.remove("id");
						db.update("access", values, "id = ?", new String[]{id});
					}
				}
			});
		}

		public void remove(final String[] ids)
		{
			lock.writeAsync(new Runnable()
			{
				public void run()
				{
					final SQLiteDatabase db = dbHelper.getWritableDatabase();
					db.beginTransaction();

					try
					{
						for(String id : ids)
						{
							try
							{
								db.delete("access", "id=?", new String[]{id});
							}
							catch(SQLiteException e)
							{
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

		public ArrayList<String> getIds()
		{
			final GTFO<ArrayList<String>> ret = new GTFO<ArrayList<String>>();

			lock.read(new Runnable()
			{
				public void run()
				{
					ArrayList<String> ids = new ArrayList<String>();

					final SQLiteDatabase db = dbHelper.getReadableDatabase();
					Cursor c = null;

					try
					{
						c = db.rawQuery("SELECT id FROM access ORDER BY last_access", new String[]{});

						if(c == null || !c.moveToFirst()) return;

						while(!c.isAfterLast())
						{
							ids.add(c.getString(c.getColumnIndex("id")));

							c.moveToNext();
						}
					}
					finally
					{
						if(c != null) c.close();
					}

					ret.obj = ids;
				}
			});

			return ret.obj;
		}
	}

	@Override
	public void setMaxSize(long maxSize)
	{
		max_size = maxSize;
	}
}
