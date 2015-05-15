package info.beastarman.e621.backend;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

public class DirectImageCacheManager implements ImageCacheManagerInterface
{
	File base_path;

	int version=0;

	public long max_size;

	private HashMap<String,ReadWriteLockerWrapper> locks = new HashMap<String,ReadWriteLockerWrapper>();

	private synchronized ReadWriteLockerWrapper getLock(String id)
	{
		if(!locks.containsKey(id))
		{
			locks.put(id,new ReadWriteLockerWrapper());
		}

		return locks.get(id);
	}

	public DirectImageCacheManager(File base_path, long max_size)
	{
		this.max_size = max_size;
		this.base_path = base_path;

		clean();
	}

	private File[] listFiles()
	{
		File[] ret = base_path.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File file, String s) {
				return !s.equals(".nomedia");
			}
		});

		if(ret == null) ret = new File[0];

		return ret;
	}

	private File getFileObj(String id)
	{
		final Pattern p = Pattern.compile("\\d+ " + Pattern.quote(id)); // careful: could also throw an exception!
		File[] f = base_path.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return p.matcher(file.getName()).matches();
			}
		});

		if(f != null && f.length > 0)
		{
			return f[0];
		}
		else
		{
			return null;
		}
	}

	private synchronized File generateFile(String id)
	{
		File[] f = listFiles();

		Arrays.sort(f);

		long num = 1;

		if(f.length > 0)
		{
			File last = f[f.length-1];

			long localNum = Long.parseLong(last.getName().split("\\s")[0]);

			num = localNum+1;
		}

		String numStr = String.format("%0" + ((int) (Math.floor(Math.log10(Long.MAX_VALUE)) + 1)) + "d", num);

		File newFile = new File(base_path,numStr + " " + id);

		try
		{
			newFile.createNewFile();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return newFile;
	}

	@Override
	public boolean hasFile(final String id)
	{
		final GTFO<Boolean> ret = new GTFO<Boolean>();

		getLock(id).read(new Runnable()
		{
			@Override
			public void run()
			{
				ret.obj = getFileObj(id)!=null;
			}
		});

		return ret.obj;
	}

	@Override
	public InputStream getFile(final String id)
	{
		final GTFO<InputStream> ret = new GTFO<InputStream>();

		getLock(id).read(new Runnable()
		{
			@Override
			public void run()
			{
				File f = getFileObj(id);

				if(f != null && f.exists())
				{
					try
					{
						ret.obj = new BufferedInputStream(new FileInputStream(f));
						ret.obj = new ByteArrayInputStream(IOUtils.toByteArray(ret.obj));
					}
					catch (IOException e)
					{
					}
				}
			}
		});

		return ret.obj;
	}

	@Override
	public boolean createOrUpdate(final String id, final InputStream in)
	{
		final boolean[] ret = new boolean[]{false};

		getLock(id).write(new Runnable()
		{
			@Override
			public void run()
			{
				File obj = getFileObj(id);

				if(obj == null)
				{
					obj = generateFile(id);
				}

				try
				{
					OutputStream out = new BufferedOutputStream(new FileOutputStream(obj));
					IOUtils.copy(in, out);
					out.close();

					ret[0] = true;
				}
				catch (IOException e)
				{
					e.printStackTrace();

					if(obj.exists()) obj.delete();
				}
			}
		});

		return ret[0];
	}

	@Override
	public void removeFile(final String id)
	{
		getLock(id).write(new Runnable()
		{
			@Override
			public void run()
			{
				File f = getFileObj(id);

				if(f != null)
				{
					f.delete();
				}
			}
		});
	}

	public void removeFiles(final String[] ids)
	{
		for(String id : ids)
		{
			removeFile(id);
		}
	}
	
	private HashMap<String,Long> getAllFiles()
	{
		HashMap<String,Long> ret = new HashMap<String,Long>();

		for(File f : listFiles())
		{
			ret.put(f.getName().split("\\s",2)[1],f.length());
		}

		return ret;
	}
	
	@Override
	public void clear()
	{
		for(File f : listFiles())
		{
			removeFile(f.getName().split("\\s",2)[1]);
		}
	}
	
	@Override
	public void clean()
	{
		if(max_size < 1)
		{
			return;
		}
		
		long size = totalSize();
		long local_max_size = max_size;
		
		if(size > local_max_size)
		{
			final long remove_until = (long) Math.floor(max_size*0.8);

			File[] files = listFiles();

			Arrays.sort(files);

			for(File f : files)
			{
				size -= f.length();

				removeFile(f.getName().split("\\s",2)[1]);

				if (size <= remove_until)
				{
					break;
				}
			}
		}
	}

	@Override
	public long totalSize()
	{
		long size = 0;

		for(File f : listFiles())
		{
			size += f.length();
		}

		return size;
	}

	@Override
	public String[] fileList()
	{
		File[] ff = listFiles();

		String[] s = new String[ff.length];

		int i=0;

		for(File f : ff)
		{
			s[i++] = f.getName().split("\\s")[1];
		}

		return s;
	}

	@Override
	public void setMaxSize(long maxSize)
	{
		max_size = maxSize;
	}
}
