package info.beastarman.e621.frontend;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.Map;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.Pair;
import info.beastarman.e621.middleware.E621Middleware;

public class AlphaSettingsActivity extends PreferenceActivity
{
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		AlphaPreferenceFragment fragment = new AlphaPreferenceFragment();

		getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
	}

	public static class AlphaPreferenceFragment extends PreferenceFragment
	{
		E621Middleware e621;
		Context ctx;

		@Override
		public void onAttach(Activity act)
		{
			super.onAttach(act);

			e621 = E621Middleware.getInstance(act);
			ctx = act;
		}

		@Override
		public void onCreate(final Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.alpha_settings);
			getPreferenceManager().setSharedPreferencesName(E621Middleware.PREFS_NAME);

			Map<String,Pair<String,Boolean>> features = e621.alpha().getFeatures();

			for(String feature : features.keySet())
			{
				CheckBoxPreference preference = new CheckBoxPreference(ctx);
				preference.setKey(e621.alpha().prepareKey(feature));
				preference.setTitle(feature);
				preference.setSummary(features.get(feature).left);
				preference.setChecked(features.get(feature).right);

				getPreferenceScreen().addPreference(preference);
			}
		}
	}
}
