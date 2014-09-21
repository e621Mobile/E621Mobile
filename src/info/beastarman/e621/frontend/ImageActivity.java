package info.beastarman.e621.frontend;

import info.beastarman.e621.R;
import info.beastarman.e621.api.DText;
import info.beastarman.e621.api.DTextObject;
import info.beastarman.e621.api.E621Comment;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.api.E621Tag;
import info.beastarman.e621.api.E621Vote;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.GIFViewHandler;
import info.beastarman.e621.middleware.ImageLoadRunnable;
import info.beastarman.e621.middleware.ImageNavigator;
import info.beastarman.e621.middleware.ImageViewHandler;
import info.beastarman.e621.middleware.NowhereToGoImageNavigator;
import info.beastarman.e621.views.GIFView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ImageActivity extends BaseActivity implements OnClickListener
{
	private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;
	
	public static String NAVIGATOR = "navigator";
	public static String INTENT = "intent";
	
	public ImageNavigator image; 
	public Intent intent;
	
	E621Image e621Image = null;
	
	EventManager event = new EventManager()
	{
		@Override
		public void onTrigger(final Object obj)
		{
			final ImageButton button = (ImageButton)findViewById(R.id.downloadButton);
			
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
					        	delete(v);
					        }
					    });
					}
					else if(obj == E621Middleware.DownloadStatus.DOWNLOADING)
					{
						button.setImageResource(android.R.drawable.stat_sys_download);
						
						button.setOnClickListener(new View.OnClickListener() {
					        @Override
					        public void onClick(View v) {
					        	delete(v);
					        }
					    });
					}
					else if(obj == E621Middleware.DownloadStatus.DELETED)
					{
						button.setImageResource(android.R.drawable.ic_menu_save);
						
						button.setOnClickListener(new View.OnClickListener() {
					        @Override
					        public void onClick(View v) {
					        	save(v);
					        }
					    });
					}
					else if(obj == E621Middleware.DownloadStatus.DELETING)
					{
						button.setImageResource(R.drawable.progress_indicator);
						
						button.setOnClickListener(new View.OnClickListener() {
					        @Override
					        public void onClick(View v) {
					        	save(v);
					        }
					    });
					}
				}
			});
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image);
		
		image = (ImageNavigator) getIntent().getSerializableExtra(NAVIGATOR);
		
		setTitle("#" + image.getId());
		
		intent = (Intent) getIntent().getParcelableExtra(INTENT);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		retrieveImage();
		
		gestureDetector = new GestureDetector(this, new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
	}
	
	private void retrieveImage()
	{
		final Handler handler = new ImageHandler(this);
		
		new Thread(new Runnable() {
	        public void run() {
	        	Message msg = handler.obtainMessage();
	        	try {
					msg.obj = e621.post__show(image.getId());
				} catch (IOException e) {
					msg.obj = null;
				}
	        	handler.sendMessage(msg);
	        }
	    }).start();
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		
		if(e621Image != null)
		{
			update_result();
		}
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
		
		e621.unbindDownloadState(image.getId(),event);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                open_settings();
                return true;
            case R.id.get_full_size:
            	force_full_size();
            	return true;
            case android.R.id.home:
            	if(goUp())
            	{
            		return true;
            	}
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	public void force_full_size()
	{
		ImageView imageWrapper = (ImageView) findViewById(R.id.imageWrapper);
		View progressBarLoader = findViewById(R.id.progressBarLoader);
		
		//imageWrapper.setBackgroundResource(0);
		progressBarLoader.setVisibility(View.VISIBLE);
		
		((View)imageWrapper.getParent()).invalidate();
		
		ImageViewHandler handler = new ImageViewHandler(
			imageWrapper,
			progressBarLoader);
		
		new Thread(new ImageLoadRunnable(handler,e621Image,e621,E621Image.FULL)).start();
	}
    
    public void open_settings()
    {
    	Intent intent;
    	intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
    }
    
    public void reload(View v)
    {
    	findViewById(R.id.progressBar1).setVisibility(View.VISIBLE);
    	findViewById(R.id.textView1).setVisibility(View.GONE);
		findViewById(R.id.reload_button).setVisibility(View.GONE);
		
		retrieveImage();
    }
	
	public void update_result()
	{
		if(e621Image == null)
		{
			findViewById(R.id.progressBar1).setVisibility(View.GONE);
			findViewById(R.id.textView1).setVisibility(View.VISIBLE);
			findViewById(R.id.reload_button).setVisibility(View.VISIBLE);
			
			return;
		}
		
		View mainView = getLayoutInflater().inflate(R.layout.activity_image_loaded, null);
		
		mainView.setOnClickListener(ImageActivity.this); 
		mainView.setOnTouchListener(gestureListener);
		
		mainView.post(new Runnable() 
	    {
	        @Override
	        public void run() 
	        {
	        	e621.bindDownloadState(image.getId(),event);
	        	
	        	retrieveVote();
	        	retrieveFav();
	        	retrieveComments();
	        	
	        	updateRelated();
	        	
	        	ImageView imgView = (ImageView)findViewById(R.id.imageWrapper);
	        	
	        	View v = findViewById(R.id.content_wrapper);
	        	
	        	RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(new RelativeLayout.LayoutParams(
	        			v.getWidth(),
	        			(int) (v.getWidth() * (((double)e621Image.height) / e621Image.width))));
				
	        	if(
	        			e621Image.file_ext.equals("jpg") ||
	        			e621Image.file_ext.equals("png") ||
	        			(e621Image.file_ext.equals("gif") && !e621.playGifs())
	        		)
	        	{
		        	imgView.setLayoutParams(lp);
		        	
		    		ImageViewHandler handler = new ImageViewHandler(
		    			imgView,
		    			findViewById(R.id.progressBarLoader));
		    		
		    		new Thread(new ImageLoadRunnable(handler,e621Image,e621,e621.getFileDownloadSize())).start();
	        	}
	        	else if(e621Image.file_ext.equals("gif"))
	        	{
	        		ViewGroup g = (ViewGroup) imgView.getParent();
	        		
	        		int index = g.indexOfChild(imgView);
	        		
	        		g.removeViewAt(index);
	        		
	        		GIFView gifView = new GIFView(getApplicationContext());
	        		gifView.setLayoutParams(lp);
	        		gifView.setPadding(0, dpToPx(24), 0, 0);
	        		
	        		g.addView(gifView,index);
	        		
	        		GIFViewHandler handler = new GIFViewHandler(
		    			gifView,
		    			findViewById(R.id.progressBarLoader));
		    		
		    		new Thread(new ImageLoadRunnable(handler,e621Image,e621,e621.getFileDownloadSize())).start();
	        	}
	        	else
	        	{
	        		ViewGroup g = (ViewGroup) imgView.getParent();
	        		
	        		int index = g.indexOfChild(imgView);
	        		
	        		g.removeViewAt(index);
	        		
	        		LinearLayout rel = new LinearLayout(getApplicationContext());
	        		rel.setOrientation(LinearLayout.VERTICAL);
	        		rel.setLayoutParams(new ViewGroup.LayoutParams(
	        				ViewGroup.LayoutParams.MATCH_PARENT,
	        				ViewGroup.LayoutParams.WRAP_CONTENT));
	        		
	        		ImageView error_image = new ImageView(getApplicationContext());
	        		error_image.setBackgroundResource(android.R.drawable.ic_menu_report_image);
	        		LinearLayout.LayoutParams rel_params = new LinearLayout.LayoutParams(
	        				ViewGroup.LayoutParams.WRAP_CONTENT,
	        				ViewGroup.LayoutParams.WRAP_CONTENT);
	        		rel_params.gravity = Gravity.CENTER_HORIZONTAL;
	        		error_image.setLayoutParams(rel_params);
	        		
	        		rel.addView(error_image);
	        		
	        		TextView text = new TextView(getApplicationContext());
	        		text.setText("File not supported. Click here to try opening it with another app.");
	        		text.setTextColor(getResources().getColor(R.color.white));
	        		text.setGravity(Gravity.CENTER_HORIZONTAL);
	        		
	        		rel.addView(text);
	        		
	        		g.addView(rel);
	        		
	        		rel.setOnClickListener(new OnClickListener()
	        		{
	        			@Override
						public void onClick(View arg0)
	        			{
							Intent i = new Intent();
							i.setAction(Intent.ACTION_VIEW);
							i.setData(Uri.parse(e621Image.file_url));
							startActivity(i);
						}
	        		});
	        		
	        		findViewById(R.id.progressBarLoader).setVisibility(View.GONE);
	        	}
	        	
	        	new Thread(new Runnable()
	        	{
	        		public void run()
	        		{
	        			int i=0;
	        			
	        			String[] tag_names = new String[e621Image.tags.size()];
	        			
	        			for(i=0; i<e621Image.tags.size(); i++)
	        			{
	        				tag_names[i] = e621Image.tags.get(i).getTag();
	        			}
	        			
	        			e621Image.tags = e621.getTags(tag_names);
	    	        	
	    	        	runOnUiThread(new Runnable()
	    	        	{
	    	        		public void run()
	    	        		{
	    	        			fillTags(e621Image.tags);
	    	        		}
	    	        	});
	    	        	
	    	        	String artists = "";
	    	    		
	    	    		for(E621Tag t : e621Image.tags)
	    	    		{
	    	    			if(t.type == E621Tag.ARTIST)
	    	    			{
	    	    				if(artists.length() == 0)
	    	    				{
	    	    					artists += ": " + t.getTag();
	    	    				}
	    	    				else
	    	    				{
	    	    					artists += ", " + t.getTag();
	    	    				}
	    	    			}
	    	    		}
	    	    		
	    	    		final String new_title = "#" + e621Image.id + artists;
	    	    		
	    	    		runOnUiThread(new Runnable()
	    	        	{
	    	        		public void run()
	    	        		{
	    	        			setTitle(new_title);
	    	        			
	    	        			final FrameLayout tagFrame = (FrameLayout) findViewById(R.id.tagFrame);
	    	    	        	
	    	    	        	tagFrame.post(new Runnable()
	    	    	        	{
	    	    	        		@Override
	    	    					public void run()
	    	    	        		{
	    	    	        			View tagsToggle = findViewById(R.id.tagsToggle);
	    	    	        			tagsToggle.setOnClickListener(new OnClickListener()
	    	    	    	        	{
	    	    	    	        		@Override
	    	    	    					public void onClick(View arg0)
	    	    	    	        		{
	    	    	    	        			toogleTags();
	    	    	    					}
	    	    	    	        	});
	    	    					}
	    	    	        	});
	    	    	        	
	    	    	        	TextView tags_label = (TextView) findViewById(R.id.tags);
	    	    	        	tags_label.setText(R.string.tags_dropdown);
	    	        		}
	    	        	});
	        		}
	        	}).start();
	        }
	    });
		
		setContentView(mainView);
	}
	
	int commentsNextPage = 0;
	
	public void retrieveComments()
	{
		loadComments();
	}
	
	private Semaphore loadSemaphore = new Semaphore(1);
	
	public void loadComments()
	{
		if(!loadSemaphore.tryAcquire())
		{
			return;
		}
		
		new Thread(new Runnable()
		{
			public void run()
			{
				ArrayList<E621Comment> comments = e621.comment__index(Integer.valueOf(e621Image.id),commentsNextPage++);
				
				if(comments!=null && comments.size() > 0)
				{
					appendComments(comments);
				}
				else
				{
					runOnUiThread(new Runnable()
					{
						public void run()
						{
							findViewById(R.id.loadMore).setVisibility(View.GONE);
						}
					});
				}
				
				loadSemaphore.release();
			}
		}).start();
	}
	
	public void loadMoreComments(View v)
	{
		loadComments();
	}
	
	public void appendComments(List<E621Comment> comments)
	{
		final ViewGroup comments_container = (ViewGroup) findViewById(R.id.commentsLayout);
		final ArrayList<View> views = new ArrayList<View>();
		
		for(E621Comment cmt : comments)
		{
			View v = getLayoutInflater().inflate(R.layout.comment_area, null, false);
			
			TextView username = (TextView) v.findViewById(R.id.username);
			username.setText(cmt.creator);
			
			TextView time = (TextView) v.findViewById(R.id.time);
			time.setText(formatCommentTime(cmt.created_at));
			
			ViewGroup group = (ViewGroup) v.findViewById(R.id.dtext);
			group.addView(getDTextView(cmt.body));
			
			views.add(v);
		}
		
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				for(View v : views)
				{
					comments_container.addView(v);
				}
			}
		});
	}
	
	public String formatCommentTime(Date time)
	{
		Date now = new Date();
		
		long minutes = TimeUnit.MINUTES.convert(now.getTime() - time.getTime(), TimeUnit.MILLISECONDS);
		
		if(minutes == 0)
		{
			return "Less than a minute ago";
		}
		else if(minutes < 60)
		{
			return minutes + " minutes ago";
		}
		else if(TimeUnit.HOURS.convert(minutes, TimeUnit.MINUTES) < 24)
		{
			return TimeUnit.HOURS.convert(minutes, TimeUnit.MINUTES) + " hours ago";
		}
		else if(TimeUnit.DAYS.convert(minutes, TimeUnit.MINUTES) < 30)
		{
			return TimeUnit.DAYS.convert(minutes, TimeUnit.MINUTES) + " days ago";
		}
		else if(TimeUnit.DAYS.convert(minutes, TimeUnit.MINUTES) < 365)
		{
			return (int)Math.floor(12F*TimeUnit.DAYS.convert(minutes, TimeUnit.MINUTES)/365F) + " months ago";
		}
		else
		{
			return (int)Math.floor(TimeUnit.DAYS.convert(minutes, TimeUnit.MINUTES)/365F) + " years ago";
		}
	}
	
	public View getDTextView(DText text)
	{
		LinearLayout container = new LinearLayout(getApplicationContext());
		container.setOrientation(LinearLayout.VERTICAL);
		
		for(DTextObject obj : text)
		{
			TextView t = new TextView(getApplicationContext());
			t.setText(obj.raw());
			container.addView(t);
		}
		
		return container;
	}
	
	public void retrieveFav()
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
								
								favButton.setImageResource(android.R.drawable.star_big_on);
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
								
								favButton.setImageResource(android.R.drawable.star_big_off);
							}
	        			});
	        		}
	        	}
	        }
	    }).start();
	}
	
	public void fillParentView()
	{
		TextView text = (TextView) findViewById(R.id.parentTextView);
		
		text.setText(String.format(text.getText().toString(),e621Image.parent_id));
	}
	
	public void goToParent(View v)
	{
		if(e621Image.parent_id == null) return;
		
		Intent i = new Intent(this,ImageActivity.class);
		i.putExtra(ImageActivity.NAVIGATOR, new NowhereToGoImageNavigator(Integer.parseInt(e621Image.parent_id)));
		i.putExtra(ImageActivity.INTENT,intent);
		startActivity(i);
	}
	
	public void goToChildren(View v)
	{
		Intent i = new Intent(this,SearchActivity.class);
		i.putExtra(SearchActivity.SEARCH, "parent:" + e621Image.id);
		startActivity(i);
	}
	
	public void updateRelated()
	{
		boolean hide = true;
		
		if(e621Image.parent_id != null)
		{
			hide = false;
			
			fillParentView();
		}
		else
		{
			findViewById(R.id.parentWrapper).setVisibility(View.GONE);
		}
		
		if(e621Image.has_children)
		{
			hide = false;
		}
		else
		{
			findViewById(R.id.childrenWrapper).setVisibility(View.GONE);
		}
		
		if(hide)
		{
			findViewById(R.id.relatedWrapper).setVisibility(View.GONE);
		}
	}
	
	public static final int NO_VOTE = 0;
	public static final int VOTE_UP = 1;
	public static final int VOTE_DOWN= 2;
	
	public Integer vote = null;
	
	public void retrieveVote()
	{
		if(!e621.isLoggedIn() || vote!=null)
		{
			updateScore(e621Image.score);
		}
		
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				E621Search s = null;
				
				try {
					s = e621.post__index("id:" + e621Image.id + " voted:" + e621.getLoggedUser(), 0, 1);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if(s!=null)
				{
					if(s.images.size() > 0)
					{
						try {
							s = e621.post__index("id:" + e621Image.id + " votedup:" + e621.getLoggedUser(), 0, 1);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						if(s!=null)
						{
							if(s.images.size() > 0)
							{
								vote = VOTE_UP;
							}
							else
							{
								vote = VOTE_DOWN;
							}
						}
					}
					else
					{
						vote = NO_VOTE;
					}
				}
				
				if(vote != null)
				{
					final TextView score = (TextView) findViewById(R.id.score);
					
					runOnUiThread(new Runnable()
					{
						public void run()
						{
							score.setText(String.valueOf(e621Image.score));
							
							if(vote.equals(VOTE_UP))
							{
								score.setTextColor(getResources().getColor(R.color.green));
							}
							else if(vote.equals(VOTE_DOWN))
							{
								score.setTextColor(getResources().getColor(R.color.red));
							}
						}
					});
				}
			}
		}).start();
	}
	
	public void updateScore(int score)
	{
		TextView scoreView = (TextView) findViewById(R.id.score);
		scoreView.setText(String.valueOf(score));
		
		if(vote == null) return;
		
		switch(vote)
		{
			case NO_VOTE:
				scoreView.setTextColor(getResources().getColor(R.color.white));
				break;
			case VOTE_UP:
				scoreView.setTextColor(getResources().getColor(R.color.green));
				break;
			case VOTE_DOWN:
				scoreView.setTextColor(getResources().getColor(R.color.red));
				break;
			default:
				break;
		}
	}
	
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
		
		new Thread(new Runnable()
		{
			public void run()
			{
				final E621Vote v = e621.post__vote(e621Image.id, true);
				
				if(v != null && v.success)
				{
					if(vote.equals(VOTE_UP))
					{
						vote = NO_VOTE;
					}
					else
					{
						vote = VOTE_UP;
					}
					
					runOnUiThread(new Runnable()
					{
						public void run()
						{
							updateScore(v.score);
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
		
		new Thread(new Runnable()
		{
			public void run()
			{
				final E621Vote v = e621.post__vote(e621Image.id, false);
				
				if(v != null && v.success)
				{
					if(vote.equals(VOTE_DOWN))
					{
						vote = NO_VOTE;
					}
					else
					{
						vote = VOTE_DOWN;
					}
					
					runOnUiThread(new Runnable()
					{
						public void run()
						{
							updateScore(v.score);
						}
					});
				}
			}
		}).start();
	}
	
	public boolean tags_hidden = true;
	
	public void toogleTags()
	{
		if(tags_hidden)
	    {
	    	show_tags();
	    }
	    else
	    {
	    	hide_tags();
	    }
	}
	
	public void show_tags()
	{
		final FrameLayout tagFrame = (FrameLayout) findViewById(R.id.tagFrame);
		tagFrame.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
	    final int targetHeight = tagFrame.getMeasuredHeight();
	    
	    Animation a = new Animation()
		{
		    @Override
		    protected void applyTransformation(float interpolatedTime, Transformation t)
		    {
		    	ViewGroup.LayoutParams drawerParams = (ViewGroup.LayoutParams) tagFrame.getLayoutParams();
		    	
		        drawerParams.height = (int) (interpolatedTime * targetHeight);
		        
		        tagFrame.setLayoutParams(drawerParams);
		    }
		};

		a.setDuration(300);
		tagFrame.startAnimation(a);
		((View)tagFrame.getParent()).invalidate();
		
		tags_hidden = false;
	}
	
	public void hide_tags()
	{
		final FrameLayout tagFrame = (FrameLayout) findViewById(R.id.tagFrame);
		tagFrame.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
	    final int targetHeight = tagFrame.getMeasuredHeight();
	    
	    Animation a = new Animation()
		{
		    @Override
		    protected void applyTransformation(float interpolatedTime, Transformation t)
		    {
		    	interpolatedTime = 1f - interpolatedTime;
		    	
		    	ViewGroup.LayoutParams drawerParams = (ViewGroup.LayoutParams) tagFrame.getLayoutParams();
		    	
		        drawerParams.height = (int) (interpolatedTime * targetHeight);
		        tagFrame.setLayoutParams(drawerParams);
		    }
		};

		a.setDuration(300);
		tagFrame.startAnimation(a);
		
		tags_hidden = true;
	}
	
	public void fillTags(ArrayList<E621Tag> tags)
	{
		SparseArray<ArrayList<E621Tag>> catTags = new SparseArray<ArrayList<E621Tag>>();
		
		for(E621Tag tag : tags)
		{
			Integer type = (tag.type == null?E621Tag.GENERAL:tag.type);
			
			ArrayList<E621Tag> cur_tags = catTags.get(type, new ArrayList<E621Tag>());
			
			cur_tags.add(tag);
			
			catTags.put(type, cur_tags);
		}
		
		fillTags(catTags);
	}
	
	public void fillTags(SparseArray<ArrayList<E621Tag>> catTags)
	{
		LinearLayout tagList = (LinearLayout) findViewById(R.id.tagLayout);
		
		Integer[] types = new Integer[]{
				E621Tag.ARTIST,
				E621Tag.CHARACTER,
				E621Tag.COPYRIGHT,
				E621Tag.SPECIES,
				E621Tag.GENERAL,
			};
		
		for(Integer type : types)
		{
			ArrayList<E621Tag> tags = catTags.get(type);
			if(tags != null && tags.size()>0)
			{
				ArrayList<TextView> views = createTagViews(type,tags);
				
				for(TextView view : views)
				{
					tagList.addView(view);
				}
			}
		}
	}
	
	public ArrayList<TextView> createTagViews(Integer type, ArrayList<E621Tag> tags)
	{
		ArrayList<TextView> views = createTagViews(tags);
		
		TextView cat = new TextView(getApplicationContext());
		cat.setTypeface(null, Typeface.BOLD);
		cat.setTextColor(getResources().getColor(R.color.white));
		
		if(type == E621Tag.ARTIST)
		{
			cat.setText("Artist");
			
			for(TextView view : views)
			{
				view.setTextColor(getResources().getColor(R.color.yellow));
			}
		}
		else if(type == E621Tag.CHARACTER)
		{
			cat.setText("Character");
			
			for(TextView view : views)
			{
				view.setTextColor(getResources().getColor(R.color.green));
			}
		}
		else if(type == E621Tag.COPYRIGHT)
		{
			cat.setText("Copyright");
			
			for(TextView view : views)
			{
				view.setTextColor(getResources().getColor(R.color.magenta));
			}
		}
		else if(type == E621Tag.SPECIES)
		{
			cat.setText("Species");
			
			for(TextView view : views)
			{
				view.setTextColor(getResources().getColor(R.color.red));
			}
		}
		else
		{
			cat.setText("General");
		}
		
		views.add(0, cat);
		
		return views;
	}
	
	public ArrayList<TextView> createTagViews(ArrayList<E621Tag> tags)
	{
		ArrayList<TextView> views = new ArrayList<TextView>();
		
		for(final E621Tag tag : tags)
		{
			TextView view = new TextView(getApplicationContext());

			view.setText(tag.getTag());
			view.setPadding(dpToPx(5), dpToPx(2), 0, dpToPx(2));
			
			view.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View arg0) {
					EditText input = (EditText) findViewById(R.id.searchInput);
					
					String text = " " + input.getText().toString() + " ";
					
					if(text.contains(" " + tag.getTag() + " "))
					{
						text = text.replace(" " + tag.getTag() + " ", " ");
					}
					else
					{
						text += tag.getTag() + " ";
					}
					
					input.setText(text.substring(1, text.length()-1));
				}
			});
			
			views.add(view);
		}
		
		return views;
	}
	
	public void search(View view)
    {
    	EditText editText = (EditText) findViewById(R.id.searchInput);
    	String search = editText.getText().toString().trim();
    	
    	if(search.length() > 0)
    	{
    		Intent intent = new Intent(this, SearchActivity.class);
    		intent.putExtra(SearchActivity.SEARCH,search);
    		startActivity(intent);
    	}
    }
	
	public void delete(View view)
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				e621.deleteImage(e621Image);
			}
		}).start();
	}
	
	public void save(View view)
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				e621.saveImage(e621Image);
			}
		}).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		if(e621.getFileDownloadSize() == E621Image.FULL)
		{
			getMenuInflater().inflate(R.menu.image_full, menu);
		}
		else
		{
			getMenuInflater().inflate(R.menu.image, menu);
		}
		return true;
	}

	private static class ImageHandler extends Handler
	{
		ImageActivity activity;
		
		public ImageHandler(ImageActivity activity)
		{
			this.activity = activity;
		}
		
		@Override
		public void handleMessage(Message msg)
		{
			E621Image result = (E621Image)msg.obj;
			
			activity.e621Image = result;
			activity.update_result();
		}
	}
	
	class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
                {
                    next();
                }
                else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
                {
                    prev();
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }

            @Override
        public boolean onDown(MotionEvent e) {
              return true;
        }
    }
	
	public void prev()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run() {
				ImageNavigator nav = image.prev();
				
				if(nav != null)
				{
					final Intent new_intent = new Intent(ImageActivity.this, ImageActivity.class);
					new_intent.putExtra(ImageActivity.NAVIGATOR, nav);
					new_intent.putExtra(ImageActivity.INTENT,intent);
					runOnUiThread(new Runnable()
					{
						@Override
						public void run() {
							startActivity(new_intent);
						}
					});
				}
			}
		}).start();
	}
	
	public void next()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run() {
				ImageNavigator nav = image.next();
				
				if(nav != null)
				{
					final Intent new_intent = new Intent(ImageActivity.this, ImageActivity.class);
					new_intent.putExtra(ImageActivity.NAVIGATOR, nav);
					new_intent.putExtra(ImageActivity.INTENT,intent);
					runOnUiThread(new Runnable()
					{
						@Override
						public void run() {
							startActivity(new_intent);
						}
					});
				}
			}
		}).start();
	}

	@Override
	public void onClick(View v)
	{
	};
	
	public boolean goUp()
	{
		if(intent == null)
		{
			intent = new Intent(ImageActivity.this, MainActivity.class);
		}
		
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		
		return true;
	}
	
	Boolean is_faved = null;
	
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
				final Boolean ret = e621.post_favorite(e621Image.id, !is_faved);
				
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
									favButton.setImageResource(android.R.drawable.star_big_on);
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
									favButton.setImageResource(android.R.drawable.star_big_off);
								}
							});
						}
					}
				}
			}
		}).start();
	}
}
