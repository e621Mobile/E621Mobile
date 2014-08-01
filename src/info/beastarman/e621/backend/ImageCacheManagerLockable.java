package info.beastarman.e621.backend;

import java.io.InputStream;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class ImageCacheManagerLockable
{
	public ReadWriteLock lock = new ReentrantReadWriteLock();
	
	public ImageCacheManagerLockable()
	{
		super();
	}
	
	public boolean hasFile(String id)
	{
		try
		{
			lock.readLock().lock();
			
			return onHasFile(id);
		}
		finally
		{
			lock.readLock().unlock();
		}
	}
	
	protected abstract boolean onHasFile(String id);
	
	public InputStream getFile(String id)
	{
		try
		{
			lock.readLock().lock();
			
			return onGetFile(id);
		}
		finally
		{
			lock.readLock().unlock();
		}
	}
	
	protected abstract InputStream onGetFile(String id);
	
	public void createOrUpdate(String id, InputStream in)
	{
		try
		{
			lock.writeLock().lock();
			
			onCreateOrUpdate(id,in);
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}
	
	protected abstract void onCreateOrUpdate(String id, InputStream in);

	public final void removeFile(String id)
	{
		try
		{
			lock.writeLock().lock();
			
			onRemoveFile(id);
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}
	
	protected abstract void onRemoveFile(String id);
	
	public final void clean()
	{
		try
		{
			lock.writeLock().lock();
			
			onClean();
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}
	
	protected abstract void onClean();
	
	public final long totalSize()
	{
		try
		{
			lock.writeLock().lock();
			
			return onTotalSize();
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}
	
	protected abstract long onTotalSize();
}