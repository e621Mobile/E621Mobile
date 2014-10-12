package info.beastarman.e621.middleware;

import android.content.SharedPreferences;

public class E621BlackList extends BlackList
{
	E621Middleware e621;

	public E621BlackList(SharedPreferences settings, E621Middleware e621)
	{
		super(settings);
		this.e621 = e621;
	}

	public E621BlackList(SharedPreferences settings, String enabledName, String disabledName, E621Middleware e621)
	{
		super(settings, enabledName, disabledName);
		this.e621 = e621;
	}

	@Override
	public void enable(String query)
	{
		query = e621.resolveAlias(query);

		super.enable(query);
	}

	@Override
	public void disable(String query)
	{
		query = e621.resolveAlias(query);

		super.disable(query);
	}

	@Override
	public void remove(String query)
	{
		query = e621.resolveAlias(query);

		super.remove(query);
	}


}
