package info.beastarman.e621.backend;

public abstract class PendingTask
{
	public enum States
	{
		CANCEL,
		COMPLETE
	}

	public void start(final EventManager eventManager)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				if(runTask(eventManager))
				{
					onComplete(eventManager);
				}
				else
				{
					onCancel(eventManager);
				}
			}
		});
	}

	protected abstract boolean runTask(EventManager eventManager);

	private boolean cancelled = false;

	public void cancel()
	{
		cancelled = true;
	}

	public boolean isCancelled()
	{
		return cancelled;
	}

	public void onCancel(final EventManager eventManager)
	{
		eventManager.trigger(States.CANCEL);
	}

	public void onComplete(final EventManager eventManager)
	{
		eventManager.trigger(States.COMPLETE);
	}
}
