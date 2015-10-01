package info.beastarman.e621.backend;

public abstract class PendingTask extends EventManagetMultiPlayer
{
	public enum States
	{
		START,
		CANCEL,
		COMPLETE
	}

	public boolean isNeeded()
	{
		return true;
	}

	public void start()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				trigger(States.START);

				if(runTask())
				{
					onComplete();
				}
				else
				{
					onCancel();
				}
			}
		}).start();
	}

	protected abstract boolean runTask();

	private boolean cancelled = false;

	public void cancel()
	{
		cancelled = true;
	}

	protected boolean isCancelled()
	{
		return cancelled;
	}

	protected void onCancel()
	{
		trigger(States.CANCEL);
	}

	protected void onComplete()
	{
		trigger(States.COMPLETE);
	}
}
