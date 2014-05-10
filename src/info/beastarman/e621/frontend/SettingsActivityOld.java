package info.beastarman.e621.frontend;

import info.beastarman.e621.R;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.views.SeekBarDialogPreference;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;

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
        
        SeekBarDialogPreference thumbnailCacheSize = (SeekBarDialogPreference)findPreference("thumbnailCacheSize");
        thumbnailCacheSize.setProgress(getPreferenceManager().getSharedPreferences().getInt("thumbnailCacheSize", 5));
        
        SeekBarDialogPreference fullCacheSize = (SeekBarDialogPreference)findPreference("fullCacheSize");
        fullCacheSize.setProgress(getPreferenceManager().getSharedPreferences().getInt("fullCacheSize", 10));
        
        Preference button = (Preference)getPreferenceManager().findPreference("updateTags");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                updateTags();
                return true;
            }
        });
    }
}
