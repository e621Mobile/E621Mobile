package info.beastarman.e621.backend;

import java.util.HashMap;
import java.util.Map;

public class ObjectStorage<T>
{
	Map<Long, T> storage = new HashMap<Long, T>();
	
	public Long rent(T purse)
	{
		Long key;
		
		do
		{
			key = Math.round(Math.random() * Integer.MAX_VALUE);
		}
		while(storage.containsKey(key));
		
		storage.put(key, purse);
		
		return key;
	}
	
	public T returnKey(Long key)
	{
		return storage.remove(key);
	}
}
