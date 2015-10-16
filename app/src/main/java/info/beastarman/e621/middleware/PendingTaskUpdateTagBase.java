package info.beastarman.e621.middleware;

import info.beastarman.e621.backend.PendingTask;

/**
 * Created by beastarman on 10/16/2015.
 */
public class PendingTaskUpdateTagBase extends PendingTask
{
	private final String updateTagBase = "updateTagBase";

	private E621Middleware e621Middleware;

	public PendingTaskUpdateTagBase(E621Middleware e621Middleware)
	{
		this.e621Middleware = e621Middleware;
	}

	@Override
	public boolean isNeeded()
	{
		return (!e621Middleware.settings.getBoolean(updateTagBase, false)) || (e621Middleware.download_manager.tags.getTagCount() < 10000);
	}

	@Override
	protected boolean runTask()
	{
		trigger(this);

		e621Middleware.force_update_tags(this);

		return true;
	}

	@Override
	protected void onComplete()
	{
		e621Middleware.settings.edit().putBoolean(updateTagBase, true).commit();
		super.onComplete();
	}

	public enum PendingTaskUpdateTagBaseStates
	{
		CLEANING,
		UPDATING,
	}
}
