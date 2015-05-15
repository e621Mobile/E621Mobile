package info.beastarman.e621.frontend;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Map;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.Pair;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.views.AlphaFeatureFeedbackAlertDialog;
import info.beastarman.e621.views.LongPressCheckBoxPreference;

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
		boolean started = false;

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
		}

		@Override
		public void onStart()
		{
			super.onStart();

			if(started)
			{
				return;
			}

			started = true;

			ListView listView = (ListView) getView().findViewById(android.R.id.list);
			listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
			{
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
				{
					ListView listView = (ListView) parent;
					ListAdapter listAdapter = listView.getAdapter();
					Object obj = listAdapter.getItem(position);
					if(obj != null && obj instanceof View.OnLongClickListener)
					{
						View.OnLongClickListener longListener = (View.OnLongClickListener) obj;
						return longListener.onLongClick(view);
					}
					return false;
				}
			});

			Map<String, Pair<String, Boolean>> features = e621.alpha().getFeatures();

			for(final String feature : features.keySet())
			{
				LongPressCheckBoxPreference preference = new LongPressCheckBoxPreference(ctx)
				{
					@Override
					public boolean onLongClick(View v)
					{
						final AlphaFeatureFeedbackAlertDialog dialog = new AlphaFeatureFeedbackAlertDialog(ctx);
						dialog.setButton(Dialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialogInterface, int i)
							{
							}
						});
						dialog.setButton(Dialog.BUTTON_POSITIVE, "Send", new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialogInterface, int i)
							{
								e621.sendReport("Feature feedback: " + feature + "\n\n" + dialog.text(), dialog.sendStatistics());

								Toast.makeText(ctx, "Thank you for the feedback!", Toast.LENGTH_SHORT).show();
							}
						});
						dialog.show();

						return true;
					}
				};

				preference.setKey(e621.alpha().prepareKey(feature));
				preference.setTitle(feature);
				preference.setSummary(features.get(feature).left);
				preference.setDefaultValue(features.get(feature).right);

				getPreferenceScreen().addPreference(preference);
			}
		}
	}
}
