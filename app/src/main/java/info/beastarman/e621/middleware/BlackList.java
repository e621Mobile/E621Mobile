package info.beastarman.e621.middleware;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class BlackList
{
	SharedPreferences settings;
	String enabledName = "enabledBlacklist";
	String disabledName = "disabledBlacklist";

	public BlackList(SharedPreferences settings)
	{
		this.settings = settings;
	}

	public BlackList(SharedPreferences settings, String enabledName, String disabledName)
	{
		this.settings = settings;
		this.enabledName = enabledName;
		this.disabledName = disabledName;
	}

	public Set<String> getEnabled()
	{
		return new HashSet<String>(settings.getStringSet(enabledName, new HashSet<String>()));
	}

	public Set<String> getDisabled()
	{
		return new HashSet<String>(settings.getStringSet(disabledName, new HashSet<String>()));
	}

	public HashMap<String,Boolean> getBlacklist()
	{
		Set<String> enabled = getEnabled();
		Set<String> disabled = getDisabled();

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

		Set<String> disabled = getDisabled();
		if(disabled.contains(query))
		{
			disabled.remove(query);
			edit.putStringSet(disabledName,disabled);
		}

		Set<String> enabled = getEnabled();
		if(!enabled.contains(query))
		{
			enabled.add(query);
			edit.putStringSet(enabledName,enabled);
		}

		edit.commit();
	}

	public void disable(String query)
	{
		query = SearchQuery.normalize(query);

		SharedPreferences.Editor edit = settings.edit();

		Set<String> disabled = getDisabled();
		if(!disabled.contains(query))
		{
			disabled.add(query);
			edit.putStringSet(disabledName,disabled);
		}

		Set<String> enabled = getEnabled();
		if(enabled.contains(query))
		{
			enabled.remove(query);
			edit.putStringSet(enabledName,enabled);
		}

		edit.commit();
	}

	public void remove(String query)
	{
		query = SearchQuery.normalize(query);

		SharedPreferences.Editor edit = settings.edit();

		Set<String> disabled = getDisabled();
		if(disabled.contains(query))
		{
			disabled.remove(query);
			edit.putStringSet(disabledName,disabled);
		}

		Set<String> enabled = getEnabled();
		if(enabled.contains(query))
		{
			enabled.remove(query);
			edit.putStringSet(enabledName,enabled);
		}

		edit.commit();
	}
}
