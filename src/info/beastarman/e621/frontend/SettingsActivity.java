package info.beastarman.e621.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.backend.GTFO;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.views.SeekBarDialogPreference;
import info.beastarman.e621.views.StepsProgressDialog;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity
{
	E621Middleware e621;
	
	@Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        e621 = E621Middleware.getInstance(getApplicationContext());
        
        MyPreferenceFragment fragment = new MyPreferenceFragment();
        fragment.activity = this;
        
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }
	
	protected void updateTags()
	{
		final ProgressDialog dialog = ProgressDialog.show(SettingsActivity.this, "","Updating tags. This may take an while. Please wait...", true);
		dialog.setIndeterminate(true);
		dialog.show();
		
		new Thread(new Runnable()
		{
			@Override
			public void run() {
				e621.update_tags();
				dialog.dismiss();
			}
		}).start();
	}
	
	protected void clearCache()
	{
		final ProgressDialog dialog = ProgressDialog.show(SettingsActivity.this, "","Clearing cache. Please wait...", true);
		dialog.setIndeterminate(true);
		dialog.show();
		
		new Thread(new Runnable()
		{
			@Override
			public void run() {
				e621.clearCache();
				dialog.dismiss();
			}
		}).start();
	}
	
	protected void restoreBackup(final Date date)
	{
		AlertDialog.Builder removeNewBuilder = new AlertDialog.Builder(this);
		removeNewBuilder.setMessage("Keep images not present on backup?");
		removeNewBuilder.setPositiveButton("Keep", new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				restoreBackup(date,true);
			}
		});
		removeNewBuilder.setNegativeButton("Delete", new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				restoreBackup(date,false);
			}
		});
		
		removeNewBuilder.create().show();
	}
	
	private void restoreBackup(final Date date, final boolean keep)
	{
		final GTFO<StepsProgressDialog> dialogWrapper = new GTFO<StepsProgressDialog>();
		dialogWrapper.obj = new StepsProgressDialog(this);
		dialogWrapper.obj.show();
		
		new Thread(new Runnable()
		{
			public void run()
			{
				final GTFO<String> message = new GTFO<String>();
				message.obj = "";
				
				e621.restoreBackup(date,keep,new EventManager()
		    	{
		    		@Override
					public void onTrigger(Object obj)
		    		{
		    			if(obj == E621Middleware.BackupStates.READING)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Reading current backup").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.CURRENT)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Creating emergency backup").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.SEARCHES)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Overriding saved searches").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.SEARCHES_COUNT)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Updating saved searches remaining images").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.REMOVE_EMERGENCY)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Removing emergency backup").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.GETTING_IMAGES)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Getting current images").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.DELETING_IMAGES)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Removing unnecessary images").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.INSERTING_IMAGES)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Inserting images").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.DOWNLOADING_IMAGES)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Downloading images").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.SUCCESS)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.setDone("Backup finished!");
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.FAILURE)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.setDone("Backup could not be restored!");
		    					}
		    				});
		    			}
					}
		    	});
			}
		}).start();
	}

    public static class MyPreferenceFragment extends PreferenceFragment
    {
    	SettingsActivity activity;
    	
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            getPreferenceManager().setSharedPreferencesName(E621Middleware.PREFS_NAME);
            
            CheckBoxPreference hideDownload = (CheckBoxPreference)findPreference("hideDownloadFolder");
            hideDownload.setChecked(getPreferenceManager().getSharedPreferences().getBoolean("hideDownloadFolder", true));
            
            CheckBoxPreference playGifs = (CheckBoxPreference)findPreference("playGifs");
            playGifs.setChecked(getPreferenceManager().getSharedPreferences().getBoolean("playGifs", true));
            
            CheckBoxPreference downloadInSearch = (CheckBoxPreference)findPreference("downloadInSearch");
            downloadInSearch.setChecked(getPreferenceManager().getSharedPreferences().getBoolean("downloadInSearch", true));
            
            ListPreference downloadSize = (ListPreference)findPreference("prefferedFileDownloadSize");
            downloadSize.setValue(String.valueOf(getPreferenceManager().getSharedPreferences().getInt("prefferedFileDownloadSize", 2)));

            SeekBarDialogPreference thumbnailCacheSize = (SeekBarDialogPreference)findPreference("thumbnailCacheSize");
            thumbnailCacheSize.setProgress(getPreferenceManager().getSharedPreferences().getInt("thumbnailCacheSize", 5));

            SeekBarDialogPreference resultsPerPage = (SeekBarDialogPreference)findPreference("resultsPerPage");
            resultsPerPage.setProgress(getPreferenceManager().getSharedPreferences().getInt("resultsPerPage", 2));
            
            SeekBarDialogPreference fullCacheSize = (SeekBarDialogPreference)findPreference("fullCacheSize");
            fullCacheSize.setProgress(getPreferenceManager().getSharedPreferences().getInt("fullCacheSize", 10));
            
            MultiSelectListPreference ratings = (MultiSelectListPreference)findPreference("allowedRatings");
            ratings.setValues(getPreferenceManager().getSharedPreferences().getStringSet("allowedRatings",new HashSet<String>()));
            
            Preference clearCache = (Preference)getPreferenceManager().findPreference("clearCache");
            clearCache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	activity.clearCache();
                    return true;
                }
            });
            
            ArrayList<Date> backups = activity.e621.getBackups();
            CharSequence[] entries = new CharSequence[backups.size()];
            CharSequence[] entriesValues = new CharSequence[backups.size()];
            
            for(int i=0; i<backups.size(); i++)
            {
            	entries[i] = backups.get(i).toString();
            	entriesValues[i] = String.valueOf(backups.get(i).getTime());
            }
            
            ListPreference restoreBackup = (ListPreference)getPreferenceManager().findPreference("restoreBackup");
            restoreBackup.setDefaultValue(null);
            restoreBackup.setEntries(entries);
            restoreBackup.setEntryValues(entriesValues);
            restoreBackup.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                	Log.d(E621Middleware.LOG_TAG,newValue.toString());
                	
                	activity.restoreBackup(new Date(Long.parseLong(newValue.toString())));
                	
                    return false;
                }
            });
            
            Preference allowedMascots = (Preference)getPreferenceManager().findPreference("allowedMascots");
            allowedMascots.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	E621MascotSelect fragment = new E621MascotSelect();
            		fragment.show(getFragmentManager(), "MascotSelect");
            		
            		return true;
                }
            });
            
            Preference button = (Preference)getPreferenceManager().findPreference("updateTags");
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	activity.updateTags();
                    return true;
                }
            });
            
            Preference aboutE621 = (Preference)getPreferenceManager().findPreference("aboutE621");
            aboutE621.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0)
                {
                	Intent i = new Intent(Intent.ACTION_VIEW);
                	i.setData(Uri.parse("https://e621.net/wiki/show?title=e621%3Aabout"));
                	startActivity(i);
                    return true;
                }
            });
            
            Preference sendErrorReport = (Preference)getPreferenceManager().findPreference("sendErrorReport");
            sendErrorReport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0)
                {
                	try {
            			String[] get_pid = {
            				"sh",
            				"-c",
            				"ps | grep info.beastarman.e621 | cut -c10-15"
            			};
            			
            			Process process = Runtime.getRuntime().exec(get_pid);
            			String pid = IOUtils.toString(process.getInputStream());
            			
            			String[] get_log = {
            				"sh",
            				"-c",
            				"logcat -d -v time | grep -e " + pid + " -e " + E621Middleware.LOG_TAG + " 2> /dev/null"
            			};
            			
            			process = Runtime.getRuntime().exec(get_log);
            			String log = IOUtils.toString(process.getInputStream());
            			
            			Intent intent = new Intent(activity.getApplicationContext(), ErrorReportActivity.class);
            			intent.putExtra(ErrorReportActivity.LOG, log);
            			startActivity(intent);
            		} catch (IOException e1)
            		{
            			e1.printStackTrace();
            		}
                	
                	return true;
                }
            });
            
            CheckBoxPreference lazyLoad = (CheckBoxPreference)findPreference("lazyLoad");
            lazyLoad.setChecked(getPreferenceManager().getSharedPreferences().getBoolean("lazyLoad", true));
        }
    }
}
