package info.beastarman.e621.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.backend.GTFO;
import info.beastarman.e621.middleware.AndroidAppUpdater;
import info.beastarman.e621.middleware.AndroidAppUpdater.AndroidAppVersion;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.views.SeekBarDialogPreference;
import info.beastarman.e621.views.StepsProgressDialog;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.text.Html;
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
	
	protected void donate()
	{
		Intent i = new Intent(this,DonateActivity.class);
		startActivity(i);
	}

	protected void updateTags()
	{
		final ProgressDialog dialog = ProgressDialog.show(SettingsActivity.this, "","Updating tags. This may take an while. Please wait...", true);
		dialog.setIndeterminate(true);
		dialog.show();
		
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				e621.update_tags();
				
				dialog.dismiss();
			}
		}).start();
	}

	protected void forceUpdateTags()
	{
		AlertDialog.Builder confirmFullUpdateBuilder = new AlertDialog.Builder(this);
		confirmFullUpdateBuilder.setMessage("Are you sure? This will take an while.");
		confirmFullUpdateBuilder.setPositiveButton("Continue", new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialogInterface, int which)
			{
				final ProgressDialog dialog = ProgressDialog.show(SettingsActivity.this, "","Forcing tags update. This will take an while. Please wait...", true);
				dialog.setIndeterminate(true);
				dialog.show();
				
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						e621.force_update_tags();
						
						dialog.dismiss();
					}
				}).start();
			}
		});
		confirmFullUpdateBuilder.setNegativeButton("Cancel", new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				
			}
		});
		
		confirmFullUpdateBuilder.create().show();
	}
	
	protected void clearCache()
	{
		final ProgressDialog dialog = ProgressDialog.show(SettingsActivity.this, "","Clearing cache. Please wait...", true);
		dialog.setIndeterminate(true);
		dialog.show();
		
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
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
		    			if(obj == E621Middleware.BackupStates.OPENING)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Opening current backup").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.CURRENT)
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
		    						dialogWrapper.obj.allowDismiss();
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
		    			else if(obj == E621Middleware.BackupStates.UPDATE_TAGS)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Updating tags").showStepsMessage();
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
	
	private static class FailException extends Exception
	{
		private static final long serialVersionUID = 1615513842090522333L;
		
		public int code;
		
		public FailException(int code)
		{
			this.code = code;
		}
	};
	
	protected void update()
	{
		final AndroidAppUpdater appUpdater = e621.getAndroidAppUpdater();
		
		new Thread(new Runnable()
		{
			public void run()
			{
				PackageInfo pInfo = null;
				
				try
				{
					try {
						pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
					} catch (NameNotFoundException e) {
						e.printStackTrace();
						throw new FailException(0);
					}
					
					int currentVersion = pInfo.versionCode;
					final AndroidAppVersion version = appUpdater.getLatestVersionInfo();
					
					e621.updateMostRecentVersion(version);
					
					if(version == null)
					{
						throw new FailException(1);
					}
					
					if(version.versionCode > currentVersion)
					{
						final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SettingsActivity.this).setTitle("New Version Found").setCancelable(true).
								setMessage(String.format(getResources().getString(R.string.new_version_found),version.versionName));
						
						runOnUiThread(new Runnable()
						{
							public void run()
							{
								final AlertDialog dialog = dialogBuilder.create();
								
								dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Update", new OnClickListener()
								{
									@Override
									public void onClick(DialogInterface arg0,int arg1)
									{
										dialog.dismiss();
										
										final GTFO<StepsProgressDialog> dialogWrapper = new GTFO<StepsProgressDialog>();
										dialogWrapper.obj = new StepsProgressDialog(SettingsActivity.this);
										dialogWrapper.obj.show();
										
										e621.updateApp(version, new EventManager()
										{
											@Override
											public void onTrigger(Object obj)
											{
												if(obj == E621Middleware.UpdateState.START)
								    			{
								    				runOnUiThread(new Runnable()
								    				{
								    					public void run()
								    					{
								    						dialogWrapper.obj.addStep("Retrieving package file").showStepsMessage();
								    					}
								    				});
								    			}
								    			else if(obj == E621Middleware.UpdateState.DOWNLOADED)
								    			{
								    				runOnUiThread(new Runnable()
								    				{
								    					public void run()
								    					{
								    						dialogWrapper.obj.addStep("Package downloaded").showStepsMessage();
								    					}
								    				});
								    			}
								    			else if(obj == E621Middleware.UpdateState.SUCCESS)
								    			{
								    				runOnUiThread(new Runnable()
								    				{
								    					public void run()
								    					{
								    						dialogWrapper.obj.setDone("Starting package install");
								    					}
								    				});
								    			}
								    			else if(obj == E621Middleware.UpdateState.FAILURE)
								    			{
								    				runOnUiThread(new Runnable()
								    				{
								    					public void run()
								    					{
								    						dialogWrapper.obj.setDone("Package could not be retrieved");
								    					}
								    				});
								    			}
											}
										});
									}
								});
								
								dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"Maybe later", new OnClickListener()
								{
									@Override
									public void onClick(DialogInterface arg0,int arg1)
									{
										dialog.dismiss();
									}
								});
								
								dialog.show();
							}
						});
					}
					else
					{
						throw new FailException(2);
					}
				}
				catch(FailException e)
				{
					final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SettingsActivity.this).setTitle("Update").
							setCancelable(true);
					
					switch(e.code)
					{
						case 1:
							dialogBuilder.setMessage("Could not retrieve latest version");
							break;
						case 2:
							dialogBuilder.setMessage("No newer version found");
							break;
						default:
							dialogBuilder.setMessage("Unknown error happened");
							break;
					}
					
					runOnUiThread(new Runnable()
					{
						public void run()
						{
							final AlertDialog dialog = dialogBuilder.create();
							
							dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Ok", new OnClickListener()
							{
								@Override
								public void onClick(DialogInterface arg0,int arg1)
								{
									dialog.dismiss();
								}
							});
							
							dialog.show();
						}
					});
				}
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

            Preference about = (Preference)getPreferenceManager().findPreference("about");
            about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0)
                {
                	String title;
					try {
						title = "About E621Mobile " + activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
					} catch (NameNotFoundException e) {
						e.printStackTrace();
						return true;
					}
                	
                	AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(title).setMessage(R.string.about).
                			setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							}).create();
                	dialog.show();
                	
                    return true;
                }
            });

            Preference changelog = (Preference)getPreferenceManager().findPreference("changeLog");
            changelog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Change Log").setMessage(R.string.changelog).
                			setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							}).create();
                	dialog.show();
                	
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

            Preference updateTagsForce = (Preference)getPreferenceManager().findPreference("updateTagsForce");
            updateTagsForce.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	activity.forceUpdateTags();
                    return true;
                }
            });

            final Preference donate = (Preference)getPreferenceManager().findPreference("donate");
            donate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0)
                {
                	donate.setSummary(Html.fromHtml("Buy me porn"));
                	activity.donate();
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

            Preference update = (Preference)getPreferenceManager().findPreference("update");
            update.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0)
                {
                	activity.update();
                    return true;
                }
            });

            Preference sync = (Preference)getPreferenceManager().findPreference("sync");
            sync.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0)
                {
                	final ProgressDialog dialog = ProgressDialog.show(activity,
                			"Synchronizing", "Please wait while sync is in progress",true,false);
                	
                	new Thread(new Runnable()
                	{
                		public void run()
                		{
                			activity.e621.sync();
                			
                			dialog.dismiss();
                		}
                	}).start();
                	
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
            				"ps | grep info.beastarman.e621"
            			};
            			
            			Process process = Runtime.getRuntime().exec(get_pid);
            			String pid = IOUtils.toString(process.getInputStream());
            			
            			pid = pid.substring(10,15);
            			
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
        
        @Override
        public void onStart()
        {
        	super.onStart();
        	
        	final Preference donate = (Preference)getPreferenceManager().findPreference("donate");
        	donate.setSummary("Buy me a beer");
        }
    }
}
