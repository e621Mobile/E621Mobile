package info.beastarman.e621.frontend;

import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.OnlineImageNavigator;
import info.beastarman.e621.middleware.SearchQuery;
import info.beastarman.e621.views.LazyRunScrollView;

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

		min_id = getIntent().getIntExtra(SearchActivity.MIN_ID, -1);
		max_id = getIntent().getIntExtra(SearchActivity.MAX_ID, -1);
		
		cur_min_id = min_id = (min_id == -1 ? null : min_id);
		cur_max_id = max_id = (max_id == -1 ? null : max_id);
		
		previous_page = getIntent().getIntExtra(SearchActivity.PREVIOUS_PAGE, -666);
		if(previous_page < 0)
		{
			previous_page = null;
		}
		
		Long e621SearchKey = getIntent().getLongExtra(SearchActivity.PRELOADED_SEARCH, -1);
		
		if(e621SearchKey != -1)
		{
			e621Search = (E621Search) e621.getStorage().returnKey(e621SearchKey);
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
			text = String.format(res.getString(R.string.page_counter), String.valueOf(page + 1), "...");
		}
		else
		{
			text = String.format(res.getString(R.string.page_counter), String.valueOf(page + 1), String.valueOf(total_pages));
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
				
				if(e621.antecipateOnlyOnWiFi() && !e621.isWifiConnected())
				{
					return;
				}
				
				E621Search nextSearch = get_results(page + 1);
				
				if(nextSearch != null)
				{
					nextE621Search = e621.getStorage().rent(nextSearch);

					for(final E621Image img : nextSearch.images)
					{
						new Thread(new Runnable()
						{
							public void run()
							{
								e621.getImage(img, e621.getFileThummbnailSize(img));
							}
						}).start();
					}
				}
			}
		}).start();
	}
	
	protected Integer getSearchResultsPages(String search, int limit)
	{
		return e621.getSearchResultsPages(search, limit);
	}
	
	protected E621Search get_results(int page)
	{
		try
		{
			return e621.post__index(search, page, limit);
		}
		catch(IOException e)
		{
			return null;
		}
	}

	Integer lastScrollY = null;

	@Override
	public void onStart()
	{
		super.onStart();

		if(e621Search != null)
		{
			getWindow().getDecorView().post(new Runnable()
			{
				public void run()
				{
					update_results();
				}
			});
		}
	}

	@Override
	public void onStop()
	{
		super.onStop();

		LazyRunScrollView scroll = (LazyRunScrollView) findViewById(R.id.resultsScrollView);
		lastScrollY = scroll.getScrollY();

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
		
		for(ImageEventManager event : events)
		{
			e621.unbindDownloadState(event.image.id, event);
		}
		
		events.clear();

		LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);
		layout.removeAllViews();
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
										   min_id != null ? String.valueOf(min_id) : null,
										   max_id != null ? String.valueOf(max_id) : null);
			}
		}).start();
		
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
										   cur_min_id != null ? String.valueOf(cur_min_id) : null,
										   cur_max_id != null ? String.valueOf(cur_max_id) : null);
			}
		}).start();
		
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	public void open_settings()
	{
		Intent intent;
		intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	public void update_results_retry()
	{
		final LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);
		layout.removeAllViews();

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

	public void update_results_empty()
	{
		final LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);
		layout.removeAllViews();

		Resources res = getResources();
		String text = String.format(res.getString(R.string.page_counter),
										   String.valueOf(e621Search.current_page() + 1), String.valueOf(e621Search.total_pages()));

		TextView page_counter = (TextView) findViewById(R.id.page_counter);
		page_counter.setText(text);

		if(e621Search.images.size() == 0)
		{
			TextView t = new TextView(getApplicationContext());
			t.setText(R.string.no_results);
			t.setGravity(Gravity.CENTER_HORIZONTAL);
			t.setPadding(0, dpToPx(24), 0, 0);

			layout.addView(t);

			return;
		}
	}

	public void update_results_full()
	{
		Resources res = getResources();
		String text = String.format(res.getString(R.string.page_counter),
										   String.valueOf(e621Search.current_page() + 1), String.valueOf(e621Search.total_pages()));

		TextView page_counter = (TextView) findViewById(R.id.page_counter);
		page_counter.setText(text);

		final LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);

		layout.post(new Runnable()
		{
			@Override
			public void run()
			{
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						final int layout_width = layout.getWidth();

						final LazyRunScrollView scroll = (LazyRunScrollView) findViewById(R.id.resultsScrollView);

						final ArrayList<View> views = new ArrayList<View>();

						int position = e621Search.offset;

						for(final E621Image image : e621Search.images)
						{
							E621Image img = new E621Image(image);

							if(!e621.isBlacklisted(img).isEmpty())
							{
								if(e621.blacklistMethod() == E621Middleware.BlacklistMethod.HIDE || e621.blacklistMethod() == E621Middleware.BlacklistMethod.QUERY)
								{
									views.add(new View(SearchActivity.this));

									continue;
								}
								else if(e621.blacklistMethod() == E621Middleware.BlacklistMethod.FLAG)
								{
									img.height = img.width / 3;
									img.sample_height = img.sample_width / 3;
									img.preview_height = img.preview_width / 3;
								}
							}

							final LinearLayout resultWrapper = getResultWrapper(img, layout_width, position);

							ImageEventManager event = new ImageEventManager((ImageButton) resultWrapper.findViewById(R.id.downloadButton), img);

							e621.bindDownloadState(img.id, event);

							events.add(event);

							position++;

							views.add(resultWrapper);
						}

						for(int i = 0; i < e621Search.images.size(); i++)
						{
							E621Image img = e621Search.images.get(i);

							if(!e621.isBlacklisted(img).isEmpty() && (e621.blacklistMethod() == E621Middleware.BlacklistMethod.HIDE || e621.blacklistMethod() == E621Middleware.BlacklistMethod.QUERY))
							{
								continue;
							}

							View resultWrapper = views.get(i);

							final ImageView imgView = (ImageView) resultWrapper.findViewById(R.id.imageView);
							final ProgressBar progressBar = (ProgressBar) resultWrapper.findViewById(R.id.progressBar);

							if(!e621.isBlacklisted(img).isEmpty() && e621.blacklistMethod() == E621Middleware.BlacklistMethod.FLAG)
							{
								runOnUiThread(new Runnable()
								{
									@Override
									public void run()
									{
										imgView.setImageResource(android.R.drawable.ic_menu_report_image);
										imgView.setBackgroundColor(getResources().getColor(R.color.gray));
										progressBar.setVisibility(View.GONE);
									}
								});

								continue;
							}
							else
							{
								imageViews.add(imgView);
							}
						}

						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								layout.removeAllViews();

								int image_y = 0;

								for(int i = 0; i < e621Search.images.size(); i++)
								{
									if(!isAlive())
									{
										return;
									}

									final E621Image img = e621Search.images.get(i);

									if(!e621.isBlacklisted(img).isEmpty() && (e621.blacklistMethod() == E621Middleware.BlacklistMethod.HIDE || e621.blacklistMethod() == E621Middleware.BlacklistMethod.QUERY))
									{
										continue;
									}

									View resultWrapper = views.get(i);

									layout.addView(resultWrapper);

									if(!(!e621.isBlacklisted(img).isEmpty() && e621.blacklistMethod() == E621Middleware.BlacklistMethod.FLAG))
									{
										final ImageView imgView = (ImageView) resultWrapper.findViewById(R.id.imageView);
										final ProgressBar progressBar = (ProgressBar) resultWrapper.findViewById(R.id.progressBar);

										final int _image_y = image_y;

										imgView.post(new Runnable()
										{
											@Override
											public void run()
											{
												scroll.addThread(new Thread(new Runnable()
												{

													@Override
													public void run()
													{
														if(imgView.getWidth() == 0 || imgView.getHeight() == 0)
														{
															return;
														}

														final Bitmap bmp = e621.getThumbnail(img.id, imgView.getWidth(), imgView.getHeight());

														if(!isAlive())
														{
															return;
														}

														runOnUiThread(new Runnable()
														{
															@Override
															public void run()
															{
																if(bmp != null)
																{
																	imgView.setImageBitmap(bmp);

																	RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
																																			bmp.getWidth(),
																																			bmp.getHeight());
																	imgView.setLayoutParams(lp);
																}
																else
																{
																	imgView.setImageResource(R.drawable.bad_image);
																}

																progressBar.setVisibility(View.GONE);

																fadeInImage(imgView);
															}
														});
													}
												}), _image_y);
											}
										});
									}

									if(e621.lazyLoad())
									{
										resultWrapper.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

										image_y += resultWrapper.getMeasuredHeight() + resultWrapper.getPaddingBottom();
									}
								}

								if(lastScrollY != null)
								{
									final LazyRunScrollView scroll = (LazyRunScrollView) findViewById(R.id.resultsScrollView);

									scroll.post(new Runnable()
									{
										@Override
										public void run()
										{
											scroll.scrollTo(0, lastScrollY);
										}
									});
								}
							}
						});
					}
				}).start();
			}
		});
	}

	private void fadeInImage(final ImageView img)
	{
		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				img.setAlpha(interpolatedTime);
			}
		};

		a.setDuration(300);
		img.startAnimation(a);
		((View) img.getParent()).invalidate();
	}

	public void update_results()
	{
		if(e621Search == null)
		{
			update_results_retry();
			
			return;
		}

		if(e621Search.images.isEmpty())
		{
			update_results_empty();

			return;
		}

		update_results_full();
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
		RelativeLayout imageWrapper = generateImageWrapper(img, layout_width, position);
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
		layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		bar.setLayoutParams(layoutParams);
		
		return bar;
	}

	private ImageView generateImageView(E621Image img, int layout_width, int position)
	{
		int image_height = (int) (layout_width * (((double) img.preview_height) / img.preview_width));
		
		ImageView imgView = new ImageView(getApplicationContext());
		ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(new ViewGroup.LayoutParams(
																								 layout_width,
																								 image_height));
		imgView.setLayoutParams(lp);
		
		imgView.setTag(R.id.imageObject, img);
		imgView.setTag(R.id.imagePosition, position);

		imgView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
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
			imageWrapper.setPadding(dpToPx(5), dpToPx(2), dpToPx(5), dpToPx(2));
		}
		
		ProgressBar bar = generateProgressBar();
		ImageView imgView = generateImageView(img, layout_width, position);
		LinearLayout highlights = generateHighlights(img);
		ImageButton download = generateDownloadButton(img);
		
		imgView.setId(R.id.imageView);
		bar.setId(R.id.progressBar);
		download.setId(R.id.downloadButton);
		
		imageWrapper.addView(bar);
		imageWrapper.addView(imgView);
		imageWrapper.addView(highlights);
		imageWrapper.addView(download);
		
		return imageWrapper;
	}

	private LinearLayout generateHighlights(final E621Image img)
	{
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
																					ViewGroup.LayoutParams.WRAP_CONTENT,
																					ViewGroup.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		layout.setLayoutParams(params);

		ArrayList<String> highlights = e621.isBlacklisted(img);

		if(highlights.isEmpty())
		{
			highlights = e621.isHighlighted(img);
		}

		if(highlights.isEmpty())
		{
			return layout;
		}

		Collections.sort(highlights, new Comparator<String>()
		{
			@Override
			public int compare(String s, String s2)
			{
				return s.length() - s2.length();
			}
		});

		for(String query : highlights)
		{
			layout.addView(generateHighlightQuery(query));
		}

		return layout;
	}

	private View generateHighlightQuery(String query)
	{
		TextView t = new TextView(this);

		t.setBackgroundColor(getResources().getColor(R.color.gray));
		t.setTextColor(getResources().getColor(R.color.white));
		t.setText(query);
		t.setAlpha(0.5f);

		return t;
	}

	private ImageButton generateDownloadButton(final E621Image img)
	{
		final ImageButton download = new ImageButton(getApplicationContext());
		
		if(!e621.downloadInSearch() || ((!e621.isBlacklisted(img).isEmpty()) && e621.blacklistMethod() == E621Middleware.BlacklistMethod.FLAG))
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
			detailsText += "↕0";
		}
		else if(img.score > 0)
		{
			detailsText += "<font color=#00FF00>↑" + String.valueOf(img.score) + "</font>";
		}
		else if(img.score < 0)
		{
			detailsText += "<font color=#FF0000>↓" + String.valueOf(img.score) + "</font>";
		}

		detailsText += " ♥" + String.valueOf(img.fav_count);
		
		if(img.has_comments)
		{
			detailsText += " C";
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

	public void imageClick(View view)
	{
		Intent intent = new Intent(this, ImageFullScreenActivity.class);
		intent.putExtra(ImageFullScreenActivity.NAVIGATOR, new OnlineImageNavigator(
																						   (E621Image) view.getTag(R.id.imageObject),
																						   (Integer) view.getTag(R.id.imagePosition),
																						   search,
																						   limit,
																						   e621Search));
		intent.putExtra(ImageFullScreenActivity.INTENT, getIntent());
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
		if(page > 0)
		{
			if(e621Search == null)
			{
				return;
			}
			
			if(e621Search != null && !e621Search.has_prev_page())
			{
				return;
			}
			
			if(previous_page != null && previous_page == page - 1)
			{
				finish();
			}
			else
			{
				goToPage(page - 1);
			}
		}
	}

	public void next(View view)
	{
		if(e621Search == null)
		{
			return;
		}
		
		if(e621Search != null && !e621Search.has_next_page())
		{
			return;
		}

		if(previous_page != null && previous_page == page + 1)
		{
			finish();
		}
		else
		{
			goToPage(page + 1);
		}
	}

	public void next(View view)
	{
		if(e621Search == null)
		{
			return;
		}

		if(e621Search != null && !e621Search.has_next_page())
		{
			return;
		}

		if(previous_page != null && previous_page == page + 1)
		{
			finish();
		}
		else
		{
			goToPage(page + 1);
		}
	}

	protected void goToPage(int newPage)
	{
		Intent intent = new Intent(this, SearchActivity.class);
		intent.putExtra(SearchActivity.SEARCH, search);
		intent.putExtra(SearchActivity.PAGE, newPage);
		intent.putExtra(SearchActivity.LIMIT, limit);
		intent.putExtra(SearchActivity.MIN_ID, cur_min_id);
		intent.putExtra(SearchActivity.MAX_ID, cur_max_id);
		intent.putExtra(SearchActivity.PREVIOUS_PAGE, page);

		if(nextE621Search != null && newPage == page + 1)
		{
			intent.putExtra(SearchActivity.PRELOADED_SEARCH, nextE621Search);
		}

		startActivity(intent);
	}	ShareActionProvider mShareActionProvider;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.search, menu);

		return true;
	}

	public void skipToPage(View view)
	{
		if(e621Search != null)
		{
			final PageSelectorDialog dialog = new PageSelectorDialog(this, e621Search.total_pages());

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
					if(obj == E621Middleware.DownloadStatus.DOWNLOADED)
					{
						button.setImageResource(android.R.drawable.ic_menu_delete);
						
						button.setOnClickListener(new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								delete();
							}
						});
					}
					else if(obj == E621Middleware.DownloadStatus.DOWNLOADING)
					{
						button.setImageResource(android.R.drawable.stat_sys_download);
						
						button.setOnClickListener(new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								delete();
							}
						});
					}
					else if(obj == E621Middleware.DownloadStatus.DELETED)
					{
						button.setImageResource(android.R.drawable.ic_menu_save);
						
						button.setOnClickListener(new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								save();
							}
						});
					}
					else if(obj == E621Middleware.DownloadStatus.DELETING)
					{
						button.setImageResource(R.drawable.progress_indicator);
						
						button.setOnClickListener(new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								save();
							}
						});
					}
				}
			});
		}
	}
}
