package info.beastarman.e621.backend;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class ImageCacheManager implements ImageCacheManagerInterface
{
	File base_path;
	File cache_file;
	File access_file;

	int version=0;

	public long max_size;
	public ReadWriteLockerWrapper lock = new ReadWriteLockerWrapper();

	private AccessWatcher accessWatcher;

	public ImageCacheManager(File base_path, long max_size)
	{
		this.max_size = max_size;
		this.base_path = base_path;

		cache_file = new File(base_path, ".cache.sqlite3");

		accessWatcher = new AccessWatcher(new File(base_path, ".access.sqlite3"));

		getImageDB().close();

		clean();
	}

	static Semaphore s = new Semaphore(1);

	protected synchronized SQLiteDatabase getImageDB()
	{
		SQLiteDatabase db;

		try
		{
			s.acquire();
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}

		try
		{
			db = SQLiteDatabase.openDatabase(cache_file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
		} catch (SQLiteException e)
		{
			e.printStackTrace();
			db = SQLiteDatabase.openOrCreateDatabase(cache_file, null);
			newImageDB(db);
		}

		setImageDBVersion(version, db);

		s.release();

		return db;
	}

	protected void newImageDB(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE images (" +
						"id TEXT PRIMARY KEY" +
						", " +
						"file_size UNSIGNED BIG INT" +
						");"
		);
	}

	protected void setImageDBVersion(int version, SQLiteDatabase db)
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

	protected void update_db(int version, SQLiteDatabase db)
	{
	}

	@Override
	public boolean hasFile(final String id)
	{
		final GTFO<Boolean> ret = new GTFO<Boolean>();

		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = getImageDB();
				Cursor c = null;

				try
				{
					c = db.rawQuery("SELECT 1 FROM images WHERE id=?", new String[]{id});

					if(c.getCount() == 0)
					{
						ret.obj = false;
						return;
					}

					ret.obj = new File(base_path,id).exists();
				}
				finally
				{
					if(c != null) c.close();

					db.close();
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
					try
					{
						ret.obj = new BufferedInputStream(new FileInputStream(new File(base_path,id)));
						ret.obj = new ByteArrayInputStream(IOUtils.toByteArray(ret.obj));
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		});

		return ret.obj;
	}

	@Override
	public File createOrUpdate(final String id, final InputStream in)
	{
		lock.write(new Runnable()
		{
			public void run()
			{
				byte[] data;

				try {
					data = IOUtils.toByteArray(in);
				} catch (IOException e) {
					return;
				}

				ContentValues values = new ContentValues();
				values.put("id", id);
				values.put("file_size", data.length + 4096);

				String[] query_params = new String[]{id};

				SQLiteDatabase db = getImageDB();

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
		});

		if(max_size > 0) accessWatcher.insert(id);

		return new File(base_path,id);
	}

	@Override
	public void removeFile(final String id)
	{
		lock.write(new Runnable()
		{
			public void run()
			{
				String[] query_params = new String[]{id};

				SQLiteDatabase db = getImageDB();

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
		});

		if(max_size > 0) accessWatcher.remove(new String[]{id});
	}

	public void removeFiles(final String[] ids)
	{
		lock.write(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = getImageDB();
				db.beginTransaction();

				try
				{
					for(String id : ids)
					{
						if(db.delete("images", "id = ?", new String[]{id}) > 0)
						{
							new File(base_path,id).delete();
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
		});

		if(max_size > 0) accessWatcher.remove(ids);
	}

	private HashMap<String,Long> getAllFiles()
	{
		final GTFO<HashMap<String,Long>> ret = new GTFO<HashMap<String,Long>>();
		ret.obj = new HashMap<String,Long>();

		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = getImageDB();
				Cursor c = null;

				try
				{
					c = db.rawQuery("SELECT id, file_size FROM images",null);

					if(c==null || !c.moveToFirst())
					{
						return;
					}

					while(!c.isAfterLast())
					{
						ret.obj.put(
								c.getString(c.getColumnIndex("id")),
								c.getLong(c.getColumnIndex("file_size"))
						);

						c.moveToNext();
					}
				}
				finally
				{
					if(c != null) c.close();

					db.close();
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
				SQLiteDatabase db = getImageDB();

				try
				{
					ret.obj = totalSize(db);
				}
				finally
				{
					db.close();
				}
			}
		});

		return ret.obj;
	}

	private class AccessWatcher
	{
		File database_file;
		ReadWriteLockerWrapper lock = new ReadWriteLockerWrapper();

		public AccessWatcher(File database_file)
		{
			this.database_file = database_file;

			getDB().close();
		}

		Semaphore s = new Semaphore(1);

		private SQLiteDatabase getDB()
		{
			SQLiteDatabase db;

			try
			{
				s.acquire();
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}

			try
			{
				db = SQLiteDatabase.openDatabase(database_file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
			}
			catch(SQLiteException e)
			{
				e.printStackTrace();
				db = SQLiteDatabase.openOrCreateDatabase(database_file, null);
				newDB(db);
			}

			s.release();

			return db;
		}

		private void newDB(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE access (" +
							"id TEXT PRIMARY KEY" +
							", " +
							"last_access DATETIME DEFAULT CURRENT_TIMESTAMP" +
							");"
			);
		}

		public void insert(final String id)
		{
			lock.writeAsync(new Runnable()
			{
				public void run()
				{
					final SQLiteDatabase db = getDB();

					try
					{
						ContentValues values = new ContentValues();
						values.put("id", id);
						values.put("last_access", dateFormat.format(new Date()));

						if(db.insert("access", null, values) == -1)
						{
							values.remove("id");
							db.update("access", values, "id = ?", new String[]{id});
						}
					}
					finally
					{
						db.close();
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
					final SQLiteDatabase db = getDB();
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
						db.close();
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

					final SQLiteDatabase db = getDB();
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

						db.close();
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
