package info.beastarman.e621.frontend;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.DonationManager;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.backend.Pair;
import info.beastarman.e621.backend.PendingTask;
import info.beastarman.e621.middleware.E621DownloadedImages;
import info.beastarman.e621.middleware.Mascot;
import info.beastarman.e621.middleware.PendingTaskUpdateTagBase;
import info.beastarman.e621.middleware.PendingTaskUpdateVideosWebmMp4;
import info.beastarman.e621.views.NoHorizontalScrollView;

public class MainActivity extends SlideMenuBaseActivity
{
	Mascot[] mascots;
	
	int previous_mascot = -1;

	@Override
    protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);

        File nomedia = e621.noMediaFile();

		int newVersion = e621.isNewVersion();

		if(e621.isFirstRun())
		{
			AlertDialog.Builder confirmFullUpdateBuilder = new AlertDialog.Builder(this);
			confirmFullUpdateBuilder.setTitle("Welcome to E621 Mobile!");
			confirmFullUpdateBuilder.setMessage(getString(R.string.welcome));
			confirmFullUpdateBuilder.setPositiveButton("Ok", new OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialogInterface, int which)
				{
				}
			});
			
			confirmFullUpdateBuilder.create().show();
		}
		else if(newVersion > 0)
		{
			newVersionPopup(newVersion);
		}
        else if(e621.testNoMediaFile() && nomedia != null)
        {
            showNoMediaPopup(nomedia);
        }
        else
        {
            show_donate_popup();
        }

		updateNotifications();
	}
	
	protected void onStart()
	{
		super.onStart();
		
		mascots = e621.getMascots();
		
		change_mascot();

        updateStatistics();

		updateDonator();
	}

	private void updateNotifications()
	{
		ArrayList<PendingTask> pendingTasks = e621.getPendingTasks();
		LinearLayout notifications = (LinearLayout)findViewById(R.id.notification_area);

		for(PendingTask task : pendingTasks)
		{
			if(task instanceof PendingTaskUpdateVideosWebmMp4)
			{
				notifications.addView(getVideoUpdateView((PendingTaskUpdateVideosWebmMp4) task), 0);
			}
			else if(task instanceof PendingTaskUpdateTagBase)
			{
				notifications.addView(getUpdateTagBaseView((PendingTaskUpdateTagBase) task), 0);
			}
		}
	}

	private View getUpdateTagBaseView(final PendingTaskUpdateTagBase task)
	{
		final View v = getLayoutInflater().inflate(R.layout.notification_generic,null);

		final TextView tv = (TextView) v.findViewById(R.id.notification_generic_text);
		tv.setText("Pending action: Update tags");

		final View clickable = v.findViewById(R.id.notification_generic_scroll);

		task.addEventManager(new EventManager()
		{
			String last = "";

			private void updateText(final String text)
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						tv.setText(text);
					}
				});
			}

			@Override
			public void onTrigger(final Object obj)
			{
				if(obj instanceof PendingTaskUpdateVideosWebmMp4.States)
				{
					if(obj.equals(PendingTaskUpdateVideosWebmMp4.States.START))
					{
						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								clickable.setClickable(false);
								clickable.setEnabled(false);
								clickable.setFocusable(false);
								clickable.setFocusableInTouchMode(false);

								tv.setText("Starting...");
							}
						});
					}
					else
					{
						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								((ViewGroup) v.getParent()).removeView(v);
							}
						});
					}
				}
				else
				{
					if(obj == E621DownloadedImages.UpdateStates.CLEANING)
					{
						last = "Cleaning metadata";
						updateText(last);
					}
					else if(obj == E621DownloadedImages.UpdateStates.TAG_SYNC)
					{
						last = "Synchronizing tags";
						updateText(last);
					}
					else if(obj == E621DownloadedImages.UpdateStates.TAG_ALIAS_SYNC)
					{
						last = "Synchronizing tag aliases";
						updateText(last);
					}
					else if(obj == E621DownloadedImages.UpdateStates.IMAGE_TAG_SYNC)
					{
						last = "Synchronizing image tags";
						updateText(last);
					}
					else if(obj == E621DownloadedImages.UpdateStates.IMAGE_TAG_DB)
					{
						last = "Saving image tags into database";
						updateText(last);
					}
					else if(obj == E621DownloadedImages.UpdateStates.COMPLETED)
					{
						updateText("Finished");
					}
					else if(obj instanceof Pair)
					{
						Pair<String, String> pair = ((Pair<String, String>) obj);

						updateText(last + " (" + pair.left + "/" + pair.right + ")");
					}
				}
			}
		});

		if(clickable.isEnabled())
		{
			clickable.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					if(clickable.isEnabled()) task.start();
				}
			});
		}

		return v;
	}

	private View getVideoUpdateView(final PendingTaskUpdateVideosWebmMp4 task)
	{
		final View v = getLayoutInflater().inflate(R.layout.notification_generic,null);

		final TextView tv = (TextView) v.findViewById(R.id.notification_generic_text);
		tv.setText("Pending action: Downloaded videos update");

		final View clickable = v.findViewById(R.id.notification_generic_scroll);

		task.addEventManager(new EventManager()
		{
			@Override
			public void onTrigger(final Object obj)
			{
				if(obj instanceof PendingTaskUpdateVideosWebmMp4.States)
				{
					if(obj.equals(PendingTaskUpdateVideosWebmMp4.States.START))
					{
						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								clickable.setClickable(false);
								clickable.setEnabled(false);
								clickable.setFocusable(false);
								clickable.setFocusableInTouchMode(false);

								tv.setText("Starting...");
							}
						});
					}
					else
					{
						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								((ViewGroup) v.getParent()).removeView(v);
							}
						});
					}
				}

				if(obj instanceof Pair<?, ?>)
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							Pair<Integer, Integer> progress = (Pair<Integer, Integer>) obj;

							tv.setText(String.format("Downloading video %1$d of %2$d", progress.left + 1, progress.right));
						}
					});
				}
			}
		});

		if(clickable.isEnabled())
		{
			clickable.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					if(clickable.isEnabled()) task.start();
				}
			});
		}

		return v;
	}

	private void updateDonator()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				final DonationManager.Donator donator = e621.getDonationManager().getHighlight();
				final RelativeLayout wrapper = (RelativeLayout) findViewById(R.id.donation_highlight_wrapper);
				final NoHorizontalScrollView s = (NoHorizontalScrollView) findViewById(R.id.donation_highlight_scroll);
				final TextView text = (TextView) findViewById(R.id.donation_highlight_text);

				final DecimalFormat df = new DecimalFormat();
				df.setMinimumFractionDigits(2);
				df.setMaximumFractionDigits(2);

				if(donator != null)
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							SpannableString ss = new SpannableString(donator.name);

							if(donator.url != null)
							{
								ss.setSpan(new URLSpan(donator.url.toString()),0,ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
								text.setMovementMethod(LinkMovementMethod.getInstance());
							}

							wrapper.setVisibility(View.VISIBLE);
							text.setText("A big thanks to ");
							text.append(ss);
							text.append(" for donating a total of $" + df.format(donator.ammount));

							final Animation a = new Animation()
							{
								@Override
								protected void applyTransformation(float interpolatedTime, Transformation t)
								{
									int scrollLength = s.getChildAt(0).getWidth() - s.getWidth();

									s.scrollTo((int)(scrollLength*interpolatedTime),0);
								}
							};

							s.post(new Runnable()
							{
								@Override
								public void run()
								{
									int scrollLength = s.getChildAt(0).getWidth() - s.getWidth();

									if(scrollLength > 0)
									{
										a.setDuration(scrollLength * 10);
										s.startAnimation(a);
										a.setRepeatMode(Animation.REVERSE);
										a.setRepeatCount(Animation.INFINITE);
										a.setStartOffset(1000);
										((View) s.getParent()).invalidate();
									}
								}
							});
						}
					});
				}
			}
		}).start();
	}

	private void newVersionPopup(int version)
	{
		String message = null;

		switch (version)
		{
			case 17:
			case 18:
				message = getString(R.string.version_17);
				break;
			case 19:
				message = getString(R.string.version_19);
				break;
			case 21:
				message = getString(R.string.version_21);
				break;
			case 22:
				message = getString(R.string.version_22);
				break;
			case 24:
				message = getString(R.string.version_24);
				break;
			case 25:
				message = getString(R.string.version_25);
				break;
			default:
				break;
		}

		if(message == null) return;

		AlertDialog.Builder confirmFullUpdateBuilder = new AlertDialog.Builder(this);
		confirmFullUpdateBuilder.setTitle("News");
		confirmFullUpdateBuilder.setMessage(message);
		confirmFullUpdateBuilder.setPositiveButton("Dismiss", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int which) {
			}
		});

		confirmFullUpdateBuilder.create().show();
	}

	private String getSize(double size)
	{
		if(size < 1024)
		{
			return new DecimalFormat("#.##").format(size) + " B";
		}

		size /= 1024;

		if(size < 1024)
		{
			return new DecimalFormat("#.##").format(size) + " KB";
		}

		size /= 1024;

		if(size < 1024)
		{
			return new DecimalFormat("#.##").format(size) + " MB";
		}

		size /= 1024;

		return new DecimalFormat("#.##").format(size) + " GB";
	}

	String online = "- Posts Online";
	String offline = "- Posts Offilne";
    public void updateStatistics()
    {
		final TextView statistics = (TextView) findViewById(R.id.statisticsText);

		if(!e621.showStatisticsInHome())
		{
			statistics.setVisibility(View.GONE);

			return;
		}
		else
		{
			statistics.setVisibility(View.VISIBLE);
		}

		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setGroupingSeparator(' ');

		final DecimalFormat df = new DecimalFormat();
		df.setDecimalFormatSymbols(symbols);
		df.setGroupingSize(3);
		df.setMaximumFractionDigits(2);

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					online = df.format(e621.getOnlinePosts()) + "+ Posts Online";
				} catch(IOException e)
				{
					e.printStackTrace();
				}

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						statistics.setText(online + "\n" + offline);
					}
				});
			}
		}).start();

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				offline = df.format(e621.localSearchCount("")) + " Posts Offline (" + getSize(e621.getOfflinePostsSize()) + ")";

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						statistics.setText(online + "\n" + offline);
					}
				});
			}
		}).start();
    }

    public void showNoMediaPopup(File nomedia)
    {
        AlertDialog.Builder confirmFullUpdateBuilder = new AlertDialog.Builder(this);
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
    }

    public void show_donate_popup()
    {
        if(e621.showDonatePopup())
        {
            AlertDialog.Builder confirmFullUpdateBuilder = new AlertDialog.Builder(this);
            confirmFullUpdateBuilder.setTitle("Spare some change please");
            confirmFullUpdateBuilder.setMessage(getString(R.string.donate_plz));
            confirmFullUpdateBuilder.setPositiveButton("There you go!", new OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int which)
                {
                    Intent intent = new Intent(MainActivity.this, DonateActivity.class);
                    startActivity(intent);
                }
            });
            confirmFullUpdateBuilder.setNegativeButton("Nope. Sorry.", new OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int which)
                {
                }
            });

            confirmFullUpdateBuilder.create().show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                open_settings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public void visit_mascot_website(View v)
    {
    	visit_mascot_website();
    }
    
    public void visit_mascot_website()
    {
    	Mascot m;
    	
    	if(previous_mascot >=0 && previous_mascot < mascots.length)
    	{
    		m = mascots[previous_mascot];
    	}
    	else
    	{
    		return;
    	}
    	
    	Intent i = new Intent(Intent.ACTION_VIEW);
    	i.setData(Uri.parse(m.artistUrl));
    	startActivity(i);
    }
    
    public void change_mascot(View v)
    {
    	change_mascot();
    }
    
    public void change_mascot()
    {
    	ImageView mascot = (ImageView)findViewById(R.id.mascot);
    	ImageView mascot_blur = (ImageView)findViewById(R.id.mascot_blur);
    	TextView mascot_by = (TextView)findViewById(R.id.mascotBy);
    	
    	if(mascots.length == 0)
    	{
			mascot.setVisibility(View.VISIBLE);
			mascot_blur.setVisibility(View.VISIBLE);
    		mascot_by.setVisibility(View.INVISIBLE);

			mascot.setImageResource(R.drawable.e621_generic_pattern);
			mascot_blur.setImageResource(R.drawable.e621_generic_pattern_blur);
    		
    		return;
    	}
    	else
    	{
    		mascot.setVisibility(View.VISIBLE);
    		mascot_blur.setVisibility(View.VISIBLE);
    		mascot_by.setVisibility(View.VISIBLE);
    	}
    	
    	Mascot m;
    	
    	if(mascots.length == 1)
    	{
    		m = mascots[0];
    	}
    	else
    	{
	    	int random_mascot = (int) (Math.random()*(mascots.length-1));
	    	if(random_mascot >= previous_mascot)
	    	{
	    		random_mascot++;
	    	}
	    	
	    	previous_mascot = random_mascot;
    	
	    	m = mascots[random_mascot%mascots.length];
    	}
    	
    	mascot.setImageResource(m.image);
    	mascot_blur.setImageResource(m.blur);
    	mascot_by.setText("Mascot by " + m.artistName);
    }
    
    public void open_settings()
    {
    	Intent intent;
    	intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
    }
    
    public void search(View view)
    {
		EditText editText = (EditText) findViewById(R.id.searchInput);
    	String search = editText.getText().toString().trim();
    	
		Intent intent = new Intent(this, SearchActivity.class);
		intent.putExtra(SearchActivity.SEARCH,search);
		startActivity(intent);
    }
    
    public void localSearch(View view)
    {
    	EditText editText = (EditText) findViewById(R.id.searchInput);
    	String search = editText.getText().toString().trim();
    	
		Intent intent = new Intent(this, DownloadsActivity.class);
		intent.putExtra(DownloadsActivity.SEARCH,search);
		startActivity(intent);
    }
}
