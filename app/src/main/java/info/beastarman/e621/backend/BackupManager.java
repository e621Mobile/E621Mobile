package info.beastarman.e621.backend;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

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
	private long[] ls;
	
	public BackupManager(File backup_folder, long[] ls)
	{
		this.backup_folder = backup_folder.getAbsoluteFile();
		this.ls = ls;
	}
	
	private VersionManager versionManager = null;
	
	public ArrayList<Long> getBackups()
	{
		ArrayList<Long> backups = new ArrayList<Long>();
		
		if(backup_folder.listFiles() == null) return backups;
		
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
		
		Collections.sort(backups);
		
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
	
	public synchronized void backup(final InputStream in)
	{
		versionManager = getVersionManager();
		
		long now = System.currentTimeMillis();

		byte[] data = null;

		String dataMD5 = null;
		String backupMD5 = null;

		try {
			data = IOUtils.toByteArray(in);

			dataMD5 = DigestUtils.md5Hex(data);

			File mostRecentBackupFile = new File(backup_folder,String.valueOf(versionManager.getMostRecentVersion()));

			try
			{
				InputStream fin = new BufferedInputStream(new FileInputStream(mostRecentBackupFile));
				backupMD5 = DigestUtils.md5Hex(fin);
			}
			catch (IOException e3)
			{
				e3.printStackTrace();
			}
		}
		catch (IOException e2)
		{
			e2.printStackTrace();
			return;
		}

		if(backupMD5 != null && backupMD5.equals(dataMD5))
		{
			return;
		}

		try
		{
			in.reset();
		} catch (IOException e)
		{
			e.printStackTrace();

		}

		if(versionManager.addVersion(now))
		{
			try {
				File target_file = new File(backup_folder,String.valueOf(now));
				
				if(!target_file.createNewFile()) return;
				
				OutputStream out = new BufferedOutputStream(new FileOutputStream(target_file));
				
				out.write(data);
				
				out.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return;
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
	
	public InputStream getBackup(Long l)
	{
		File f = new File(backup_folder,String.valueOf(l));
		
		if(f.exists() && f.isFile())
		{
			try
			{
				return new BufferedInputStream(new FileInputStream(f));
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
				return null;
			}
		}
		
		return null;
	}
}
