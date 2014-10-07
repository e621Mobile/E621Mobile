package info.beastarman.e621.middleware;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class BlackList
{
	SharedPreferences settings;

	public BlackList(SharedPreferences settings)
	{
		this.settings = settings;
	}

	public HashMap<String,Boolean> getBlacklist()
	{
		Set<String> enabled = settings.getStringSet("enabledBlacklist",new HashSet<String>());
		Set<String> disabled = settings.getStringSet("disabledBlacklist",new HashSet<String>());

		HashMap<String,Boolean> blacklist = new HashMap<String,Boolean>();

		for(String s : enabled)
		{
			blacklist.put(s,true);
		}

		for(String s : disabled)
		{
			blacklist.put(s,false);
		}

		return blacklist;
	}

	public void enable(String query)
	{
		query = SearchQuery.normalize(query);

		SharedPreferences.Editor edit = settings.edit();

		Set<String> disabled = new HashSet<String>(settings.getStringSet("disabledBlacklist",new HashSet<String>()));
		if(disabled.contains(query))
		{
			disabled.remove(query);
			edit.putStringSet("disabledBlacklist",disabled);
		}

		Set<String> enabled = new HashSet<String>(settings.getStringSet("enabledBlacklist",new HashSet<String>()));
		if(!enabled.contains(query))
		{
			enabled.add(query);
			edit.putStringSet("enabledBlacklist",enabled);
		}

		edit.commit();
	}

	public void disable(String query)
	{
		query = SearchQuery.normalize(query);

		SharedPreferences.Editor edit = settings.edit();

		Set<String> disabled = new HashSet<String>(settings.getStringSet("disabledBlacklist",new HashSet<String>()));
		if(!disabled.contains(query))
		{
			disabled.add(query);
			edit.putStringSet("disabledBlacklist",disabled);
		}

		Set<String> enabled = new HashSet<String>(settings.getStringSet("enabledBlacklist",new HashSet<String>()));
		if(enabled.contains(query))
		{
			enabled.remove(query);
			edit.putStringSet("enabledBlacklist",enabled);
		}

		edit.commit();
	}

	public void remove(String query)
	{
		query = SearchQuery.normalize(query);

		SharedPreferences.Editor edit = settings.edit();

		Set<String> disabled = new HashSet<String>(settings.getStringSet("disabledBlacklist",new HashSet<String>()));
		if(disabled.contains(query))
		{
			disabled.remove(query);
			edit.putStringSet("disabledBlacklist",disabled);
		}

		Set<String> enabled = new HashSet<String>(settings.getStringSet("enabledBlacklist",new HashSet<String>()));
		if(enabled.contains(query))
		{
			enabled.remove(query);
			edit.putStringSet("enabledBlacklist",enabled);
		}

		edit.commit();
	}
}
