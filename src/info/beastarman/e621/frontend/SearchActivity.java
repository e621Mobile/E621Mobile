package info.beastarman.e621.frontend;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.ImageLoadRunnable;
import info.beastarman.e621.middleware.ImageViewHandler;
import info.beastarman.e621.middleware.OnlineImageNavigator;
import info.beastarman.e621.middleware.SearchQuery;
import info.beastarman.e621.views.LazyRunScrollView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SearchActivity extends BaseActivity
{
	public static String SEARCH = "search";
	public static String PAGE = "page";
	public static String LIMIT = "limit";
	
	public static String MIN_ID = "min_id";
	public static String MAX_ID = "max_id";
	
	public static String PREVIOUS_PAGE = "previous_page";
	public static String PRELOADED_SEARCH = "preloaded_search";

	public String search = "";
	public int page = 0;
	public int limit;

	public Integer min_id = null;
	public Integer max_id = null;

	public Integer cur_min_id = null;
	public Integer cur_max_id = null;
	
	public Integer previous_page = null;

	protected E621Search e621Search = null;
	protected Long nextE621Search = null;
	private ArrayList<ImageView> imageViews = new ArrayList<ImageView>();
	
	private Set<ImageEventManager> events = new HashSet<ImageEventManager>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		ActionBar actionBar = getActionBar();
	    actionBar.setDisplayHomeAsUpEnabled(true);

		search = getIntent().getStringExtra(SearchActivity.SEARCH);
		if(search == null)
		{
			search = "";
		}
		page = getIntent().getIntExtra(SearchActivity.PAGE, 0);
		limit = getIntent().getIntExtra(SearchActivity.LIMIT, e621.resultsPerPage());

		min_id = getIntent().getIntExtra(SearchActivity.MIN_ID,-1);
		max_id = getIntent().getIntExtra(SearchActivity.MAX_ID,-1);
		
		cur_min_id = min_id = (min_id==-1?null:min_id);
		cur_max_id = max_id = (max_id==-1?null:max_id);
		
		previous_page = getIntent().getIntExtra(SearchActivity.PREVIOUS_PAGE, -666);
		if(previous_page<0) previous_page = null;
		
		Long e621SearchKey = getIntent().getLongExtra(SearchActivity.PRELOADED_SEARCH,-1);
		
		if(e621SearchKey != -1)
		{
			e621Search = e621.getStorage().returnKey(e621SearchKey);
		}
		
		trySearch();
		
		((EditText) findViewById(R.id.searchInput)).setText(search);
	}
	
	private void trySearch()
	{
		setContentView(R.layout.activity_search);
		
		Integer total_pages = getSearchResultsPages(search, limit);
		
		Resources res = getResources();
		
		String text;
		
		if(total_pages == null)
		{
			text = String.format(res.getString(R.string.page_counter),String.valueOf(page + 1),"...");
		}
		else
		{
			text = String.format(res.getString(R.string.page_counter),String.valueOf(page + 1),String.valueOf(total_pages));
		}
		
		TextView page_counter = (TextView) findViewById(R.id.page_counter);
		page_counter.setText(text);
		
		new Thread(new Runnable()
		{
			public void run()
			{
				if(e621Search == null)
				{
					e621Search = get_results(page);
					
					runOnUiThread(new Runnable()
					{
						public void run()
						{
							update_results();
						}
					});
				}
				else
				{
					runOnUiThread(new Runnable()
					{
						public void run()
						{
							getWindow().getDecorView().post(new Runnable()
							{
								public void run()
								{
									update_results();
								}
							});
						}
					});
				}
				
				if(e621.antecipateOnlyOnWiFi() && !e621.isWifiConnected())
				{
					return;
				}
				
				E621Search nextSearch = get_results(page+1);
				
				if(nextSearch != null)
				{
					nextE621Search = e621.getStorage().rent(nextSearch);
					
					for(final E621Image img : nextSearch.images)
					{
						new Thread(new Runnable()
						{
							public void run()
							{
								e621.getImage(img,E621Image.PREVIEW);
							}
						}).start();
					}
				}
			}
		}).start();
	}
	
	protected Integer getSearchResultsPages(String search, int limit)
	{
		return e621.getSearchResultsPages(search,limit);
	}
	
	protected E621Search get_results(int page)
	{
		try {
			return e621.post__index(search, page, limit);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		if (e621Search != null)
		{
			update_results();
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		for (ImageView img : imageViews) {
			Drawable drawable = img.getDrawable();
			if (drawable instanceof BitmapDrawable) {
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				Bitmap bitmap = bitmapDrawable.getBitmap();
				
				if(bitmap != null)
				{
					bitmap.recycle();
				}
			}
		}

		imageViews.clear();
		
		for(ImageEventManager event : events)
		{
			e621.unbindDownloadState(event.image.id, event);
		}
		
		events.clear();

		LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);
		layout.removeAllViews();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_settings:
			open_settings();
			return true;
		case R.id.action_offine_search:
			offline_search();
			return true;
		case R.id.action_continue_later:
			continue_later();
			return true;
		case R.id.action_continue_later_after:
			continue_later_after();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	public void offline_search()
	{
		Intent intent = new Intent(this, DownloadsActivity.class);
		intent.putExtra(DownloadsActivity.SEARCH, search);
		startActivity(intent);
	}
	
	public void continue_later()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				e621.continue_later(SearchQuery.normalize(search),
						min_id!=null?String.valueOf(min_id):null,
						max_id!=null?String.valueOf(max_id):null);
			}
		}).start();
		
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}
	
	public void continue_later_after()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				e621.continue_later(SearchQuery.normalize(search),
						cur_min_id!=null?String.valueOf(cur_min_id):null,
						cur_max_id!=null?String.valueOf(cur_max_id):null);
			}
		}).start();
		
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}

	public void open_settings() {
		Intent intent;
		intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	public void update_results()
	{
		final LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);
		layout.removeAllViews();

		if (e621Search == null)
		{
			TextView t = new TextView(getApplicationContext());
			t.setText(R.string.no_internet_no_results);
			t.setGravity(Gravity.CENTER_HORIZONTAL);
			t.setPadding(0, dpToPx(24), 0, dpToPx(24));

			layout.addView(t);

			Button b = new Button(getApplicationContext());
			b.setText("Try Again");
			b.setGravity(Gravity.CENTER_HORIZONTAL);
			
			b.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					trySearch();
				}
			});

			layout.addView(b);
			
			return;
		}
		
		Resources res = getResources();
		String text = String.format(res.getString(R.string.page_counter),
				String.valueOf(e621Search.current_page() + 1), String.valueOf(e621Search.total_pages()));

		TextView page_counter = (TextView) findViewById(R.id.page_counter);
		page_counter.setText(text);
		
		if (e621Search.images.size() == 0) {
			TextView t = new TextView(getApplicationContext());
			t.setText(R.string.no_results);
			t.setGravity(Gravity.CENTER_HORIZONTAL);
			t.setPadding(0, dpToPx(24), 0, 0);

			layout.addView(t);

			return;
		}

		final int layout_width = layout.getWidth();
		
		final LazyRunScrollView scroll = (LazyRunScrollView)findViewById(R.id.resultsScrollView);

		final ArrayList<View> views = new ArrayList<View>();
		
		int position = e621Search.offset;
		
		for (final E621Image img : e621Search.images)
		{
			final LinearLayout resultWrapper = getResultWrapper(img,layout_width,position);
			
			ImageEventManager event = new ImageEventManager((ImageButton)resultWrapper.findViewById(R.id.downloadButton),img);
			
			e621.bindDownloadState(img.id, event);
			
			events.add(event);
			
			position++;
			
			views.add(resultWrapper);
		}
		
		int image_y = 0;
		
		for(int i=0; i<e621Search.images.size(); i++)
		{
			E621Image img = e621Search.images.get(i);
			View resultWrapper = views.get(i);
			
			ImageView imgView = (ImageView) resultWrapper.findViewById(R.id.imageView);
			ProgressBar progressBar = (ProgressBar) resultWrapper.findViewById(R.id.progressBar);
			
			layout.addView(resultWrapper);
			ImageViewHandler handler = new ImageViewHandler(imgView, progressBar);
			
			scroll.addThread(new Thread(new ImageLoadRunnable(handler, img, e621,E621Image.PREVIEW)),image_y);
			
			imageViews.add(imgView);
			
			resultWrapper.measure(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
			
			if(e621.lazyLoad()) image_y += resultWrapper.getMeasuredHeight() + dpToPx(10);
		}
	}
	
	private LinearLayout getResultWrapper(E621Image img, int layout_width, int position)
	{
		LinearLayout resultWrapper = new LinearLayout(getApplicationContext());
		RelativeLayout rel = new RelativeLayout(getApplicationContext());
		RelativeLayout detailsWrapper = new RelativeLayout(getApplicationContext());
		
		resultWrapper.setOrientation(LinearLayout.VERTICAL);
		
		ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		resultWrapper.setLayoutParams(lp);
		
		lp = new ViewGroup.LayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		detailsWrapper.setLayoutParams(lp);
		detailsWrapper.setBackgroundColor(getResources().getColor(R.color.detailsBackgroundColor));
		
		TextView details = getImageFooter(img);
		RelativeLayout imageWrapper = generateImageWrapper(img,layout_width,position);
		updateCurMinMax(img);
		
		resultWrapper.setPadding(0, dpToPx(10), 0, dpToPx(10));
		
		rel.addView(imageWrapper);
		resultWrapper.addView(rel);
		
		detailsWrapper.addView(details);
		resultWrapper.addView(detailsWrapper);
		
		return resultWrapper;
	}
	
	private ProgressBar generateProgressBar()
	{
		ProgressBar bar = new ProgressBar(getApplicationContext());

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT, 
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT,
				RelativeLayout.TRUE);
		bar.setLayoutParams(layoutParams);
		
		return bar;
	}

	private ImageView generateImageView(E621Image img, int layout_width, int position)
	{
		int image_height = (int) (layout_width * (((double)img.preview_height) / img.preview_width));
		
		ImageView imgView = new ImageView(getApplicationContext());
		ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(new ViewGroup.LayoutParams(
				layout_width,
				image_height));
		imgView.setLayoutParams(lp);
		
		imgView.setTag(R.id.imageObject, img);
		imgView.setTag(R.id.imagePosition, position);

		imgView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				imageClick(v);
			}
		});
		
		return imgView;
	}

	private RelativeLayout generateImageWrapper(E621Image img, int layout_width, int position)
	{
		RelativeLayout imageWrapper = new RelativeLayout(getApplicationContext());
		
		boolean has_border = false;

		if(img.status == E621Image.FLAGGED)
		{
			imageWrapper.setBackgroundResource(R.drawable.delete_mark);
			has_border = true;
		}
		else if(img.status == E621Image.PENDING)
		{
			imageWrapper.setBackgroundResource(R.drawable.pending_mark);
			has_border = true;
		}
		else if(img.parent_id != null)
		{
			imageWrapper.setBackgroundResource(R.drawable.has_parent_mark);
			has_border = true;
		}
		else if(img.has_children)
		{
			imageWrapper.setBackgroundResource(R.drawable.has_children_mark);
			has_border = true;
		}
		
		if(has_border)
		{
			imageWrapper.setPadding(dpToPx(5),dpToPx(2),dpToPx(5),dpToPx(2));
		}
		
		ProgressBar bar = generateProgressBar();
		ImageView imgView = generateImageView(img,layout_width,position);
		ImageButton download = generateDownloadButton(img);
		
		imgView.setId(R.id.imageView);
		bar.setId(R.id.progressBar);
		download.setId(R.id.downloadButton);
		
		imageWrapper.addView(bar);
		imageWrapper.addView(imgView);
		imageWrapper.addView(download);
		
		return imageWrapper;
	}

	private ImageButton generateDownloadButton(final E621Image img)
	{
		final ImageButton download = new ImageButton(getApplicationContext());
		
		if(!e621.downloadInSearch())
		{
			download.setVisibility(View.GONE);
			return download;
		}

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
			    dpToPx(55), 
			    dpToPx(45));
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		download.setLayoutParams(params);
		
		download.setImageResource(R.drawable.spinner);
		
		return download;
	}

	private void updateCurMinMax(E621Image img)
	{
		if(cur_min_id != null)
		{
			cur_min_id = Math.min(cur_min_id, img.id);
		}
		else
		{
			cur_min_id = img.id;
		}

		if(cur_max_id != null)
		{
			cur_max_id = Math.max(cur_max_id, img.id);
		}
		else
		{
			cur_max_id = img.id;
		}
	}

	private TextView getImageFooter(E621Image img)
	{
		String detailsText = "";
		
		if(img.score == 0)
		{
			detailsText += "0";
		}
		else if(img.score > 0)
		{
			detailsText += "<font color=#00FF00>↑" + String.valueOf(img.score) + "</font>";
		}
		else if(img.score < 0)
		{
			detailsText += "<font color=#FF0000>↓" + String.valueOf(img.score) + "</font>";
		}
		
		if(img.has_comments)
		{
			detailsText += " C";
		}
		
		if(img.file_ext.equals("gif") || img.file_ext.equals("swf"))
		{
			detailsText += " A";
		}
		
		if(img.rating.equals(E621Image.EXPLICIT))
		{
			detailsText += " <font color=#FF0000>E</font>";
		}
		else if(img.rating.equals(E621Image.QUESTIONABLE))
		{
			detailsText += " <font color=#FFFF00>Q</font>";
		}
		else if(img.rating.equals(E621Image.SAFE))
		{
			detailsText += " <font color=#00FF00>S</font>";
		}
		
		TextView details = new TextView(getApplicationContext());
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
			    RelativeLayout.LayoutParams.WRAP_CONTENT, 
			    RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		details.setLayoutParams(params);
		details.setText(Html.fromHtml(detailsText));
		
		return details;
	}

	public void imageClick(View view) {
		Intent intent = new Intent(this, ImageActivity.class);
		intent.putExtra(ImageActivity.NAVIGATOR, new OnlineImageNavigator(
				(E621Image) view.getTag(R.id.imageObject),
				(Integer) view.getTag(R.id.imagePosition),
				search,
				limit,
				e621Search));
		intent.putExtra(ImageActivity.INTENT,getIntent());
		startActivity(intent);
	}

	public void search(View view)
	{
		EditText editText = (EditText) findViewById(R.id.searchInput);
		String search = editText.getText().toString().trim();
		
		if(page == 0 && search.equals(this.search))
		{
			return;
		}

		Intent intent = new Intent(this, SearchActivity.class);
		intent.putExtra(SearchActivity.SEARCH, search);
		startActivity(intent);
	}

	public void prev(View view)
	{
		if (page > 0)
		{
			if(e621Search == null) return;
			
			if(e621Search != null && !e621Search.has_prev_page())
			{
				return;
			}
			
			if(previous_page!=null && previous_page == page-1)
			{
				finish();
			}
			else
			{
				Intent intent = new Intent(this, SearchActivity.class);
				intent.putExtra(SearchActivity.SEARCH, search);
				intent.putExtra(SearchActivity.PAGE, page - 1);
				intent.putExtra(SearchActivity.LIMIT, limit);
				intent.putExtra(SearchActivity.MIN_ID, cur_min_id);
				intent.putExtra(SearchActivity.MAX_ID, cur_max_id);
				intent.putExtra(SearchActivity.PREVIOUS_PAGE, page);
				
				if(nextE621Search != null)
				{
					intent.putExtra(SearchActivity.PRELOADED_SEARCH, nextE621Search);
				}
				
				startActivity(intent);
			}
		}
	}

	public void next(View view)
	{
		if(e621Search == null) return;
		
		if(e621Search != null && !e621Search.has_next_page())
		{
			return;
		}

		if(previous_page!=null && previous_page == page+1)
		{
			finish();
		}
		else
		{
			Intent intent = new Intent(this, SearchActivity.class);
			intent.putExtra(SearchActivity.SEARCH, search);
			intent.putExtra(SearchActivity.PAGE, page + 1);
			intent.putExtra(SearchActivity.LIMIT, limit);
			intent.putExtra(SearchActivity.MIN_ID, cur_min_id);
			intent.putExtra(SearchActivity.MAX_ID, cur_max_id);
			intent.putExtra(SearchActivity.PREVIOUS_PAGE, page);
			
			if(nextE621Search != null)
			{
				intent.putExtra(SearchActivity.PRELOADED_SEARCH, nextE621Search);
			}
			
			startActivity(intent);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.search, menu);
		return true;
	}
	
	private class ImageEventManager extends EventManager
	{
		private ImageButton button;
		private E621Image image;
		
		public ImageEventManager(ImageButton button, E621Image image)
		{
			this.button = button;
			this.image = image;
		}

		private void delete()
		{
			new Thread(new Runnable()
			{
				public void run()
				{
					e621.deleteImage(image);
				}
			}).start();
		}

		private void save()
		{
			new Thread(new Runnable()
			{
				public void run()
				{
					e621.saveImage(image);
				}
			}).start();
		}
		
		@Override
		public void onTrigger(final Object obj)
		{
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					if(obj ==  E621Middleware.DownloadStatus.DOWNLOADED)
					{
						button.setImageResource(android.R.drawable.ic_menu_delete);
						
						button.setOnClickListener(new View.OnClickListener() {
					        @Override
					        public void onClick(View v) {
					        	delete();
					        }
					    });
					}
					else if(obj == E621Middleware.DownloadStatus.DOWNLOADING)
					{
						button.setImageResource(android.R.drawable.stat_sys_download);
						
						button.setOnClickListener(new View.OnClickListener() {
					        @Override
					        public void onClick(View v) {
					        	delete();
					        }
					    });
					}
					else if(obj == E621Middleware.DownloadStatus.DELETED)
					{
						button.setImageResource(android.R.drawable.ic_menu_save);
						
						button.setOnClickListener(new View.OnClickListener() {
					        @Override
					        public void onClick(View v) {
					        	save();
					        }
					    });
					}
					else if(obj == E621Middleware.DownloadStatus.DELETING)
					{
						button.setImageResource(R.drawable.progress_indicator);
						
						button.setOnClickListener(new View.OnClickListener() {
					        @Override
					        public void onClick(View v) {
					        	save();
					        }
					    });
					}
				}
			});
		}
	}
}
