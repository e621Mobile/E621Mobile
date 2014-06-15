package info.beastarman.e621.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Tag;
import info.beastarman.e621.middleware.GIFViewHandler;
import info.beastarman.e621.middleware.ImageLoadRunnable;
import info.beastarman.e621.middleware.ImageNavigator;
import info.beastarman.e621.middleware.ImageViewHandler;
import info.beastarman.e621.views.GIFView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image);
		
		image = (ImageNavigator) getIntent().getSerializableExtra(NAVIGATOR);
		
		intent = (Intent) getIntent().getParcelableExtra(INTENT);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
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
		
		gestureDetector = new GestureDetector(this, new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
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
	protected void onPause()
	{
		//overridePendingTransition(R.anim.hold, R.anim.pull_out_to_left);
        super.onPause();
    }
	
	@Override
	public void onResume()
	{
		//this.overridePendingTransition(R.anim.pull_in_from_left, R.anim.hold);
		super.onResume();
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                open_settings();
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
    
    public void open_settings()
    {
    	Intent intent;
    	intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
    }
	
	public void update_result()
	{
		View mainView = getLayoutInflater().inflate(R.layout.activity_image_loaded, null);
		
		mainView.setOnClickListener(ImageActivity.this); 
		mainView.setOnTouchListener(gestureListener);
		
		mainView.post(new Runnable() 
	    {
	        @Override
	        public void run() 
	        {
	        	if(e621.isSaved(e621Image))
	        	{
	        		ImageButton button = (ImageButton)findViewById(R.id.downloadButton);
	        		button.setImageResource(android.R.drawable.ic_menu_delete);
	        	}
	        	
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
	        		
	        		g.addView(gifView,index);
	        		
	        		GIFViewHandler handler = new GIFViewHandler(
		    			gifView,
		    			findViewById(R.id.progressBarLoader));
		    		
		    		new Thread(new ImageLoadRunnable(handler,e621Image,e621,e621.getFileDownloadSize())).start();
	        	}
	        	
	        	fillTags(e621Image.tags);
	        }
	    });
		
		setContentView(mainView);
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
	
	public void save_delete(View view)
	{
		if(e621.isSaved(e621Image))
		{
			delete(view);
		}
		else
		{
			save(view);
		}
	}
	
	public void delete(View view)
	{
		final ImageButton button = (ImageButton)view;
		
		e621.deleteImage(e621Image);
		
		button.setImageResource(android.R.drawable.ic_menu_save);
		
		button.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	save(v);
	        }
	    });
	}
	
	public void save(View view)
	{
		final ImageButton button = (ImageButton)view;
		button.setImageResource(android.R.drawable.stat_sys_download);
		
		e621.saveImageAsync(e621Image, this, new Runnable()
		{
			@Override
			public void run() {
				
				runOnUiThread(new Runnable()
				{
					@Override
					public void run() {
						button.setImageResource(android.R.drawable.ic_menu_delete);
						
						button.setOnClickListener(new View.OnClickListener() {
					        @Override
					        public void onClick(View v) {
					        	delete(v);
					        }
					    });
					}
				});
			}
		}, new Runnable()
		{
			@Override
			public void run() {
				
				runOnUiThread(new Runnable()
				{
					@Override
					public void run() {
						button.setImageResource(android.R.drawable.ic_menu_save);
						
						button.setOnClickListener(new View.OnClickListener() {
					        @Override
					        public void onClick(View v) {
					        	save(v);
					        }
					    });
					}
				});
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.image, menu);
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
}
