package info.beastarman.e621;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;

public class SettingsActivityOld extends SettingsActivity
{
	@SuppressWarnings("deprecation")
	@Override
    protected void onCreate(final Bundle savedInstanceState)
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
