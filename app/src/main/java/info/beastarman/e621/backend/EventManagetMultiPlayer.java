package info.beastarman.e621.backend;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by beastarman on 9/30/2015.
 */
public class EventManagetMultiPlayer extends EventManager
{
	private ArrayList<Object> triggerHistory = new ArrayList<Object>();
	private HashSet<EventManager> managers = new HashSet<EventManager>();

	public synchronized void addEventManager(EventManager eventManager)
	{
		managers.add(eventManager);

		for(Object obj : triggerHistory)
		{
			eventManager.trigger(obj);
		}
	}

	@Override
	public synchronized void onTrigger(Object obj)
	{
		triggerHistory.add(obj);

		for(EventManager eventManager : managers)
		{
			eventManager.trigger(obj);
		}
	}
}
