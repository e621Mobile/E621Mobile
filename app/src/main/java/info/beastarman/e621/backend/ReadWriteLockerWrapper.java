package info.beastarman.e621.backend;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockerWrapper
{
	private ReadWriteLock lock = new ReentrantReadWriteLock();

	public void read(Runnable run)
	{
		try
		{
			lock.readLock().lock();
			
			run.run();
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	public void readAsync(final Runnable run)
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					lock.readLock().lock();
					
					run.run();
				}
				finally
				{
					lock.readLock().unlock();
				}
			}
		}).start();
	}

	public void write(Runnable run)
	{
		try
		{
			while(!lock.writeLock().tryLock())
			{
				Thread.sleep(1000);
			}
			
			run.run();
		}
		catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}

	public void writeAsync(final Runnable run)
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					while(!lock.writeLock().tryLock())
					{
						Thread.sleep(1000);
					}
					
					run.run();
				}
				catch(InterruptedException e)
				{
					Thread.currentThread().interrupt();
					e.printStackTrace();
				}
				finally
				{
					lock.writeLock().unlock();
				}
			}
		}).start();
	}
}
