package info.beastarman.e621.backend;

import org.apache.commons.lang3.ArrayUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by beastarman on 1/21/2016.
 */
public class CombinedImageCacheManager implements ImageCacheManagerInterface
{
	List<ImageCacheManagerInterface> managers = new ArrayList<ImageCacheManagerInterface>();

	public CombinedImageCacheManager(List<ImageCacheManagerInterface> managers)
	{
		this.managers.addAll(managers);
	}

	@Override
	public boolean hasFile(String id)
	{
		for(ImageCacheManagerInterface i : managers)
		{
			if(i.hasFile(id)) return true;
		}

		return false;
	}

	@Override
	public InputStream getFile(String id)
	{
		for(ImageCacheManagerInterface i : managers)
		{
			InputStream is = i.getFile(id);

			if(is != null) return is;
		}

		return null;
	}

	@Override
	public boolean createOrUpdate(String id, InputStream in)
	{
		for(ImageCacheManagerInterface i : managers)
		{
			if(!i.hasSpaceLeft()) continue;

			return i.createOrUpdate(id,in);
		}

		return false;
	}

	@Override
	public void removeFile(String id)
	{
		for(ImageCacheManagerInterface i : managers)
		{
			i.removeFile(id);
		}
	}

	@Override
	public boolean hasSpaceLeft()
	{
		for(ImageCacheManagerInterface i : managers)
		{
			if(i.hasSpaceLeft()) return true;
		}

		return false;
	}

	@Override
	public void clean()
	{
		for(ImageCacheManagerInterface i : managers)
		{
			i.clean();
		}
	}

	@Override
	public long totalSize()
	{
		long total =  0;

		for(ImageCacheManagerInterface i : managers)
		{
			total += i.totalSize();
		}

		return total;
	}

	@Override
	public String[] fileList()
	{
		String[] files = new String[0];

		for(ImageCacheManagerInterface i : managers)
		{
			files = ArrayUtils.addAll(files,i.fileList());
		}

		return files;
	}

	@Override
	public void setMaxSize(long maxSize)
	{
	}

	@Override
	public void removeFiles(String[] ids)
	{
		for(ImageCacheManagerInterface i : managers)
		{
			i.removeFiles(ids);
		}
	}

	@Override
	public void clear()
	{
		for(ImageCacheManagerInterface i : managers)
		{
			i.clear();
		}
	}
}
