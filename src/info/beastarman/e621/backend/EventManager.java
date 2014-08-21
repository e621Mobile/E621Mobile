package info.beastarman.e621.backend;

public abstract class EventManager
{
	public abstract void onTrigger(Object obj);
	
	public void trigger(Object obj)
	{
		onTrigger(obj);
	}
}
