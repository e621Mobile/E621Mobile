package info.beastarman.e621.backend;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by beastarman on 1/25/2016.
 */


public class UsableStorages
{
	public List<UsableStorage> storages = new ArrayList<UsableStorage>();

	public static class UsableStorage
	{
		public String storagePath;
		public int sizeLimit;

		public UsableStorage(String storagePath)
		{
			this(storagePath,-1);
		}

		public UsableStorage(String storagePath, int sizeLimit)
		{
			this.sizeLimit = sizeLimit;
			this.storagePath = storagePath;
		}
	}
}
