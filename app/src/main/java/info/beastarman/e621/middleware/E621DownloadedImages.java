package info.beastarman.e621.middleware;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Tag;
import info.beastarman.e621.api.E621TagAlias;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.backend.GTFO;
import info.beastarman.e621.backend.ImageCacheManager;
import info.beastarman.e621.backend.Pair;
import info.beastarman.e621.backend.ReadWriteLockerWrapper;

public class E621DownloadedImages
{
	File base_path;
	File image_tag_file;
	ImageCacheManager images;
	public E621TagDatabase tags;

	int version = 1;
	
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
				db.execSQL("CREATE TABLE image_tag (" +
								"image UNSIGNED INTEGER" +
								", " +
								"tag UNSIGNED INTEGER" +
								", " +
								"PRIMARY KEY(image,tag)" +
								", " +
								"FOREIGN KEY (image) REFERENCES e621image(id)" +
								");"
				);

				db.execSQL("CREATE TABLE e621image (" +
								"image_file TEXT" +
								", " +
								"id UNSIGNED INTEGER" +
								", " +
								"rating VARCHAR(1)" +
								", " +
								"width INTEGER DEFAULT 1" +
								", " +
								"height INTEGER DEFAULT 1" +
								", " +
								"PRIMARY KEY(id)" +
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

	public E621DownloadedImages(Context ctx, File base_path)
	{
		this.base_path = base_path;
		this.image_tag_file = new File(base_path,".image_tag.sqlite3");
		this.images = new ImageCacheManager(ctx,base_path,0);
		this.tags = new E621TagDatabase(ctx, new File(base_path,".tags.sqlite3"));

		dbHelper = new DatabaseHelper(ctx,image_tag_file);
	}
	
	public E621Tag getTag(String name)
	{
		return tags.getTag(name);
	}
	
	public ArrayList<E621Tag> getTags(String[] names)
	{
		return tags.getTags(names);
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
						sql = sql + String.format(" rating=\"%1$s\" ", value);
					}
				}
                else if(meta.equals("type"))
                {
                    sql = sql + " AND";
                    sql = sql + String.format(" image_file LIKE \"%%.%1$s\" ", value);
                }
                else if(meta.equals("id"))
                {
                    sql = sql + " AND";
                    sql = sql + String.format(" id = \"%1$s\" ", value);
                }
			}
			else
			{
				E621Tag tag = tags.getTag(tags.tryResolveAlias(s));
				
				if(tag == null) continue;
				
				s = String.valueOf(tag.getId());
				
				sql = sql + " AND";
				sql = sql + String.format(" EXISTS(SELECT 1 FROM image_tag WHERE image=e621image.id AND tag=\"%1$s\") ", s);
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
					
					if(meta.equals("rating"))
					{
						value = value.substring(0, 1);
						
						if(value.equals(E621Image.SAFE) || value.equals(E621Image.QUESTIONABLE) || value.equals(E621Image.EXPLICIT))
						{
							sql = sql + " OR";
							sql = sql + String.format(" rating=\"%1$s\" ", value);
						}
					}
					else if(meta.equals("type"))
					{
						sql = sql + " OR";
						sql = sql + String.format(" image_file LIKE \"%%.%1$s\" ", value);
					}
                    else if(meta.equals("id"))
                    {
                        sql = sql + " OR";
                        sql = sql + String.format(" id = \"%1$s\" ", value);
                    }
				}
				else
				{
					E621Tag tag = tags.getTag(tags.tryResolveAlias(s));
					
					if(tag == null) continue;
					
					s = String.valueOf(tag.getId());
					
					sql = sql + " OR";
					sql = sql + String.format(" EXISTS(SELECT 1 FROM image_tag WHERE image=e621image.id AND tag=\"%1$s\") ", s);
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
							sql = sql + String.format(" rating=\"%1$s\" ", value);
						}
					}
					else if(meta.equals("type"))
					{
						sql = sql + " OR";
						sql = sql + String.format(" image_file LIKE \"%%.%1$s\" ", value);
					}
                    else if(meta.equals("id"))
                    {
                        sql = sql + " OR";
                        sql = sql + String.format(" id = \"%1$s\" ", value);
                    }
				}
				else
				{
					E621Tag tag = tags.getTag(tags.tryResolveAlias(s));
					
					if(tag == null) continue;
					
					s = String.valueOf(tag.getId());
					
					sql = sql + " OR";
					sql = sql + String.format(" EXISTS(SELECT 1 FROM image_tag WHERE image=e621image.id AND tag=\"%1$s\") ", s);
				}
			}
			
			sql = sql + " ) ";
		}

		return sql;
	}

	public ArrayList<E621DownloadedImage> search(final int page, final int limit, SearchQuery query)
	{
		final String search_query = toSql(query);
		
		final GTFO<ArrayList<E621DownloadedImage>> ret = new GTFO<ArrayList<E621DownloadedImage>>();
		ret.obj = new ArrayList<E621DownloadedImage>();
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					c = db.rawQuery("SELECT id, image_file, width, height FROM e621image WHERE " + search_query + " ORDER BY id LIMIT ? OFFSET ?;",new String[]{String.valueOf(limit),String.valueOf(limit*page)});
					
					if(c == null || !c.moveToFirst())
					{
						return;
					}
					
					while(!c.isAfterLast())
					{
						ret.obj.add(new E621DownloadedImage(
								c.getInt(c.getColumnIndex("id")),
								c.getString(c.getColumnIndex("image_file")),
								c.getInt(c.getColumnIndex("width")),
								c.getInt(c.getColumnIndex("height"))
							));
						
						c.moveToNext();
					}
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});
		
		return ret.obj;
	}

	public Integer totalEntries(SearchQuery query)
	{
		final String search_query = toSql(query);
		
		final GTFO<Integer> ret = new GTFO<Integer>();
		ret.obj = 0;
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					c = db.rawQuery("SELECT COUNT(1) AS c FROM e621image WHERE " + search_query + ";",null);
					
					if(c == null || !c.moveToFirst())
					{
						return;
					}
					
					ret.obj = c.getInt(c.getColumnIndex("c"));
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});
		
		return ret.obj;
	}
	
	public boolean hasFile(final E621DownloadedImage img)
	{
		return images.hasFile(img.filename);
	}
	
	public boolean hasFile(final E621Image img)
	{
		return hasFile(img.id);
	}
	
	private String getFileName(final Integer id)
	{
		final GTFO<String> ret = new GTFO<String>();
		ret.obj = null;
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					c = db.rawQuery("SELECT image_file FROM e621image WHERE id = ?", new String[]{String.valueOf(id)});
					
					if(c == null || !c.moveToFirst())
					{
						return;
					}
					
					ret.obj = c.getString(c.getColumnIndex("image_file"));
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});
		
		return ret.obj;
	}
	
	private String getFileName(final E621Image img)
	{
		return getFileName(img.id);
	}
	
	public boolean hasFile(final Integer id)
	{
		if(id == null) return false;
		
		final GTFO<Boolean> ret = new GTFO<Boolean>();
		ret.obj = false;
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					c = db.rawQuery("SELECT image_file FROM e621image WHERE id = ?", new String[]{String.valueOf(id)});
					
					ret.obj = (c != null && c.moveToFirst());
					
					if(ret.obj)
					{
						ret.obj = images.hasFile(c.getString(c.getColumnIndex("image_file")));
					}
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});
		
		return ret.obj;
	}
	
	public InputStream getFile(final Integer id)
	{
		if(id == null)
		{
			return null;
		}
		
		E621Image img = new E621Image();
		img.id = id;
		
		return getFile(img);
	}
	
	public InputStream getFile(final E621Image img)
	{
		if(img == null) return null;
		
		String file_name = getFileName(img);
		
		if(file_name != null)
		{
			return images.getFile(file_name);
		}
		else
		{
			return null;
		}
	}
	
	public InputStream getFile(final E621DownloadedImage img)
	{
		if(img == null) return null;
		
		String file_name = img.filename;
		
		if(file_name != null)
		{
			return images.getFile(file_name);
		}
		else
		{
			return null;
		}
	}
	
	public void removeFile(final Integer id)
	{
		if(id == null) return;
		
		final String file_name = getFileName(id);
		
		lock.write(new Runnable()
		{
			public void run()
			{
				if(file_name != null)
				{
					images.removeFile(file_name);
				}
				
				SQLiteDatabase db = dbHelper.getWritableDatabase();
				
				db.delete("image_tag", "image = ?", new String[]{String.valueOf(id)});
				db.delete("e621image", "id = ?", new String[]{String.valueOf(id)});
			}
		});
	}
	
	public void removeFile(final E621Image img)
	{
		if(img == null) return;
		
		removeFile(img.id);
	}
	
	public void createOrUpdate(final E621Image img, final InputStream in, final String file_ext)
	{
		final String file_name = img.id + "." + file_ext;
		
		lock.write(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getWritableDatabase();

				ContentValues values = new ContentValues();
				values.put("id", img.id);
				values.put("image_file", file_name);
				values.put("rating", img.rating);
				values.put("width", img.width);
				values.put("height", img.height);

				db.insert("e621image", null, values);

				for(E621Tag tag : img.tags)
				{
					tag = tags.getTag(tag.getTag());

					if(tag != null)
					{
						values = new ContentValues();
						values.put("image", img.id);
						values.put("tag", tag.getId());

						db.insert("image_tag", null, values);
					}
				}
			}
		});
		
		images.createOrUpdate(file_name, in);
	}

	public void fixTags(E621Middleware e621, EventManager em)
	{
		final ArrayList<Integer> cur_ids = new ArrayList<Integer>();

		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;

				try
				{
					c = db.rawQuery("SELECT e621image.id AS id, COUNT(image_tag.image) AS tags FROM e621image INNER JOIN image_tag ON e621image.id = image_tag.image GROUP BY e621image.image_file HAVING (tags <= 5);", null);
					//c = db.rawQuery("SELECT e621image.id AS id, COUNT(image_tag.image) AS tags FROM e621image INNER JOIN image_tag ON e621image.id = image_tag.image WHERE (e621image.image_file LIKE '%.jpg' OR e621image.image_file LIKE '%.png' OR e621image.image_file LIKE '%.gif') GROUP BY e621image.image_file HAVING (tags <= 5);", null);

					if(c == null || !c.moveToFirst())
					{
						return;
					}

					while(!c.isAfterLast())
					{
						cur_ids.add(c.getInt(c.getColumnIndex("id")));

						c.moveToNext();
					}
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});

		if(cur_ids.size() == 0)
		{
			return;
		}

		retrieveMetadata(e621, em, cur_ids);
	}

	public enum UpdateStates
	{
		CLEANING,
		TAG_SYNC,
		TAG_ALIAS_SYNC,
		IMAGE_TAG_SYNC,
		IMAGE_TAG_DB,
		COMPLETED,
	}
	
	public synchronized void updateMetadataForce(E621Middleware e621, EventManager em)
	{
		android.util.Log.i(E621Middleware.LOG_TAG + "_Meta","Cleaning Metadata");
		em.trigger(UpdateStates.CLEANING);
		cleanTagBase();
		
		updateMetadata(e621, em);
	}
	
	public synchronized void updateMetadata(E621Middleware e621, EventManager em)
	{
		android.util.Log.i(E621Middleware.LOG_TAG + "_Meta","Starting Tag sync");
		em.trigger(UpdateStates.TAG_SYNC);
		updateTagBase(e621,em);
		
		android.util.Log.i(E621Middleware.LOG_TAG + "_Meta","Starting Tag Alias sync");
		em.trigger(UpdateStates.TAG_ALIAS_SYNC);
		updateTagAliasBase(e621,em);
		
		android.util.Log.i(E621Middleware.LOG_TAG + "_Meta","Starting Image Tag sync");
		em.trigger(UpdateStates.IMAGE_TAG_SYNC);
		updateImageTags(e621,em);
		
		android.util.Log.i(E621Middleware.LOG_TAG + "_Meta","Sync completed");
		em.trigger(UpdateStates.COMPLETED);
	}

	public synchronized int updateMetadataPartial(E621Middleware e621, int breakPoint, EventManager em)
	{
		android.util.Log.i(E621Middleware.LOG_TAG + "_Meta","Starting Tag sync");
		em.trigger(UpdateStates.TAG_SYNC);
		updateTagBase(e621,em);

		android.util.Log.i(E621Middleware.LOG_TAG + "_Meta","Starting Tag Alias sync");
		em.trigger(UpdateStates.TAG_ALIAS_SYNC);
		updateTagAliasBase(e621,em);

		android.util.Log.i(E621Middleware.LOG_TAG + "_Meta","Starting Image Tag sync");
		em.trigger(UpdateStates.IMAGE_TAG_SYNC);
		breakPoint = updateImageTagsPartial(e621, breakPoint, em);

		android.util.Log.i(E621Middleware.LOG_TAG + "_Meta","Sync completed");
		em.trigger(UpdateStates.COMPLETED);

		return breakPoint;
	}
	
	private void cleanTagBase()
	{
		this.tags.clean();
		
		lock.write(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getWritableDatabase();
				db.beginTransaction();
				
				try
				{
					db.delete("image_tag", "1", null);
					
					db.setTransactionSuccessful();
				}
				finally
				{
					db.endTransaction();
				}
			}
		});
	}
	
	private void updateTagBase(E621Middleware e621, EventManager em)
	{
		Integer max_id = null;
		
		E621Tag max = tags.getMaxTag();
		
		if(max != null) max_id = max.getId();
		
		int page = 0;
		ArrayList<E621Tag> tags;
		
		do
		{
			em.trigger(new Pair<String,String>((1+page)+"","?"));

			tags = null;

			while(tags == null)
			{
				tags = e621.tag__index(10000, page, null, null, max_id, null, null);
			}
			
			this.tags.addTag((E621Tag[])tags.toArray(new E621Tag[tags.size()]));
			
			page++;
		}
		while(tags.size() == 10000);
	}
	
	private void updateTagAliasBase(final E621Middleware e621, EventManager em)
	{
		Integer max_id = null;
		
		E621TagAlias max = tags.getMaxTagAlias();
		
		if(max != null) max_id = max.alias_id;
		
		boolean stop = false;
		int page = 0;
		final int steps = 10;
		
		while(!stop)
		{
			ArrayList<Thread> threads = new ArrayList<Thread>();
			final ArrayList<ArrayList<E621TagAlias>> rets = new ArrayList<ArrayList<E621TagAlias>>();
			
			final int ppage = page;

			em.trigger(new Pair<String,String>((1+page)+"","?"));
			
			for(int i=0; i<steps; i++)
			{
				final int delta = i;
				
				Thread t = new Thread(new Runnable()
				{
					public void run()
					{
						ArrayList<E621TagAlias> aliases = e621.tag_alias__index(true, "date", ppage*steps + delta);
						
						if(aliases == null)
						{
							aliases = e621.tag_alias__index(true, "date", ppage*steps + delta);
						}
						
						if(aliases != null)
						{
							rets.add(aliases);
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
			
			if(rets.size() == 0)
			{
				stop = true;
			}
			
			for(ArrayList<E621TagAlias> aliases : rets)
			{
				if(aliases.size() == 0) stop = true;
				
				for(E621TagAlias alias : aliases)
				{
					if(alias.id <= max_id)
					{
						stop=true;
					}
				}
				
				tags.addTagAlias((E621TagAlias[])aliases.toArray(new E621TagAlias[aliases.size()]));
			}
			
			page++;
		}
	}
	
	private void updateImageTags(final E621Middleware e621, EventManager em)
	{
		final ArrayList<Integer> cur_ids = new ArrayList<Integer>();
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					c = db.rawQuery("SELECT id FROM e621image ORDER BY id;", null);
					
					if(c == null || !c.moveToFirst())
					{
						return;
					}
					
					while(!c.isAfterLast())
					{
						cur_ids.add(c.getInt(c.getColumnIndex("id")));
						
						c.moveToNext();
					}
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});

		retrieveMetadata(e621, em, cur_ids);
	}

	private int updateImageTagsPartial(final E621Middleware e621, final int breakPoint, EventManager em)
	{
		final ArrayList<Integer> cur_ids = new ArrayList<Integer>();

		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;

				try
				{
					c = db.rawQuery("SELECT id FROM e621image ORDER BY id LIMIT 100 OFFSET ?;", new String[]{breakPoint+""});

					if(c == null || !c.moveToFirst())
					{
						return;
					}

					while(!c.isAfterLast())
					{
						cur_ids.add(c.getInt(c.getColumnIndex("id")));

						c.moveToNext();
					}
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});

		if(cur_ids.size() == 0 && breakPoint != 0)
		{
			return updateImageTagsPartial(e621, 0, em);
		}

		retrieveMetadata(e621, em, cur_ids);

		return breakPoint + 100;
	}

	private void retrieveMetadata(final E621Middleware e621, final EventManager em, ArrayList<Integer> cur_ids)
	{
		final List<E621Image> images = Collections.synchronizedList(new ArrayList<E621Image>());

		final int steps = 20;

		int totalSize = cur_ids.size();

		while(cur_ids.size() > 0)
		{
			em.trigger(new Pair<String,String>((totalSize-cur_ids.size())+"",totalSize+""));

			ArrayList<Thread> threads = new ArrayList<Thread>();

			for(int i=0; i<steps && cur_ids.size() > i; i++)
			{
				final int id = cur_ids.get(i);

				Thread t = new Thread(new Runnable()
				{
					public void run()
					{
						try {
							images.add(e621.post__show(id));
						} catch (IOException e) {
							e.printStackTrace();
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

			cur_ids.subList(0, Math.min(cur_ids.size(),steps)).clear();
		}

		em.trigger(UpdateStates.IMAGE_TAG_DB);

		final HashMap<String,Integer> tag_map = tags.getAllTagsAsHashMap();

		lock.write(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getWritableDatabase();
				db.beginTransaction();

				try
				{
					int i=0;

					for(E621Image img : images)
					{
						i++;

						em.trigger(new Pair<String,String>(i+"",images.size()+""));

						for(E621Tag tag : img.tags)
						{
							if(!tag_map.containsKey(tag.getTag()))
							{
								continue;
							}

							ContentValues values = new ContentValues();
							values.put("image", img.id);
							values.put("tag", tag_map.get(tag.getTag()));

							db.insert("image_tag", null, values);
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

	public long totalSize()
	{
		return images.totalSize();
	}

	public boolean hasTags()
	{
		E621Tag maxTag = tags.getMaxTag();

		if(maxTag == null) return false;

		return maxTag.getId() != 0;
	}
}
