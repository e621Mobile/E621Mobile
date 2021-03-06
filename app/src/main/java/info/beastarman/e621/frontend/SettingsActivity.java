package info.beastarman.e621.frontend;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.EditText;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.backend.GTFO;
import info.beastarman.e621.backend.Pair;
import info.beastarman.e621.middleware.AndroidAppUpdaterInterface;
import info.beastarman.e621.middleware.AndroidAppVersion;
import info.beastarman.e621.middleware.E621DownloadedImages;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.views.BlackListDialog;
import info.beastarman.e621.views.HighlightDialog;
import info.beastarman.e621.views.SeekBarDialogPreference;
import info.beastarman.e621.views.StepsProgressDialog;

public class SettingsActivity extends PreferenceActivity
{
	E621Middleware e621;
	
	@Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        e621 = E621Middleware.getInstance(getApplicationContext());
        
        MyPreferenceFragment fragment = new MyPreferenceFragment();
        
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
	{
		E621Middleware e621;
		Activity activity;

		@Override
		public void onAttach(Activity act)
		{
			super.onAttach(act);

			this.activity = act;

			this.e621 = E621Middleware.getInstance(act);
			;
		}

		@Override
		public void onCreate(final Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.settings);

			getPreferenceManager().setSharedPreferencesName(E621Middleware.PREFS_NAME);

			CheckBoxPreference hideDownload = (CheckBoxPreference) findPreference("hideDownloadFolder");
			hideDownload.setChecked(getPreferenceManager().getSharedPreferences().getBoolean("hideDownloadFolder", true));

            if(e621.noMediaFile() != null)
            {
                hideDownload.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
                {
                    @Override
                    public boolean onPreferenceClick(Preference preference)
                    {
                        File nomedia = e621.noMediaFile();

                        if(nomedia == null) return false;

                        AlertDialog.Builder confirmFullUpdateBuilder = new AlertDialog.Builder(activity);
                        confirmFullUpdateBuilder.setTitle(".nomedia file detected");
                        confirmFullUpdateBuilder.setMessage(String.format(getString(R.string.nomedia),nomedia.getAbsolutePath()));
                        confirmFullUpdateBuilder.setPositiveButton("Yes", new OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which)
                            {
                                e621.removeNoMediaFile();
                            }
                        });
                        confirmFullUpdateBuilder.setNegativeButton("No", new OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which)
                            {
                                e621.stopTestingMediaFile();
                            }
                        });

                        confirmFullUpdateBuilder.create().show();

                        return false;
                    }
                });
            }

			CheckBoxPreference antecipateOnlyOnWiFi = (CheckBoxPreference) findPreference("antecipateOnlyOnWiFi");
			antecipateOnlyOnWiFi.setChecked(e621.antecipateOnlyOnWiFi());

			CheckBoxPreference showStatisticsInHome = (CheckBoxPreference) findPreference("showStatisticsInHome");
			showStatisticsInHome.setChecked(e621.showStatisticsInHome());

			CheckBoxPreference syncOnlyOnWiFi = (CheckBoxPreference) findPreference("syncOnlyOnWiFi");
			syncOnlyOnWiFi.setChecked(e621.syncOnlyOnWiFi());

			CheckBoxPreference playGifs = (CheckBoxPreference) findPreference("playGifs");
			playGifs.setChecked(getPreferenceManager().getSharedPreferences().getBoolean("playGifs", true));

			CheckBoxPreference downloadInSearch = (CheckBoxPreference) findPreference("downloadInSearch");
			downloadInSearch.setChecked(getPreferenceManager().getSharedPreferences().getBoolean("downloadInSearch", true));

			ListPreference downloadSize = (ListPreference) findPreference("prefferedFileDownloadSize");
			downloadSize.setValue(String.valueOf(getPreferenceManager().getSharedPreferences().getInt("prefferedFileDownloadSize", 2)));

			ListPreference thumbnailSize = (ListPreference) findPreference("prefferedFilePreviewSize");
			thumbnailSize.setValue(String.valueOf(getPreferenceManager().getSharedPreferences().getInt("prefferedFilePreviewSize", 1)));

			ListPreference blacklistMethod = (ListPreference) findPreference("blacklistMethod");
			blacklistMethod.setValue(String.valueOf(e621.blacklistMethod().asInt()));

			ListPreference commentsSorting = (ListPreference) findPreference("commentsSorting");
			commentsSorting.setValue(String.valueOf(e621.commentsSorting()));

			SeekBarDialogPreference thumbnailCacheSize = (SeekBarDialogPreference) findPreference("thumbnailCacheSize");
			thumbnailCacheSize.setProgress(getPreferenceManager().getSharedPreferences().getInt("thumbnailCacheSize", 5));

			SeekBarDialogPreference resultsPerPage = (SeekBarDialogPreference) findPreference("resultsPerPage");
			resultsPerPage.setProgress(getPreferenceManager().getSharedPreferences().getInt("resultsPerPage", 2));

			SeekBarDialogPreference fullCacheSize = (SeekBarDialogPreference) findPreference("fullCacheSize");
			fullCacheSize.setProgress(getPreferenceManager().getSharedPreferences().getInt("fullCacheSize", 10));

			MultiSelectListPreference ratings = (MultiSelectListPreference) findPreference("allowedRatings");
			ratings.setValues(getPreferenceManager().getSharedPreferences().getStringSet("allowedRatings", new HashSet<String>()));

			Preference blacklist = (Preference) getPreferenceManager().findPreference("blacklist");
			blacklist.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference arg0)
				{
					BlackListDialog dialog = new BlackListDialog(activity, e621.blacklist());
					dialog.show();

					return true;
				}
			});

			Preference highlight = (Preference) getPreferenceManager().findPreference("highlight");
			highlight.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference arg0)
				{
					HighlightDialog dialog = new HighlightDialog(activity, e621.highlight());
					dialog.show();

					return true;
				}
			});

			Preference clearCache = (Preference) getPreferenceManager().findPreference("clearCache");
			clearCache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference arg0)
				{
					clearCache();
					return true;
				}
			});

			Preference about = (Preference) getPreferenceManager().findPreference("about");
			about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference arg0)
				{
					String title;
					try
					{
						title = "About E621Mobile " + activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
					} catch (NameNotFoundException e)
					{
						e.printStackTrace();
						return true;
					}

					AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(title).setMessage(R.string.about).
							setPositiveButton("Dismiss", new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
								}
							}).create();
					dialog.show();

					return true;
				}
			});

			Preference changelog = (Preference) getPreferenceManager().findPreference("changeLog");
			changelog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference arg0)
				{
					AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Change Log").setMessage(R.string.changelog).
							setPositiveButton("Dismiss", new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
								}
							}).create();
					dialog.show();

					return true;
				}
			});

			ArrayList<Date> backups = e621.getBackups();
			CharSequence[] entries = new CharSequence[backups.size()];
			CharSequence[] entriesValues = new CharSequence[backups.size()];

			for (int i = 0; i < backups.size(); i++)
			{
				entries[i] = backups.get(i).toString();
				entriesValues[i] = String.valueOf(backups.get(i).getTime());
			}

			ListPreference restoreBackup = (ListPreference) getPreferenceManager().findPreference("restoreBackup");
			restoreBackup.setDefaultValue(null);
			restoreBackup.setEntries(entries);
			restoreBackup.setEntryValues(entriesValues);
			restoreBackup.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
					restoreBackup(new Date(Long.parseLong(newValue.toString())));

					return false;
				}
			});

			Preference allowedMascots = (Preference) getPreferenceManager().findPreference("allowedMascots");
			allowedMascots.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					E621MascotSelect fragment = new E621MascotSelect();
					fragment.show(getFragmentManager(), "MascotSelect");

					return true;
				}
			});

			Preference updateTagsForce = (Preference) getPreferenceManager().findPreference("updateTagsForce");
			updateTagsForce.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference arg0)
				{
					forceUpdateTags();
					return true;
				}
			});

			Preference aboutE621 = (Preference) getPreferenceManager().findPreference("aboutE621");
			aboutE621.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference arg0)
				{
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse("https://e621.net/wiki/show?title=e621%3Aabout"));
					startActivity(i);
					return true;
				}
			});

			Preference update = (Preference) getPreferenceManager().findPreference("update");
			update.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference arg0)
				{
					update();
					return true;
				}
			});

			final SeekBarDialogPreference syncFrequency = (SeekBarDialogPreference) findPreference("syncFrequency");
			syncFrequency.setProgress(e621.getSyncFrequency());
			syncFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object o)
				{
					e621.setSyncFrequency((Integer) o);

					return true;
				}
			});
			syncFrequency.setTextUpdate(new SeekBarDialogPreference.TextUpdate()
			{
				@Override
				public String onTextUpdate(SeekBarDialogPreference preference, int progress)
				{
					if(progress == 0)
					{
						return "Disabled";
					}
					else if(progress == 1)
					{
						return "1 Hour";
					}
					else
					{
						return progress + " Hours";
					}
				}
			});

			Preference sync = (Preference) getPreferenceManager().findPreference("sync");
			sync.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference arg0)
				{
					final StepsProgressDialog dialog = new StepsProgressDialog(activity);
					dialog.show();

					new Thread(new Runnable()
					{
						public void run()
						{
							e621.sync(new EventManager()
							{
								String lastMsg = "Sync in progress";
								String extra = "";

								@Override
								public void onTrigger(Object obj)
								{
									if(obj instanceof E621Middleware.SyncState)
									{
										if(obj == E621Middleware.SyncState.REPORTS)
										{
											lastMsg = "Sending remaining reports";
										}
										else if(obj == E621Middleware.SyncState.FAILED_DOWNLOADS)
										{
											lastMsg = "Fixing failed downloads";
										}
										else if(obj == E621Middleware.SyncState.CHECKING_FOR_UPDATES)
										{
											lastMsg = "Checking for updates";
										}
										else if(obj == E621Middleware.SyncState.BACKUP)
										{
											lastMsg = "Creating new backup";
										}
										else if(obj == E621Middleware.SyncState.INTERRUPTED_SEARCHES)
										{
											lastMsg = "Updating interrupted searches";
										}
										else if(obj == E621Middleware.SyncState.FINISHED)
										{
											activity.runOnUiThread(new Runnable()
											{
												@Override
												public void run()
												{
													dialog.setDone("Finished");
												}
											});

											return;
										}
									}
									else if(obj instanceof E621DownloadedImages.UpdateStates)
									{
										if(obj == E621DownloadedImages.UpdateStates.CLEANING)
										{
											lastMsg = "Cleaning metadata";
										}
										else if(obj == E621DownloadedImages.UpdateStates.TAG_SYNC)
										{
											lastMsg = "Synchronizing tags";
										}
										else if(obj == E621DownloadedImages.UpdateStates.TAG_ALIAS_SYNC)
										{
											lastMsg = "Synchronizing tag aliases";
										}
										else if(obj == E621DownloadedImages.UpdateStates.IMAGE_TAG_SYNC)
										{
											lastMsg = "Synchronizing image tags";
										}
										else if(obj == E621DownloadedImages.UpdateStates.IMAGE_TAG_DB)
										{
											lastMsg = "Saving image tags into database";
										}
									}

									if(obj instanceof Pair)
									{
										Pair<String, String> pair = ((Pair<String, String>) obj);

										extra = " (" + pair.left + "/" + pair.right + ")";

										dialog.updateStep(lastMsg + extra);
									}
									else
									{
										dialog.addStep(lastMsg);
									}

									activity.runOnUiThread(new Runnable()
									{
										@Override
										public void run()
										{
											dialog.showStepsMessage();
										}
									});

									extra = "";
								}
							});
						}
					}).start();

					return true;
				}
			});

			Preference fixMe = (Preference) getPreferenceManager().findPreference("fixMe");
			fixMe.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference arg0)
				{
					fixInconsistencies();

					return true;
				}
			});

			Preference reportsAndReplies = (Preference) getPreferenceManager().findPreference("reportsAndReplies");
			reportsAndReplies.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference arg0)
				{
					Intent intent = new Intent(activity.getApplicationContext(), ErrorReportListActivity.class);
					startActivity(intent);

					return true;
				}
			});

			Preference sendErrorReport = (Preference) getPreferenceManager().findPreference("sendErrorReport");
			sendErrorReport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference arg0)
				{
					Intent intent = new Intent(activity.getApplicationContext(), FeedbackActivity.class);
					startActivity(intent);

					return true;
				}
			});

			CheckBoxPreference lazyLoad = (CheckBoxPreference) findPreference("lazyLoad");
			lazyLoad.setChecked(getPreferenceManager().getSharedPreferences().getBoolean("lazyLoad", true));

			CheckBoxPreference betaReleases = (CheckBoxPreference) findPreference("betaReleases");
			betaReleases.setChecked(e621.betaReleases());
		}

		protected void fixInconsistencies()
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle("Posts to fix");

			final EditText input = new EditText(activity);
			input.setHint("Search Query");
			builder.setView(input);

			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					fixInconsistencies(input.getText().toString());
				}
			});
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});

			builder.show();
		}

		protected void fixInconsistencies(final String query)
		{
			final StepsProgressDialog dialog = new StepsProgressDialog(activity);
			dialog.show();

			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					e621.fixMe(new EventManager()
					{
						String lastMsg = "Fixing";
						String extra = "";

						@Override
						public void onTrigger(Object obj)
						{
							if(obj instanceof E621Middleware.FixState)
							{
								if(obj == E621Middleware.FixState.TAGS)
								{
									lastMsg = "Looking for posts with few tags";
								}
								else if(obj == E621Middleware.FixState.CORRUPT)
								{
									lastMsg = "Looking for corrupted posts";
								}
								else if(obj == E621Middleware.FixState.FIXING)
								{
									lastMsg = "Redownloading corrupted posts";
								}
							}
							else if(obj instanceof E621DownloadedImages.UpdateStates)
							{
								if(obj == E621DownloadedImages.UpdateStates.IMAGE_TAG_DB)
								{
									lastMsg = "Fixing posts with few tags";
								}
							}

							if (obj instanceof Pair)
							{
								Pair<String, String> pair = ((Pair<String, String>) obj);

								extra = " (" + pair.left + "/" + pair.right + ")";

								dialog.updateStep(lastMsg + extra);
							}
							else
							{
								dialog.addStep(lastMsg);
							}

							activity.runOnUiThread(new Runnable()
							{
								@Override
								public void run()
								{
									dialog.showStepsMessage();
								}
							});

							extra = "";
						}
					},query);

					activity.runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							dialog.setDone("Done");
						}
					});
				}
			}).start();
		}

		private EventManager getTagUpdateEventManager(final StepsProgressDialog dialog)
		{
			return new EventManager()
			{
				String last = "";

				@Override
				public void onTrigger(Object obj)
				{
					if(obj == E621DownloadedImages.UpdateStates.CLEANING)
					{
						last = "Cleaning metadata";
						dialog.addStep(last);
					}
					else if(obj == E621DownloadedImages.UpdateStates.TAG_SYNC)
					{
						last = "Synchronizing tags";
						dialog.addStep(last);
					}
					else if(obj == E621DownloadedImages.UpdateStates.TAG_ALIAS_SYNC)
					{
						last = "Synchronizing tag aliases";
						dialog.addStep(last);
					}
					else if(obj == E621DownloadedImages.UpdateStates.IMAGE_TAG_SYNC)
					{
						last = "Synchronizing image tags";
						dialog.addStep(last);
					}
					else if(obj == E621DownloadedImages.UpdateStates.IMAGE_TAG_DB)
					{
						last = "Saving image tags into database";
						dialog.addStep(last);
					}
					else if(obj == E621DownloadedImages.UpdateStates.COMPLETED)
					{
						activity.runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								dialog.setDone("Finished");
							}
						});
					}
					else if(obj instanceof Pair)
					{
						Pair<String,String> pair = ((Pair<String,String>) obj);

						dialog.updateStep(last + " (" + pair.left + "/" + pair.right + ")");
					}

					activity.runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							dialog.showStepsMessage();
						}
					});
				}
			};
		}

    	protected void forceUpdateTags()
    	{
    		AlertDialog.Builder confirmFullUpdateBuilder = new AlertDialog.Builder(activity);
    		confirmFullUpdateBuilder.setMessage("Are you sure? This will take an while.");
    		confirmFullUpdateBuilder.setPositiveButton("Continue", new OnClickListener()
    		{
    			@Override
    			public void onClick(DialogInterface dialogInterface, int which)
    			{
    				final GTFO<StepsProgressDialog> dialog = new GTFO<StepsProgressDialog>();
					dialog.obj = new StepsProgressDialog(activity);
					dialog.obj.show();
    				
    				new Thread(new Runnable()
    				{
    					@Override
    					public void run()
    					{
    						e621.force_update_tags(getTagUpdateEventManager(dialog.obj));
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
    		final ProgressDialog dialog = ProgressDialog.show(activity, "","Clearing cache. Please wait...", true);
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
    		AlertDialog.Builder removeNewBuilder = new AlertDialog.Builder(activity);
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
    		dialogWrapper.obj = new StepsProgressDialog(activity);
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
    					public void onTrigger(final Object obj)
    		    		{
    		    			if(obj == E621Middleware.BackupStates.OPENING)
    		    			{
    		    				activity.runOnUiThread(new Runnable()
    		    				{
    		    					public void run()
    		    					{
    		    						dialogWrapper.obj.addStep("Opening current backup").showStepsMessage();
    		    					}
    		    				});
    		    			}
    		    			else if(obj == E621Middleware.BackupStates.CURRENT)
    		    			{
    		    				activity.runOnUiThread(new Runnable()
    		    				{
    		    					public void run()
    		    					{
    		    						dialogWrapper.obj.addStep("Reading current backup").showStepsMessage();
    		    					}
    		    				});
    		    			}
    		    			else if(obj == E621Middleware.BackupStates.CURRENT)
    		    			{
    		    				activity.runOnUiThread(new Runnable()
    		    				{
    		    					public void run()
    		    					{
    		    						dialogWrapper.obj.addStep("Creating emergency backup").showStepsMessage();
    		    					}
    		    				});
    		    			}
    		    			else if(obj == E621Middleware.BackupStates.SEARCHES)
    		    			{
    		    				activity.runOnUiThread(new Runnable()
    		    				{
    		    					public void run()
    		    					{
    		    						dialogWrapper.obj.addStep("Overriding saved searches").showStepsMessage();
    		    					}
    		    				});
    		    			}
    		    			else if(obj == E621Middleware.BackupStates.SEARCHES_COUNT)
    		    			{
    		    				activity.runOnUiThread(new Runnable()
    		    				{
    		    					public void run()
    		    					{
    		    						dialogWrapper.obj.addStep("Updating saved searches remaining images").showStepsMessage();
    		    					}
    		    				});
    		    			}
    		    			else if(obj == E621Middleware.BackupStates.REMOVE_EMERGENCY)
    		    			{
    		    				activity.runOnUiThread(new Runnable()
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
    		    				activity.runOnUiThread(new Runnable()
    		    				{
    		    					public void run()
    		    					{
    		    						dialogWrapper.obj.addStep("Getting current images").showStepsMessage();
    		    					}
    		    				});
    		    			}
    		    			else if(obj == E621Middleware.BackupStates.DELETING_IMAGES)
    		    			{
    		    				activity.runOnUiThread(new Runnable()
    		    				{
    		    					public void run()
    		    					{
    		    						dialogWrapper.obj.addStep("Removing unnecessary images").showStepsMessage();
    		    					}
    		    				});
    		    			}
    		    			else if(obj == E621Middleware.BackupStates.INSERTING_IMAGES)
    		    			{
    		    				activity.runOnUiThread(new Runnable()
    		    				{
    		    					public void run()
    		    					{
    		    						dialogWrapper.obj.addStep("Inserting images").showStepsMessage();
    		    					}
    		    				});
    		    			}
    		    			else if(obj == E621Middleware.BackupStates.DOWNLOADING_IMAGES)
    		    			{
    		    				activity.runOnUiThread(new Runnable()
    		    				{
    		    					public void run()
    		    					{
    		    						dialogWrapper.obj.addStep("Downloading images").showStepsMessage();
    		    					}
    		    				});
    		    			}
    		    			else if(obj == E621Middleware.BackupStates.UPDATE_TAGS)
    		    			{
    		    				activity.runOnUiThread(new Runnable()
    		    				{
    		    					public void run()
    		    					{
    		    						dialogWrapper.obj.addStep("Updating tags").showStepsMessage();
    		    					}
    		    				});
    		    			}
    		    			else if(obj == E621Middleware.BackupStates.SUCCESS)
    		    			{
    		    				activity.runOnUiThread(new Runnable()
    		    				{
    		    					public void run()
    		    					{
    		    						dialogWrapper.obj.setDone("Backup finished!");
    		    					}
    		    				});
    		    			}
							else if(obj == E621Middleware.BackupStates.FAILURE)
							{
								activity.runOnUiThread(new Runnable()
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
    		final AndroidAppUpdaterInterface appUpdater = e621.getAndroidAppUpdater();

			final AlertDialog.Builder confirmDialogBuilder = new AlertDialog.Builder(activity).setTitle("Please wait...").setCancelable(true).
				setMessage("Retrieving update info.");
			final AlertDialog confirmDialog = confirmDialogBuilder.create();
			confirmDialog.show();

    		new Thread(new Runnable()
    		{
    			public void run()
    			{
    				PackageInfo pInfo = null;
    				
    				try
    				{
    					try {
    						pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
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
    						final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity).setTitle("New Version Found").setCancelable(true).
    								setMessage(String.format(getResources().getString(R.string.new_version_found),version.versionName));
    						
    						activity.runOnUiThread(new Runnable()
    						{
    							public void run()
    							{
									confirmDialog.dismiss();
									final AlertDialog dialog = dialogBuilder.create();
    								
    								dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Update", new OnClickListener()
    								{
    									@Override
    									public void onClick(DialogInterface arg0,int arg1)
    									{
    										dialog.dismiss();
    										
    										final GTFO<StepsProgressDialog> dialogWrapper = new GTFO<StepsProgressDialog>();
    										dialogWrapper.obj = new StepsProgressDialog(activity);
    										dialogWrapper.obj.show();
    										
    										e621.updateApp(version, new EventManager()
    										{
    											@Override
    											public void onTrigger(Object obj)
    											{
    												if(obj == E621Middleware.UpdateState.START)
    								    			{
    													activity.runOnUiThread(new Runnable()
    								    				{
    								    					public void run()
    								    					{
    								    						dialogWrapper.obj.addStep("Retrieving package file").showStepsMessage();
    								    					}
    								    				});
    								    			}
    								    			else if(obj == E621Middleware.UpdateState.DOWNLOADED)
    								    			{
    								    				activity.runOnUiThread(new Runnable()
    								    				{
    								    					public void run()
    								    					{
    								    						dialogWrapper.obj.addStep("Package downloaded").showStepsMessage();
    								    					}
    								    				});
    								    			}
    								    			else if(obj == E621Middleware.UpdateState.SUCCESS)
    								    			{
    								    				activity.runOnUiThread(new Runnable()
    								    				{
    								    					public void run()
    								    					{
    								    						dialogWrapper.obj.setDone("Starting package install");
    								    					}
    								    				});
    								    			}
    								    			else if(obj == E621Middleware.UpdateState.FAILURE)
    								    			{
    								    				activity.runOnUiThread(new Runnable()
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
    					final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity).setTitle("Update").
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
    					
    					activity.runOnUiThread(new Runnable()
    					{
    						public void run()
    						{
								confirmDialog.dismiss();
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
    }
}
