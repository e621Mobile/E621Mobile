package info.beastarman.e621;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsActivityNew extends SettingsActivity
{
	@Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            getPreferenceManager().setSharedPreferencesName(E621Middleware.PREFS_NAME);
            
            CheckBoxPreference hideDownload = (CheckBoxPreference)findPreference("hideDownloadFolder");
            hideDownload.setChecked(getPreferenceManager().getSharedPreferences().getBoolean("hideDownloadFolder", true));
            
            ListPreference downloadSize = (ListPreference)findPreference("prefferedFileDownloadSize");
            downloadSize.setValue(String.valueOf(getPreferenceManager().getSharedPreferences().getInt("prefferedFileDownloadSize", 2)));
        }
    }
}
