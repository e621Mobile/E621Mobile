package info.beastarman.e621.frontend;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.backend.GTFO;
import info.beastarman.e621.middleware.E621DownloadedImage;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.ImageViewHandler;
import info.beastarman.e621.middleware.OfflineImageNavigator;
import info.beastarman.e621.views.LazyRunScrollView;

public class DownloadsActivity extends BaseActivity
{
	public static String SEARCH = "search";
	public static String PAGE = "page";
	public static String LIMIT = "limit";

	public String search = "";
	public int page = 0;
	public int limit;
	
	public int total_pages;

	private ArrayList<E621DownloadedImage> downloads = null;
	private ArrayList<ImageView> imageViews = new ArrayList<ImageView>();
	
	private boolean exported = false;
	
	EventManager event = new EventManager()
	{
		@Override
		public void onTrigger(Object obj)
		{
			exported = obj == E621Middleware.ExportState.CREATED;
			
			invalidateOptionsMenu();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		search = getIntent().getStringExtra(SearchActivity.SEARCH);
		if(search == null)
		{
			search = "";
		}
		else
		{
			search = search.trim();
		}
		page = getIntent().getIntExtra(SearchActivity.PAGE, 0);
		limit = getIntent().getIntExtra(SearchActivity.LIMIT, e621.resultsPerPage());

		((EditText) findViewById(R.id.searchInput)).setText(search);

		warnNoTags();
	}

	public void warnNoTags()
	{
		if(!e621.hasMetadata() && search.length() > 0)
		{
			AlertDialog.Builder confirmFullUpdateBuilder = new AlertDialog.Builder(this);
			confirmFullUpdateBuilder.setTitle("No tags found!");
			confirmFullUpdateBuilder.setMessage(getString(R.string.no_metadata));
			confirmFullUpdateBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener()
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
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		if(exported)
		{
			getMenuInflater().inflate(R.menu.downloads_exported, menu);
		}
		else
		{
			getMenuInflater().inflate(R.menu.downloads, menu);
		}
		return true;
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		
		total_pages = e621.pages(limit, search);

		Resources res = getResources();
		String text = String.format(res.getString(R.string.page_counter),
										   String.valueOf(page + 1), String.valueOf(total_pages));

		TextView page_counter = (TextView) findViewById(R.id.page_counter);
		page_counter.setText(text);
		
		downloads = e621.localSearch(page, limit, search);

		update_results();
		
		e621.bindExportSearchState(search, event);
	}
	
	@Override
	public void onStop()
	{
		super.onStop();

		for(ImageView img : imageViews)
		{
			Drawable drawable = img.getDrawable();
			if(drawable instanceof BitmapDrawable)
			{
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				Bitmap bitmap = bitmapDrawable.getBitmap();
				if(bitmap != null)
				{
					bitmap.recycle();
				}
			}
		}

		imageViews.clear();

		ViewGroup layout = (ViewGroup) findViewById(R.id.content_wrapper);
		layout.removeAllViews();
		
		e621.unbindExportSearchState(search, event);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch(item.getItemId())
		{
			case R.id.action_settings:
				open_settings();
				return true;
			case R.id.action_export:
			case R.id.action_update_export:
				export();
				return true;
			case R.id.action_remove_export:
				remove();
				return true;
			case R.id.action_online_search:
				online_search();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	public void online_search()
	{
		Intent intent = new Intent(this, SearchActivity.class);
		intent.putExtra(SearchActivity.SEARCH, search);
		startActivity(intent);
	}
	
	public void export()
	{
		final ProgressDialog dialog = ProgressDialog.show(DownloadsActivity.this, "", "Exporting images. Please wait...", true);
		dialog.setIndeterminate(true);
		dialog.show();
		
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				final File dir = e621.export(search);
				
				final GTFO<MediaScannerConnection> connection = new GTFO<MediaScannerConnection>();
				connection.obj = new MediaScannerConnection(DownloadsActivity.this, new MediaScannerConnection.MediaScannerConnectionClient()
				{
					@Override
					public void onMediaScannerConnected()
					{
						connection.obj.scanFile(dir.getAbsolutePath(), "image/*");
					}

					@Override
					public void onScanCompleted(String path, Uri uri)
					{
						Intent intent = new Intent(Intent.ACTION_VIEW);

						if(uri != null)
						{
							intent.setData(uri);
						}

						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

						try
						{
							startActivity(intent);
						}
						catch(Throwable t)
						{
							intent.setData(null);
							intent.setType("image/*");
							startActivity(intent);
						}
						finally
						{
							connection.obj.disconnect();
						}
					}
				});
				connection.obj.connect();
				
				dialog.dismiss();
			}
		}).start();
	}
	
	public void remove()
	{
		final ProgressDialog dialog = ProgressDialog.show(DownloadsActivity.this, "", "Removing images. Please wait...", true);
		dialog.setIndeterminate(true);
		dialog.show();
		
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				e621.removeExported(search);
				dialog.dismiss();
			}
		}).start();
	}

	public void open_settings()
	{
		Intent intent;
		intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}
	
	public void update_results()
	{
		final LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);
		layout.removeAllViews();
		
		layout.post(new Runnable()
		{
			@Override
			public void run()
			{
				if(downloads.size() == 0)
				{
					TextView t = new TextView(getApplicationContext());
					t.setText(R.string.no_results);
					t.setGravity(Gravity.CENTER_HORIZONTAL);
					t.setPadding(0, 24, 0, 0);

					layout.addView(t);

					return;
				}
				
				int layout_width = layout.getWidth();
				
				LazyRunScrollView scroll = (LazyRunScrollView) findViewById(R.id.resultsScrollView);
				
				int image_y = 0;
				int position = page * e621.resultsPerPage();

				for(E621DownloadedImage img : downloads)
				{
					ImageView imgView = new ImageView(getApplicationContext());
					RelativeLayout rel = new RelativeLayout(getApplicationContext());
					ProgressBar bar = new ProgressBar(getApplicationContext());
					
					int image_height = (int) (layout_width * (((double) img.height) / img.width));

					if(image_height == 0)
					{
						image_height = layout_width;
					}

					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
																						layout_width,
																						image_height);
					imgView.setLayoutParams(lp);

					rel.setPadding(0, 20, 0, 20);

					imageViews.add(imgView);

					rel.addView(bar);
					rel.addView(imgView);
					layout.addView(rel);

					imgView.setTag(R.id.imagePosition, position);
					imgView.setTag(R.id.imageObject, img);

					imgView.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							imageClick(v);
						}
					});
					
					RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) bar.getLayoutParams();
					layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
					bar.setLayoutParams(layoutParams);
					
					ImageViewHandler handler = new ImageViewHandler(imgView, bar);
					
					scroll.addThread(new Thread(new ImageLoadRunnable(handler, img)), image_y);
					
					if(e621.lazyLoad())
					{
						image_y += image_height + 40;
					}
					
					position++;
				}
			}
		});
	}
	
	public void imageClick(View view)
	{
		Intent intent = new Intent(this, ImageFullScreenActivity.class);
		intent.putExtra(ImageFullScreenActivity.NAVIGATOR, new OfflineImageNavigator(
																							(Integer) view.getTag(R.id.imagePosition),
																							search));
		intent.putExtra(ImageFullScreenActivity.INTENT, getIntent());
		startActivity(intent);
	}
	
	public void search(View view)
	{
		EditText editText = (EditText) findViewById(R.id.searchInput);
		String search = editText.getText().toString().trim();

		Intent intent = new Intent(this, DownloadsActivity.class);
		intent.putExtra(DownloadsActivity.SEARCH, search);
		startActivity(intent);
	}

	public void skipToPage(View view)
	{
		if(downloads != null)
		{
			final PageSelectorDialog dialog = new PageSelectorDialog(this, total_pages);

			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialogInterface, int i)
				{
				}
			});
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Jump", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialogInterface, int i)
				{
					goToPage(dialog.getValue() - 1);
				}
			});

			dialog.show();
		}
	}

	public void prev(View view)
	{
		if(page > 0)
		{
			goToPage(page - 1);
		}
	}

	public void next(View view)
	{
		if(page + 1 < total_pages)
		{
			goToPage(page + 1);
		}
	}

	protected void goToPage(int newPage)
	{
		Intent intent = new Intent(this, DownloadsActivity.class);
		intent.putExtra(DownloadsActivity.SEARCH, search);
		intent.putExtra(DownloadsActivity.PAGE, newPage);
		intent.putExtra(DownloadsActivity.LIMIT, limit);
		startActivity(intent);
	}
	
	private class ImageLoadRunnable implements Runnable
	{
		ImageViewHandler handler;
		E621DownloadedImage id;
		
		public ImageLoadRunnable(ImageViewHandler handler, E621DownloadedImage id)
		{
			this.handler = handler;
			this.id = id;
		}

		@Override
		public void run()
		{
			InputStream in = e621.getDownloadedImageThumb(id);
			Message msg = handler.obtainMessage();
			msg.obj = in;

			handler.sendMessage(msg);
		}
	}
}
