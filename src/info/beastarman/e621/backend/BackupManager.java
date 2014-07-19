package info.beastarman.e621.backend;

import info.beastarman.e621.middleware.E621Middleware;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import android.util.Log;

public class BackupManager
{
	private class VersionManager
	{
		private class VersionInfo
		{
			private ArrayList<Long> currentVersions = new ArrayList<Long>();
			private Long baseVersion;
			
			public VersionInfo(long baseVersion)
			{
				this.baseVersion = baseVersion;
			}
			
			public ArrayList<Long> getCurrentVersions()
			{
				return new ArrayList<Long>(currentVersions);
			}
			
			public boolean add(long version)
			{
				if(canAdd(version))
				{
					currentVersions.add(version);
					
					return true;
				}
				else
				{
					return false;
				}
			}
			
			public boolean canAdd(long version)
			{
				return version >= minNext();
			}
			
			public long minNext()
			{
				Long m = max();
				
				if(m == null)
				{
					m = -1l;
				}
				
				return m+baseVersion;
			}
			
			public Long max()
			{
				if(currentVersions.size() == 0) return null;
				
				Long max = currentVersions.get(0);
				
				for(Long cur : currentVersions.subList(1,currentVersions.size()))
				{
					max = (cur>max? cur: max);
				}
				
				return max;
			}
		}
		
		private ArrayList<VersionInfo> versions;
		
		public VersionManager(long [] bases)
		{
			versions = new ArrayList<VersionInfo>();
			
			for(long l : bases)
			{
				versions.add(new VersionInfo(l));
			}
		}
		
		public boolean addVersion(long version)
		{
			VersionInfo current = null;
			
			for(VersionInfo v : versions)
			{
				if(v.canAdd(version))
				{
					if(current == null || current.minNext() > v.minNext())
					{
						current = v;
					}
				}
			}
			
			if(current != null)
			{
				current.add(version);
				
				return true;
			}
			
			return false;
		}
		
		public Long getMostRecentVersion()
		{
			Long max = versions.get(0).max();
			
			for(VersionInfo v : versions.subList(1,versions.size()))
			{
				if(v.max() == null) continue;
				
				if(max == null || max < v.max())
				{
					max = v.max();
				}
			}
			
			return max;
		}
	};
	
	private File backup_folder;
	private File origin_file;
	private long[] ls;
	
	public BackupManager(File backup_folder, File origin_file, long[] ls)
	{
		this.backup_folder = backup_folder.getAbsoluteFile();
		this.origin_file = origin_file.getAbsoluteFile();
		this.ls = ls;
	}
	
	private VersionManager versionManager = null;
	
	private VersionManager getVersionManager()
	{
		VersionManager versionManager = new VersionManager(ls);
		
		for(File f : backup_folder.listFiles())
		{
			try
			{
				Long stamp = Long.parseLong(f.getName());
				
				versionManager.addVersion(stamp);
			}
			catch (NumberFormatException e)
			{
			}
		}
		
		return versionManager;
	}
	
	public synchronized void backup()
	{
		Log.d(E621Middleware.LOG_TAG,"Backup");
		
		versionManager = getVersionManager();
		
		long now = System.currentTimeMillis();
		
		if(origin_file.exists() && origin_file.canRead())
		{
			try {
				if(FileUtils.contentEquals(origin_file,new File(backup_folder,String.valueOf(versionManager.getMostRecentVersion()))))
				{
					return;
				}
			}
			catch (IOException e1)
			{
				return;
			}
			
			if(versionManager.addVersion(now))
			{
				try {
					File target_file = new File(backup_folder,String.valueOf(now));
					
					if(!target_file.createNewFile()) return;
					
					InputStream in = new BufferedInputStream(new FileInputStream(origin_file));
					
					OutputStream out = new BufferedOutputStream(new FileOutputStream(target_file));
					
					IOUtils.copy(in, out);
					
					in.close();
					
					out.close();
				}
				catch (IOException e)
				{
					return;
				}
			}
		}
	}
}
