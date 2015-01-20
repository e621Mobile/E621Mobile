package info.beastarman.e621.middleware;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

import info.beastarman.e621.backend.Pair;

public class AlphaFeatures
{
	private SharedPreferences settings;
	private Map<String,String> registeredFeatures;

	public String prepareKey(String feature)
	{
		return ("AlphaFeature__" + feature);
	}

	public String prepareName(String feature)
	{
		return feature.replace("AlphaFeature__","");
	}

	public AlphaFeatures(SharedPreferences settings, Map<String,String> registeredFeatures)
	{
		this.settings = settings;
		this.registeredFeatures = registeredFeatures;

		SharedPreferences.Editor editor = settings.edit();

		for(String r : registeredFeatures.keySet())
		{
			if(!settings.contains(r))
			{
				editor.putBoolean(prepareKey(r),false);
			}
		}

		editor.apply();
	}

	public boolean isEnabled(String feature)
	{
		return registeredFeatures.containsKey(feature) && settings.getBoolean(prepareKey(feature),false);
	}

	public void enable(String feature)
	{
		settings.edit().putBoolean(prepareKey(feature),true).apply();
	}

	public void disable(String feature)
	{
		settings.edit().putBoolean(prepareKey(feature),false).apply();
	}

	public HashMap<String,Pair<String,Boolean>> getFeatures()
	{
		HashMap<String,Pair<String,Boolean>> features = new HashMap<String,Pair<String,Boolean>>();

		for(String feature : settings.getAll().keySet())
		{
			if(feature.startsWith("AlphaFeature__"))
			{
				if(!registeredFeatures.containsKey(prepareName(feature)))
				{
					continue;
				}

				features.put(prepareName(feature), new Pair(registeredFeatures.get(prepareName(feature)),isEnabled(feature)));
			}
		}

		return features;
	}
}
