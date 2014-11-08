package info.beastarman.e621.frontend;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.text.Html;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.api.E621Tag;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.ImageNavigator;
import info.beastarman.e621.middleware.NowhereToGoImageNavigator;
import info.beastarman.e621.middleware.OnlineImageNavigator;
import info.beastarman.e621.views.ZoomableRelativeLayout;

public class ImageFullScreenActivity extends BaseActivity
{
	public static String NAVIGATOR = "navigator";
	public static String INTENT = "intent";

	public ImageNavigator image;
	public Intent intent;

	E621Image img = null;

	ScaleGestureDetector scaleGestureDetector;
	GestureDetector onTapListener;

	boolean scaling = false;

	int IMAGE_CHUNK_SIZE = 128;
	float TABS_HEIGHT = 0.7f;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_full_screen_activity);

		image = (ImageNavigator) getIntent().getSerializableExtra(NAVIGATOR);

		setTitle("#" + image.getId());

		intent = (Intent) getIntent().getParcelableExtra(INTENT);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.BackgroundColorTransparent)));

		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

		scaleGestureDetector = new ScaleGestureDetector(this, new OnPinchListener());
		onTapListener = new GestureDetector(this,new OnTapListener());

		final TabHost tabHost = (TabHost) findViewById(R.id.tabHost);

		tabHost.setup();
		tabHost.addTab(tabHost.newTabSpec("Info").setIndicator("Info").setContent(R.id.info));
		tabHost.addTab(tabHost.newTabSpec("Tags").setIndicator("Tags").setContent(R.id.tags));
		tabHost.addTab(tabHost.newTabSpec("Comments").setIndicator("Comments").setContent(R.id.comments));

		tabHost.setVisibility(View.INVISIBLE);

		tabHost.post(new Runnable()
		{
			@Override
			public void run()
			{
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,(int)Math.floor(getHeight()*TABS_HEIGHT));
				tabHost.setLayoutParams(params);
			}
		});

		tabHost.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent)
			{
				return true;
			}
		});
	}

	SearchView searchView = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.image_full_screen, menu);

		MenuItem searchItem = menu.findItem(R.id.action_search);
		searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
		{
			@Override
			public boolean onQueryTextSubmit(String s)
			{
				Intent i = new Intent(intent);

				Set<String> set = i.getExtras().keySet();

				for(String extra : set)
				{
					i.removeExtra(extra);
				}

				i.putExtra(SearchActivity.SEARCH,s);

				startActivity(i);

				return false;
			}

			@Override
			public boolean onQueryTextChange(String s)
			{
				return false;
			}
		});

		return true;
	}

	private int getUIInvisible()
	{
		int ret = View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

		if(Build.VERSION.SDK_INT > 18)
		{
			ret |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}

		return ret;
	}

	private int getUIVisible()
	{
		int ret = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

		return ret;
	}

	protected void onStart()
	{
		super.onStart();

		View zoomableLayout = findViewById(R.id.imageWrapper);

		zoomableLayout.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					scaling = false;
				}

				if(onTapListener.onTouchEvent(event))
				{
					return true;
				}
				else if(scaleGestureDetector.onTouchEvent(event))
				{
					return true;
				}
				return false;
			}
		});

		getWindow().getDecorView().post(new Runnable()
		{
			@Override
			public void run()
			{
				retrieveImage();
			}
		});
	}

	private void retrieveImage()
	{
		if(img == null)
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						img = e621.post__show(image.getId());

						updateImage();
					} catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}).start();
		}
		else
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					updateImage();
				}
			}).start();
		}
	}

	private float getImageScale()
	{
		int w;
		int h;

		switch (e621.getFileDownloadSize())
		{
			case E621Image.PREVIEW:
				w = img.preview_width;
				h = img.preview_height;
				break;
			case E621Image.SAMPLE:
				w = img.sample_width;
				h = img.sample_height;
				break;
			default:
				w = img.width;
				h = img.height;
				break;
		}

		float scale = ((float)w) / getWindow().getDecorView().getWidth();
		scale = Math.max(scale, ((float)h) / getWindow().getDecorView().getHeight());
		scale = Math.max(scale,1f);

		return scale;
	}

	private void updateImage()
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				hideUI();
			}
		});

		updateInfo();

		showImage();
	}

	Boolean is_faved = null;

	private void updateInfo()
	{
		updateFav();

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				updateParent();

				updateChildren();
			}
		});
	}

	private void updateParent()
	{
		final View parentWrapper = findViewById(R.id.parentWrapper);

		if(img.parent_id != null)
		{
			TextView parent_id = (TextView) findViewById(R.id.parent_id);
			parent_id.setText("#" + img.parent_id);

			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					E621Image child = new E621Image(img);
					final E621Image parent;

					try
					{
						parent = new E621Image(e621.post__show(Integer.parseInt(img.parent_id)));
					} catch (IOException e)
					{
						return;
					}

					for(E621Tag tag : new ArrayList<E621Tag>(child.tags))
					{
						if(parent.tags.contains(tag))
						{
							child.tags.remove(tag);
							parent.tags.remove(tag);
						}
					}

					String tags = "";

					for(E621Tag tag : parent.tags)
					{
						if(tags.length() > 0)
						{
							tags += ", ";
						}

						tags += "<font color=#80FF80>+" + tag.getTag().replace('_',' ') + "</font>";
					}

					for(E621Tag tag : child.tags)
					{
						if(tags.length() > 0)
						{
							tags += ", ";
						}

						tags += "<font color=#FF8080>-" + tag.getTag().replace('_',' ') + "</font>";
					}

					final TextView parentTags = (TextView) findViewById(R.id.parentTags);

					final String ttags = tags;

					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							if(ttags.length() != 0)
							{
								parentTags.setVisibility(View.VISIBLE);
								parentTags.setText(Html.fromHtml(ttags));
							}

							parentWrapper.post(new Runnable()
							{
								@Override
								public void run()
								{
									final int height;// = parentWrapper.getHeight();
									final int width;// = Math.min(height, height * parent.preview_width / parent.preview_height);

									int scale = Math.max(1, Math.min(parent.preview_width / (parentWrapper.getWidth() / 5), parent.preview_height / parentWrapper.getHeight()));

									width = parent.preview_width / scale;
									height = parent.preview_height / scale;

									new Thread(new Runnable()
									{
										@Override
										public void run()
										{
											InputStream in = e621.getImage(parent,E621Image.PREVIEW);

											if(in == null)
											{
												return;
											}

											final Bitmap bmp = e621.decodeFile(in,width,height);

											final ImageView iv = (ImageView) parentWrapper.findViewById(R.id.parentThumbnail);

											runOnUiThread(new Runnable()
											{
												@Override
												public void run()
												{
													iv.setImageBitmap(bmp);
												}
											});
										}
									}).start();
								}
							});
						}
					});
				}
			}).start();
		}
		else
		{
			parentWrapper.setVisibility(View.GONE);
		}
	}

	private void updateChildren()
	{
		final View childrenWrapper = findViewById(R.id.childrenWrapper);

		if(img.has_children)
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					E621Search children;

					try
					{
						children = getChildren();
					} catch (IOException e)
					{
						e.printStackTrace();

						return;
					}

					final LinearLayout ll = (LinearLayout) findViewById(R.id.childrenGroup);
					final ArrayList<View> views = new ArrayList<View>();

					for(E621Image child : children.images)
					{
						views.add(getChildView(child));
					}

					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							ll.removeAllViews();

							for(View v : views)
							{
								ll.addView(v);
							}
						}
					});
				}
			}).start();
		}
		else
		{
			childrenWrapper.setVisibility(View.GONE);
		}
	}

	public View getChildView(final E621Image child)
	{
		final View view = getLayoutInflater().inflate(R.layout.image_full_screen_child_post,null,false);

		TextView t = (TextView) view.findViewById(R.id.image_id);
		t.setText("#"+child.id);

		E621Image original = new E621Image(img);
		E621Image other = new E621Image(child);

		for(E621Tag tag : new ArrayList<E621Tag>(original.tags))
		{
			if(other.tags.contains(tag))
			{
				original.tags.remove(tag);
				other.tags.remove(tag);
			}
		}

		String tags = "";

		for(E621Tag tag : other.tags)
		{
			if(tags.length() > 0)
			{
				tags += ", ";
			}

			tags += "<font color=#80FF80>+" + tag.getTag().replace('_',' ') + "</font>";
		}

		for(E621Tag tag : original.tags)
		{
			if(tags.length() > 0)
			{
				tags += ", ";
			}

			tags += "<font color=#FF8080>-" + tag.getTag().replace('_',' ') + "</font>";
		}

		TextView childTags = (TextView) view.findViewById(R.id.image_tags);

		childTags.setVisibility(View.VISIBLE);
		childTags.setText(Html.fromHtml(tags));

		view.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				goToChild(child);
			}
		});

		view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener()
		{
			@Override
			public void onViewAttachedToWindow(View v)
			{
				view.post(new Runnable()
				{
					@Override
					public void run()
					{
						Log.d(E621Middleware.LOG_TAG, "I AM ALIVE!!!! " + view.getWidth());

						final int height;
						final int width;

						int scale = Math.max(1, Math.min(child.preview_width / (view.getWidth() / 5), child.preview_height / view.getHeight()));

						width = child.preview_width / scale;
						height = child.preview_height / scale;

						new Thread(new Runnable()
						{
							@Override
							public void run()
							{
								InputStream in = e621.getImage(child, E621Image.PREVIEW);

								if (in == null)
								{
									return;
								}

								final Bitmap bmp = e621.decodeFile(in, width, height);

								final ImageView iv = (ImageView) view.findViewById(R.id.image_thumbnail);

								runOnUiThread(new Runnable()
								{
									@Override
									public void run()
									{
										iv.setImageBitmap(bmp);
									}
								});
							}
						}).start();
					}
				});
			}

			@Override
			public void onViewDetachedFromWindow(View view)
			{

			}
		});

		return view;
	}

	public void searchChildren(View v)
	{
		Intent i = new Intent(this,SearchActivity.class);
		i.putExtra(SearchActivity.SEARCH, "parent:" + image.getId());
		startActivity(i);
	}

	E621Search children = null;
	private synchronized E621Search getChildren() throws IOException
	{
		if(children == null)
		{
			children = e621.post__index("parent:"+image.getId(),0,100);
		}

		return children;
	}

	public void goToChild(final E621Image image)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				E621Search children = null;

				try
				{
					children = getChildren();
				} catch (IOException e)
				{
					e.printStackTrace();
				}

				Intent i = new Intent(ImageFullScreenActivity.this,ImageFullScreenActivity.class);
				i.putExtra(ImageFullScreenActivity.INTENT,intent);

				if(children == null)
				{
					i.putExtra(ImageFullScreenActivity.NAVIGATOR,new NowhereToGoImageNavigator(image.id));
				}
				else
				{
					int pos = -1;

					for(int j=0; j<children.images.size(); j++)
					{
						if(children.images.get(j).id == image.id)
						{
							pos = j;

							break;
						}
					}

					if(pos >= 0)
					{
						i.putExtra(ImageFullScreenActivity.NAVIGATOR,new OnlineImageNavigator(image,pos,"parent:"+img.id,100,children));
					}
					else
					{
						i.putExtra(ImageFullScreenActivity.NAVIGATOR,new NowhereToGoImageNavigator(image.id));
					}
				}

				startActivity(i);
			}
		}).start();
	}

	public void goToParent(View v)
	{
		if(img.parent_id != null)
		{
			Intent i = new Intent(this, ImageFullScreenActivity.class);

			i.putExtra(ImageFullScreenActivity.NAVIGATOR, new NowhereToGoImageNavigator(Integer.parseInt((img.parent_id))));
			i.putExtra(ImageFullScreenActivity.INTENT, intent);

			startActivity(i);
		}
	}

	private void updateFav()
	{
		if(!e621.isLoggedIn())
		{
			ImageButton favButton = (ImageButton) findViewById(R.id.favButton);
			favButton.setImageResource(android.R.drawable.star_big_off);

			return;
		}

		new Thread(new Runnable() {
			public void run()
			{
				E621Search search;
				try {
					search = e621.post__index("fav:"+e621.getLoggedUser() + " id:" + image.getId(), 0, 1);
				} catch (IOException e) {
					return;
				}

				if(search != null)
				{
					if(search.images.size() > 0)
					{
						is_faved = true;

						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								ImageButton favButton = (ImageButton) findViewById(R.id.favButton);

								favButton.setBackgroundResource(R.drawable.fav_star_enabled_2);
							}
						});
					}
					else
					{
						is_faved = false;

						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								ImageButton favButton = (ImageButton) findViewById(R.id.favButton);

								favButton.setBackgroundResource(R.drawable.fav_star_disabled);
							}
						});
					}
				}
			}
		}).start();
	}

	public void fav(View v)
	{
		if(!e621.isLoggedIn())
		{
			Toast.makeText(getApplicationContext(), "Please log in at the home screen.", Toast.LENGTH_SHORT).show();

			return;
		}

		if(is_faved == null) return;

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				final Boolean ret = e621.post_favorite(image.getId(), !is_faved);

				if(ret != null)
				{
					if(ret)
					{
						is_faved = !is_faved;

						final ImageButton favButton = (ImageButton) findViewById(R.id.favButton);

						if(is_faved)
						{
							runOnUiThread(new Runnable()
							{
								@Override
								public void run()
								{
									favButton.setBackgroundResource(R.drawable.fav_star_enabled_2);
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
									favButton.setBackgroundResource(R.drawable.fav_star_disabled);
								}
							});
						}
					}
				}
			}
		}).start();
	}

	private void showImage()
	{
		final TableLayout tableLayout = (TableLayout) findViewById(R.id.imageViewTable);
		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

		InputStream is = e621.getImage(img, e621.getFileDownloadSize());

		BitmapRegionDecoder decoder = null;

		try
		{
			decoder = BitmapRegionDecoder.newInstance(is, false);
		} catch (IOException e)
		{
			e.printStackTrace();
			return;
		}

		int w = decoder.getWidth();
		int h = decoder.getHeight();

		int w_parts = (int)Math.ceil(w/(double)IMAGE_CHUNK_SIZE);
		int h_parts = (int)Math.ceil(h/(double)IMAGE_CHUNK_SIZE);

		final float scale = getImageScale();
		final int hh = h;
		final int ww = w;

		final ZoomableRelativeLayout zoomableRelativeLayout = (ZoomableRelativeLayout) findViewById(R.id.imageWrapper);

		zoomableRelativeLayout.post(new Runnable()
		{
			@Override
			public void run()
			{
				zoomableRelativeLayout.setPivotPadding(
						(int)(getWidth() - (ww/scale))/2,
						(int)(getHeight() - (hh/scale))/2,
						(int)(getWidth() - (ww/scale))/2,
						(int)(getHeight() - (hh/scale))/2
				);
			}
		});

		final ArrayList<ArrayList<ImageView>> imageViewList = new ArrayList<ArrayList<ImageView>>();

		for(int j=0; j<h_parts; j++)
		{
			ArrayList<ImageView> localArray = new ArrayList<ImageView>();

			for(int i=0; i<w_parts; i++)
			{
				ImageView iv = new ImageView(this);

				int wa = i*IMAGE_CHUNK_SIZE;
				int ha = j*IMAGE_CHUNK_SIZE;

				int wz = (i+1 == w_parts? w :(i+1)*IMAGE_CHUNK_SIZE);
				int hz = (j+1 == h_parts? h :(j+1)*IMAGE_CHUNK_SIZE);

				iv.setImageBitmap(decoder.decodeRegion(new Rect(wa, ha, wz, hz), null));

				iv.setLayoutParams(new TableRow.LayoutParams((int)Math.ceil((wz - wa) / scale), (int)Math.ceil((hz - ha) / scale)));

				try
				{
					is.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}

				localArray.add(iv);
			}

			imageViewList.add(localArray);
		}

		decoder.recycle();

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				tableLayout.removeAllViews();

				for (int i = 0; i < imageViewList.size(); i++)
				{
					ArrayList<ImageView> localArray = imageViewList.get(i);

					TableRow row = new TableRow(ImageFullScreenActivity.this);

					for (int j = 0; j < localArray.size(); j++)
					{
						row.addView(localArray.get(j));
					}

					tableLayout.addView(row);
				}

				progressBar.setVisibility(View.GONE);
			}
		});
	}

	private boolean visible = false;

	private void toggleVisibility()
	{
		if(visible)
		{
			hideUI();
		}
		else
		{
			showUI();

		}
	}

	private void showUI()
	{
		getWindow().getDecorView().setSystemUiVisibility(getUIVisible());

		final TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
		final int height = getHeight();

		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				interpolatedTime = 1f - interpolatedTime;

				final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) tabHost.getLayoutParams();
				params.setMargins(0,(int) (height * ((1-TABS_HEIGHT) + (interpolatedTime*TABS_HEIGHT))),0,0);
				tabHost.setLayoutParams(params);
			}
		};

		tabHost.setVisibility(View.VISIBLE);

		a.setDuration(300);
		tabHost.startAnimation(a);

		visible = true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				goUp();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void goUp()
	{
		if(intent == null)
		{
			intent = new Intent(this, MainActivity.class);
		}

		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	private void hideUI()
	{
		getWindow().getDecorView().setSystemUiVisibility(getUIInvisible());

		final TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
		final int height = getHeight();

		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) tabHost.getLayoutParams();
				params.setMargins(0,(int) (height * ((1-TABS_HEIGHT) + (interpolatedTime*TABS_HEIGHT))),0,0);
				tabHost.setLayoutParams(params);
			}
		};

		a.setDuration(300);
		tabHost.startAnimation(a);

		visible = false;
	}

	private class OnTapListener extends GestureDetector.SimpleOnGestureListener
	{
		@Override
		public boolean onSingleTapConfirmed(MotionEvent motionEvent)
		{
			toggleVisibility();

			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
		{
			if(!scaling)
			{
				float scale = getImageScale();

				ZoomableRelativeLayout zoomableRelativeLayout = (ZoomableRelativeLayout) ImageFullScreenActivity.this.findViewById(R.id.imageWrapper);

				zoomableRelativeLayout.move(distanceX*scale,distanceY*scale);
			}

			return false;
		}

		int doubleTapState = 0;

		@Override
		public boolean onDoubleTap(MotionEvent motionEvent)
		{
			ZoomableRelativeLayout zoomableRelativeLayout= (ZoomableRelativeLayout) ImageFullScreenActivity.this.findViewById(R.id.imageWrapper);

			if(doubleTapState == 0)
			{
				zoomableRelativeLayout.smoothScaleCenter(getImageScale());
			}
			else
			{
				zoomableRelativeLayout.reset();
			}

			doubleTapState = (doubleTapState+1)%2;

			return true;
		}
	}

	private class OnPinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
	{
		float currentSpan;
		float startFocusX;
		float startFocusY;

		float scale;

		public boolean onScaleBegin(ScaleGestureDetector detector)
		{
			currentSpan = detector.getCurrentSpan();
			startFocusX = detector.getFocusX();
			startFocusY = detector.getFocusY();

			scale = getImageScale();

			return true;
		}

		public boolean onScale(ScaleGestureDetector detector)
		{
			scaling = true;

			ZoomableRelativeLayout zoomableRelativeLayout= (ZoomableRelativeLayout) ImageFullScreenActivity.this.findViewById(R.id.imageWrapper);

			zoomableRelativeLayout.relativeScale(detector.getCurrentSpan() / currentSpan, startFocusX, startFocusY);

			currentSpan = detector.getCurrentSpan();

			return true;
		}

		public void onScaleEnd(ScaleGestureDetector detector)
		{
			ZoomableRelativeLayout zoomableRelativeLayout= (ZoomableRelativeLayout) ImageFullScreenActivity.this.findViewById(R.id.imageWrapper);

			zoomableRelativeLayout.release();
		}
	}
}
