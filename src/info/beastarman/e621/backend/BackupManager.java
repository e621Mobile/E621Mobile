package info.beastarman.e621.backend;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class BackupManager
{
	private static class VersionManager
	{
		private static class VersionInfo
		{
			private ArrayList<Long> currentVersions = new ArrayList<Long>();
			private Long baseVersion;
			private Long limit;
			
			public String toString()
			{
				String currentString = "";
				
				for(Long l : currentVersions)
				{
					currentString = currentString + String.valueOf(l) + " ";
				}
				
				return String.valueOf(baseVersion) + ": " + currentString + "[" + minNext() + "]";
			}
			
			public VersionInfo(long baseVersion, long limit)
			{
				this.baseVersion = baseVersion;
				this.limit = limit;
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
					
					if(currentVersions.size() > limit)
					{
						currentVersions.subList(0,(int)(currentVersions.size()-limit)).clear();
					}
					
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
					m = 0l;
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
			
			int i=0;
			
			for(i=1; i<bases.length; i++)
			{
				versions.add(new VersionInfo(bases[i-1],(long)Math.ceil(((double)bases[i])/bases[i-1])-1));
			}
			
			versions.add(new VersionInfo(bases[i-1],1));
		}
		
		public boolean addVersion(long version)
		{
			VersionInfo current = null;
			
			for(VersionInfo v : versions)
			{
				if(v.canAdd(version))
				{
					current = v;
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
		
		public ArrayList<Long> getAllVersions()
		{
			ArrayList<Long> versions = new ArrayList<Long>();
			
			for(VersionInfo v : this.versions)
			{
				versions.addAll(v.getCurrentVersions());
			}
			
			Collections.sort(versions);
			
			return versions;
		}
		
		public String toString()
		{
			String temp = "";
			
			for(VersionInfo v : versions)
			{
				temp = temp + v.toString() + "\n";
			}
			
			return temp;
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
	
	private ArrayList<Long> getBackups()
	{
		ArrayList<Long> backups = new ArrayList<Long>();
		
		for(File f : backup_folder.listFiles())
		{
			try
			{
				Long stamp = Long.parseLong(f.getName());
				
				backups.add(stamp);
			}
			catch (NumberFormatException e)
			{
			}
		}
		
		return backups;
	}
	
	private VersionManager getVersionManager()
	{
		VersionManager versionManager = new VersionManager(ls);
		
		for(Long l : getBackups())
		{
			versionManager.addVersion(l);
		}
		
		return versionManager;
	}
	
	public String toString()
	{
		return getVersionManager().toString();
	}
	
	public synchronized void backup()
	{
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
		
		ArrayList<Long> versions = versionManager.getAllVersions();
		
		for(Long l : getBackups())
		{
			if(!versions.contains(l))
			{
				File f = new File(backup_folder,String.valueOf(l));
				f.delete();
			}
		}
	}
}
