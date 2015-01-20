package info.beastarman.e621.frontend;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Comment;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.api.E621Tag;
import info.beastarman.e621.api.E621Vote;
import info.beastarman.e621.api.dtext.DText;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.ImageNavigator;
import info.beastarman.e621.middleware.ImageNavilagorLazyRelative;
import info.beastarman.e621.middleware.NowhereToGoImageNavigator;
import info.beastarman.e621.middleware.OnlineImageNavigator;
import info.beastarman.e621.views.DTextView;

public class ImageFullScreenActivity extends BaseFragmentActivity
{
	public static String NAVIGATOR = "navigator";
	public static String INTENT = "intent";

	public ImageNavigator image;
	public Intent intent;

	E621Image img = null;
	float TABS_HEIGHT = 0.7f;

	DownloadEventManager downloadEventManager = new DownloadEventManager();

	private ViewPager viewPager;
	private PagerAdapter mPagerAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_full_screen_activity);

		if (Intent.ACTION_VIEW.equals(getIntent().getAction()))
		{
			final List<String> segments = getIntent().getData().getPathSegments();

			if (segments.size() > 2)
			{
				try
				{
					image = new NowhereToGoImageNavigator(Integer.parseInt(segments.get(2)));
				}
				catch(NumberFormatException e)
				{
					Intent i = new Intent(this,MainActivity.class);
					startActivity(i);
				}
			}
		}
		else
		{
			image = (ImageNavigator) getIntent().getSerializableExtra(NAVIGATOR);
		}

		setTitle("#" + image.getId());

		intent = (Intent) getIntent().getParcelableExtra(INTENT);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.BackgroundColorTransparent)));

		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

		final TabHost tabHost = (TabHost) findViewById(R.id.tabHost);

		int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
		if (resourceId > 0)
		{
			int navigationHeight = getResources().getDimensionPixelSize(resourceId);

			findViewById(R.id.info).setPadding(0,0,0,navigationHeight);
			findViewById(R.id.tags).setPadding(0,0,0,navigationHeight);
			findViewById(R.id.comments).setPadding(0,0,0,navigationHeight);
		}

		tabHost.setup();
		tabHost.addTab(tabHost.newTabSpec("Info").setIndicator("Info").setContent(R.id.info));
		tabHost.addTab(tabHost.newTabSpec("Tags").setIndicator("Tags").setContent(R.id.tags));
		tabHost.addTab(tabHost.newTabSpec("Comments").setIndicator("Comments").setContent(R.id.comments));

		tabHost.setVisibility(View.GONE);

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

		viewPager = (ViewPager)findViewById(R.id.view_pager);
		mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
		viewPager.setAdapter(mPagerAdapter);
	}

	@Override
	protected void onStop()
	{
		final TableLayout tableLayout = (TableLayout) findViewById(R.id.imageViewTable);

		if(tableLayout != null)
		{
			tableLayout.removeAllViews();
		}

		e621.unbindDownloadState(img.id,downloadEventManager);

		super.onStop();
	}

	SearchView searchView = null;
	Menu mMenu = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.image_full_screen, menu);

		mMenu = menu;

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
				updateDownload();

				updateScore();

				updateDescription();

				updateStatistics();

				updateSources();

				updateParent();

				updateChildren();

				updateTags();

				updateComments();
			}
		});
	}

	public static final int NO_VOTE = 0;
	public static final int VOTE_UP = 1;
	public static final int VOTE_DOWN= 2;

	public Integer vote = null;

	public void voteUp(View view)
	{
		if(!e621.isLoggedIn())
		{
			Toast.makeText(getApplicationContext(), "Please log in at the home screen.", Toast.LENGTH_SHORT).show();

			return;
		}

		if(vote == null)
		{
			return;
		}

		final int vvote = vote;
		final int sscore = img.score;

		if(vote.equals(VOTE_UP))
		{
			vote = NO_VOTE;

			img.score--;
		}
		else
		{
			img.score++;

			if(vote == VOTE_DOWN)
			{
				img.score++;
			}

			vote = VOTE_UP;
		}

		updateScore();

		new Thread(new Runnable()
		{
			public void run()
			{
				final E621Vote v = e621.post__vote(img.id, true);

				if(v == null || !v.success)
				{
					vote = vvote;

					img.score = sscore;

					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							updateScore();
						}
					});
				}
			}
		}).start();
	}

	public void voteDown(View view)
	{
		if(!e621.isLoggedIn())
		{
			Toast.makeText(getApplicationContext(), "Please log in at the home screen.", Toast.LENGTH_SHORT).show();

			return;
		}

		if(vote == null)
		{
			return;
		}

		final int vvote = vote;
		final int sscore = img.score;

		if(vote.equals(VOTE_DOWN))
		{
			vote = NO_VOTE;

			img.score++;
		}
		else
		{
			img.score--;

			if(vote == VOTE_UP)
			{
				img.score--;
			}

			vote = VOTE_DOWN;
		}

		updateScore();

		new Thread(new Runnable()
		{
			public void run()
			{
				final E621Vote v = e621.post__vote(img.id, false);

				if(v == null || !v.success)
				{
					vote = vvote;

					img.score = sscore;

					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							updateScore();
						}
					});
				}
			}
		}).start();
	}

	public void updateVote()
	{
		if(vote == null) return;

		View thumbsUp = findViewById(R.id.thumbsUp);
		View thumbsDown = findViewById(R.id.thumbsDown);

		thumbsUp.setVisibility(View.VISIBLE);
		thumbsDown.setVisibility(View.VISIBLE);

		TextView score = (TextView)findViewById(R.id.scoreTextView);

		switch (vote)
		{
			case NO_VOTE:
				thumbsUp.setBackgroundResource(R.drawable.thumbs_up_disabled);
				thumbsDown.setBackgroundResource(R.drawable.thumbs_down_disabled);
				score.setTextColor(getResources().getColor(R.color.white));
				break;
			case VOTE_UP:
				thumbsUp.setBackgroundResource(R.drawable.thumbs_up);
				thumbsDown.setBackgroundResource(R.drawable.thumbs_down_disabled);
				score.setTextColor(getResources().getColor(R.color.green));
				break;
			default:
				thumbsUp.setBackgroundResource(R.drawable.thumbs_up_disabled);
				thumbsDown.setBackgroundResource(R.drawable.thumbs_down);
				score.setTextColor(getResources().getColor(R.color.red));
				break;
		}
	}

	public void updateScore()
	{
		TextView score = (TextView)findViewById(R.id.scoreTextView);
		score.setText(""+img.score);

		if(!e621.isLoggedIn())
		{
			return;
		}

		if(vote != null)
		{
			updateVote();
		}
		else
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					E621Search s = null;

					try
					{
						s = e621.post__index("id:" + img.id + " voted:" + e621.getLoggedUser(), 0, 1);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}

					if(s == null || s.count==0)
					{
						vote = NO_VOTE;
					}
					else
					{
						try
						{
							s = e621.post__index("id:" + img.id + " votedup:" + e621.getLoggedUser(), 0, 1);
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}

						if(s == null)
						{
							return;
						}
						else if(s.count==0)
						{
							vote = VOTE_DOWN;
						}
						else
						{
							vote = VOTE_UP;
						}
					}

					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							updateVote();
						}
					});
				}
			}).start();
		}
	}

	public void download(View v)
	{
		if(downloadEventManager.isDownloaded())
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					e621.deleteImage(img);
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
					e621.saveImage(img);
				}
			}).start();
		}
	}

	private void updateDownload()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				e621.bindDownloadState(img.id,downloadEventManager);
			}
		}).start();
	}

	public void postComment(View v)
	{
		EditText postComment = (EditText) findViewById(R.id.commentEditText);
		String s = postComment.getText().toString();

		if(!s.trim().isEmpty() && e621.isLoggedIn())
		{
			final E621Comment newComment = new E621Comment();

			newComment.creator = e621.getLoggedUser();
			newComment.body = s;
			newComment.created_at = new Date();

			View newView = getCommentView(newComment);

			LinearLayout l = (LinearLayout) findViewById(R.id.commentsLayout);

			if(e621.commentsSorting() == E621Middleware.DATE_DESC)
			{
				l.addView(newView,1);
			}
			else
			{
				l.addView(newView);

				l.post(new Runnable()
				{
					@Override
					public void run()
					{
						((ScrollView)findViewById(R.id.commentsScroll)).fullScroll(ScrollView.FOCUS_DOWN);
					}
				});
			}

			postComment.setText("");

			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					e621.comment__create(img.id, newComment.body);
				}
			}).start();
		}
	}

	ArrayList<E621Comment> comments = null;
	private synchronized ArrayList<E621Comment> getComments()
	{
		if(comments == null)
		{
			comments = e621.comment__index(img.id);

			if(e621.commentsSorting() == E621Middleware.DATE_ASC)
			{
				Collections.reverse(comments);
			}
			else if(e621.commentsSorting() == E621Middleware.SCORE)
			{
				Collections.sort(comments,new Comparator<E621Comment>()
				{
					@Override
					public int compare(E621Comment a, E621Comment b)
					{
						return b.score - a.score;
					}
				});
			}
		}

		return comments;
	}

	private View getCommentView(E621Comment c)
	{
		View v = getLayoutInflater().inflate(R.layout.image_full_screen_comment,null,false);

		((TextView)v.findViewById(R.id.username)).setText(c.creator);

		TextView score = (TextView)v.findViewById(R.id.score);
		if(c.score > 0)
		{
			score.setTextColor(getResources().getColor(R.color.green));
			score.setText("+" + c.score);
		}
		else if(c.score < 0)
		{
			score.setTextColor(getResources().getColor(R.color.red));
			score.setText("" + c.score);
		}
		else
		{
			score.setText("" + c.score);
		}

		((TextView)v.findViewById(R.id.created_at)).setText(DateUtils.getRelativeTimeSpanString(c.created_at.getTime(), new Date().getTime(), 0));

		((DTextView)v.findViewById(R.id.dtext)).setDText(c.getBodyAsDText());

		TextView respond = (TextView) v.findViewById(R.id.respond);
		respond.setMovementMethod(LinkMovementMethod.getInstance());
		Spannable span = new SpannableString(respond.getText().toString());
		span.setSpan(new respondClickableSpan(c),0,span.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		respond.setText(span, TextView.BufferType.SPANNABLE);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, dpToPx(8), 0, 0);
		v.setLayoutParams(params);

		return v;
	}

	private void updateComments()
	{
		if(findViewById(R.id.commentsLoading).getVisibility() == View.GONE) return;

		Button post = (Button)findViewById(R.id.postCommentButton);

		EditText postComment = (EditText) findViewById(R.id.commentEditText);
		postComment.addTextChangedListener(new PostCommentTextChangedListener(post));

		if(!e621.isLoggedIn())
		{
			findViewById(R.id.postCommentArea).setVisibility(View.GONE);
		}

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				final LinearLayout commentsLayout = (LinearLayout)findViewById(R.id.commentsLayout);

				final ArrayList<View> views = new ArrayList<View>();

				for(E621Comment c : getComments())
				{
					views.add(getCommentView(c));
				}

				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						for(View v : views)
						{
							commentsLayout.addView(v);
						}

						findViewById(R.id.commentsLoading).setVisibility(View.GONE);
					}
				});
			}
		}).start();
	}

	SparseArray<ArrayList<E621Tag>> catTags = null;
	private synchronized SparseArray<ArrayList<E621Tag>> prepareTags()
	{
		if(catTags == null)
		{
			catTags = new SparseArray<ArrayList<E621Tag>>();
			String[] stags = new String[img.tags.size()];

			for(int i=0; i<img.tags.size(); i++)
			{
				stags[i] = img.tags.get(i).getTag();
			}

			img.tags = e621.getTags(stags);

			for(E621Tag tag : img.tags)
			{
				ArrayList<E621Tag> ttags = catTags.get(tag.type);

				if(ttags == null)
				{
					ttags = new ArrayList<E621Tag>();
					catTags.put(tag.type, ttags);
				}

				ttags.add(tag);
			}

			for(int cat=0; cat < catTags.size(); cat++)
			{
				Collections.sort(catTags.valueAt(cat));
			}
		}

		return catTags;
	}

	private ArrayList<View> getTagViews(SparseArray<ArrayList<E621Tag>> catTags)
	{
		ArrayList<View> views = new ArrayList<View>();

		LinkedHashMap<Integer,Pair<String,Integer>> cats = new LinkedHashMap<Integer,Pair<String,Integer>>();

		cats.put(E621Tag.ARTIST, new Pair<String, Integer>("Artist",getResources().getColor(R.color.yellow)));
		cats.put(E621Tag.CHARACTER, new Pair<String, Integer>("Character",getResources().getColor(R.color.green)));
		cats.put(E621Tag.COPYRIGHT, new Pair<String, Integer>("Copyright",getResources().getColor(R.color.magenta)));
		cats.put(E621Tag.SPECIES, new Pair<String, Integer>("Species",getResources().getColor(R.color.red)));
		cats.put(E621Tag.GENERAL, new Pair<String, Integer>("General",-1));

		for(int cat : cats.keySet())
		{
			ArrayList<E621Tag> tags = catTags.get(cat);

			if(tags != null)
			{
				TextView catView = new TextView(this); // X3

				Spannable catSpan = new SpannableString(cats.get(cat).first);
				catSpan.setSpan(new StyleSpan(Typeface.BOLD),0,catSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				catSpan.setSpan(new ForegroundColorSpan(Color.WHITE),0,catSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

				catView.setText(catSpan);

				views.add(catView);

				for(E621Tag tag : tags)
				{
					TextView tagView = new TextView(this);

					Spannable tagSpan = new SpannableString(tag.getTag());

					if(cats.get(cat).second != -1)
					{
						tagSpan.setSpan(new ForegroundColorSpan(cats.get(cat).second),0,tagSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					}

					tagView.setPadding(dpToPx(16),0,0,0);

					tagView.setText(tagSpan);

					tagView.setOnClickListener(new OnTagClickListener(tag.getTag()));

					views.add(tagView);
				}
			}
		}

		return views;
	}

	private String getNewTitle(ArrayList<E621Tag> tags)
	{
		String title = "";

		if(tags == null)
		{
			return "#" + img.id;
		}
		else
		{
			for(E621Tag tag : tags)
			{
				if(title.length() > 0)
				{
					title += ", ";
				}

				title += tag.getTag();
			}

			return "#" + img.id + " " + title;
		}
	}

	private void updateTags()
	{
		if(findViewById(R.id.tagsLoading).getVisibility() == View.GONE)
		{
			return;
		}

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				SparseArray<ArrayList<E621Tag>> catTags = prepareTags();

				final LinearLayout tagsLayout = (LinearLayout)findViewById(R.id.tagsLayout);

				final ArrayList<View> views = getTagViews(catTags);

				final String newTitle = getNewTitle(catTags.get(E621Tag.ARTIST));

				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						findViewById(R.id.tagsLoading).setVisibility(View.GONE);

						setTitle(newTitle);

						for(View v : views)
						{
							tagsLayout.addView(v);
						}
					}
				});
			}
		}).start();
	}

	private String getSize()
	{
		double size = img.file_size;

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

		return new DecimalFormat("#.##").format(size) + " MB";
	}

	private void updateStatistics()
	{
		TextView rating = (TextView)findViewById(R.id.rating);

		if(img.rating.equals(E621Image.SAFE))
		{
			rating.setText(Html.fromHtml("<font color=#00FF00>Safe</font>"));
		}
		else if(img.rating.equals(E621Image.QUESTIONABLE))
		{
			rating.setText(Html.fromHtml("<font color=#FFFF00>Questionable</font>"));
		}
		else
		{
			rating.setText(Html.fromHtml("<font color=#FF0000>Explicit</font>"));
		}

		TextView size = (TextView)findViewById(R.id.size);
		size.setText(img.width + "x" + img.height + " (" + getSize() + ")");

		TextView uploader = (TextView)findViewById(R.id.uploader);
		uploader.setMovementMethod(LinkMovementMethod.getInstance());
		Spannable s = new SpannableString(img.author);
		s.setSpan(new ClickableSpan()
		{
			@Override
			public void onClick(View view)
			{
				Intent i = new Intent(ImageFullScreenActivity.this,SearchActivity.class);
				i.putExtra(SearchActivity.SEARCH,"user:" + img.author);
				startActivity(i);
			}
		},0,s.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		uploader.setText(s);

		TextView created_at = (TextView)findViewById(R.id.createdAt);
		created_at.setText(DateUtils.getRelativeTimeSpanString(img.created_at.getTime(), new Date().getTime(), 0));
	}

	private void updateSources()
	{
		if(img.sources.size() > 0)
		{
			LinearLayout sources = (LinearLayout)findViewById(R.id.sources);
			sources.removeAllViews();

			for(String source : img.sources)
			{
				TextView tv = new TextView(this);
				tv.setMovementMethod(LinkMovementMethod.getInstance());

				Spannable s = new SpannableString(source);
				s.setSpan(new URLSpan(source),0,source.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

				tv.setText(s);

				sources.addView(tv);
			}
		}
		else
		{
			(findViewById(R.id.sourcesLayout)).setVisibility(View.GONE);
		}
	}

	private void updateDescription()
	{
		final DTextView description = (DTextView)findViewById(R.id.description);

		if(!img.description.isEmpty())
		{
			description.addView(new ProgressBar(this));

			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					final DText d = img.getDescriptionAsDText();

					description.post(new Runnable()
					{
						@Override
						public void run()
						{
							description.setDText(d);
						}
					});
				}
			}).start();
		}
		else
		{
			(findViewById(R.id.descriptionLayout)).setVisibility(View.GONE);
		}
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
							if (ttags.length() != 0)
							{
								parentTags.setVisibility(View.VISIBLE);
								parentTags.setText(Html.fromHtml(ttags));
							}

							parentWrapper.post(new Runnable()
							{
								@Override
								public void run()
								{
									final int height;
									final int width;

									int scale = Math.max(1, Math.min(parent.preview_width / (getResources().getDisplayMetrics().widthPixels / 5), parent.preview_height / (getResources().getDisplayMetrics().widthPixels / 5)));

									width = parent.preview_width / scale;
									height = parent.preview_height / scale;

									new Thread(new Runnable()
									{
										@Override
										public void run()
										{
											InputStream in = e621.getImage(parent, E621Image.PREVIEW);

											if (in == null)
											{
												return;
											}

											final Bitmap bmp = e621.decodeFile(in, width, height);

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

							for (View v : views)
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
						final int height;
						final int width;

						int scale = Math.max(1, Math.min(child.preview_width / (getResources().getDisplayMetrics().widthPixels / 5), child.preview_height / (getResources().getDisplayMetrics().widthPixels / 5)));

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
					search = e621.post__index("fav:" + e621.getLoggedUser() + " id:" + image.getId(), 0, 1);
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

		final boolean f = (is_faved==null?false:is_faved);

		is_faved = !f;

		final ImageButton favButton = (ImageButton) findViewById(R.id.favButton);

		if(is_faved)
		{
			favButton.setBackgroundResource(R.drawable.fav_star_enabled_2);
		}
		else
		{
			favButton.setBackgroundResource(R.drawable.fav_star_disabled);
		}

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				final Boolean ret = e621.post_favorite(image.getId(), !f);

				if(ret == null || !ret)
				{
					is_faved = f;

					if(f)
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
		}).start();
	}

	private boolean visible = false;

	public void toggleVisibility()
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
			case R.id.action_settings:
				open_settings();
				return true;
			case android.R.id.home:
				goUp();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void open_settings()
	{
		Intent intent;
		intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
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

	private class OnTagClickListener implements View.OnClickListener
	{
		String tagName;

		public OnTagClickListener(String tagName)
		{
			this.tagName = tagName;
		}

		@Override
		public void onClick(View view)
		{
			if(ImageFullScreenActivity.this.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && mMenu != null)
			{
				MenuItem searchItem = mMenu.findItem(R.id.action_search);
				searchItem.expandActionView();
			}

			searchView.setIconified(false);

			String query = " " + searchView.getQuery().toString() + " ";

			if(query.contains(" " + tagName + " "))
			{
				query = query.replace(" " + tagName + " ", " ");
			}
			else
			{
				query = query + " " + tagName;
			}

			searchView.setQuery(query.trim(),false);
			searchView.clearFocus();
		}
	}

	private class respondClickableSpan extends ClickableSpan
	{
		E621Comment comment;

		public respondClickableSpan(E621Comment c)
		{
			comment = c;
		}

		@Override
		public void onClick(View view)
		{
			EditText postComment = (EditText) findViewById(R.id.commentEditText);
			postComment.append("[quote] " + comment.creator + " said:\n" + comment.body + "\n[/quote]\n\n");
		}
	}

	private class PostCommentTextChangedListener implements TextWatcher
	{
		Button button;

		public PostCommentTextChangedListener(Button button)
		{
			this.button = button;
		}

		@Override
		public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
		{
		}

		@Override
		public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
		{
		}

		@Override
		public void afterTextChanged(Editable editable)
		{
			button.setEnabled(!editable.toString().trim().isEmpty());
		}
	}

	private class DownloadEventManager extends EventManager
	{
		boolean downloaded = false;

		public boolean isDownloaded()
		{
			return downloaded;
		}

		@Override
		public void onTrigger(Object obj)
		{
			if(obj instanceof E621Middleware.DownloadStatus)
			{
				final ImageView iv = (ImageView)findViewById(R.id.saveButton);

				if(obj == E621Middleware.DownloadStatus.DOWNLOADED || obj == E621Middleware.DownloadStatus.DOWNLOADING)
				{
					downloaded = true;

					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							iv.setBackgroundResource(R.drawable.save_icon);
						}
					});
				}
				else
				{
					downloaded = false;

					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							iv.setBackgroundResource(R.drawable.save_icon_disabled);
						}
					});
				}
			}
		}
	}

	private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter
	{
		public ScreenSlidePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{
			return ImageFullScreenActivityStaticImageFragment.fromImageNavigator(new ImageNavilagorLazyRelative(image, position));
		}

		@Override
		public int getCount() {
			return image.getCount();
		}
	}

}
