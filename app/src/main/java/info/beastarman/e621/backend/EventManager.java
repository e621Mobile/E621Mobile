package info.beastarman.e621.backend;

import java.io.Serializable;

public abstract class EventManager implements Serializable
{
	public abstract void onTrigger(Object obj);
	
	public void trigger(Object obj)
	{
		onTrigger(obj);
	}
}
