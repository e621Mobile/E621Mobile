package info.beastarman.e621.frontend;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.middleware.E621DownloadedImage;
import info.beastarman.e621.middleware.ImageViewHandler;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.Transformation;

public class SlideMenuBaseActivity extends BaseActivity
{
	private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    
    public ArrayList<String> saved_searches;
	
	private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        RelativeLayout fullLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.activity_base, null);
        super.setContentView(fullLayout);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        gestureDetector = new GestureDetector(this, new MyGestureDetector());
        gestureListener = new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event)
            {
                return gestureDetector.onTouchEvent(event);
            }
        };
        
        fullLayout.setOnTouchListener(gestureListener);
        
        fullLayout.post(new Runnable()
        {
        	@Override
			public void run()
        	{
        		FrameLayout wrapper = (FrameLayout) findViewById(R.id.sidemenu_wrapper);
        		RelativeLayout.LayoutParams drawerParams = (RelativeLayout.LayoutParams) wrapper.getLayoutParams();
                drawerParams.width = 0;
                wrapper.setLayoutParams(drawerParams);
                
                saved_searches = e621.getAllSearches();
                LinearLayout saved_search_container = (LinearLayout) findViewById(R.id.savedSearchContainer);
                
                for(final String search : saved_searches)
                {
                	new Thread(new Runnable()
                	{
                		@Override
						public void run()
                		{
                			try {
                				E621Search ret = e621.continue_search(search, 2, 20);
                				Log.d("Msg",ret.images.toString() + " " + search);
                				Log.d("Msg",String.valueOf(ret.count) + " " + search);
                				Log.d("Msg",String.valueOf(ret.offset) + " " + search);
        					} catch (IOException e) {
        						e.printStackTrace();
        					}
						}
                	}).start();
                	
                	saved_search_container.addView(getSearchItemView(search));
                	saved_search_container.addView(getLayoutInflater().inflate(R.layout.hr, saved_search_container, false));
                }
                
                if(saved_searches.size() == 0)
                {
                	TextView continue_search_label = (TextView) findViewById(R.id.continue_search_label);
                	
                	continue_search_label.setTextColor(getResources().getColor(R.color.gray));
                }
			}
        });
    }
	
	private View getSearchItemView(final String search)
	{
		LinearLayout row = new LinearLayout(getApplicationContext());
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setPadding(dpToPx(20), 0, 0, 0);
		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		row.setLayoutParams(params);
		
		ImageView img = new ImageView(getApplicationContext());
		img.setBackgroundResource(android.R.drawable.ic_menu_gallery);
		params = new ViewGroup.LayoutParams(
				dpToPx(36),
				dpToPx(36));
		img.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
		img.setLayoutParams(params);
		
		final ImageViewHandler handler = new ImageViewHandler(img, null);
		
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				ArrayList<E621DownloadedImage> images = e621.localSearch(0, 1, search);
				
				if(images.size() > 0)
				{
					InputStream in = e621.getDownloadedImage(images.get(0).filename);
			    	Message msg = handler.obtainMessage();
			    	msg.obj = in;
			    	
			    	handler.sendMessage(msg);
				}
			}
		}).start();
		
		TextView text = new TextView(getApplicationContext());
		text.setText(search);
		text.setPadding(dpToPx(12), 0, 0, 0);
		text.setTextAppearance(getApplicationContext(), android.R.attr.textAppearanceSmall);
		text.setTextColor(getResources().getColor(R.color.white));
		params = new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
		text.setLayoutParams(params);
		text.setGravity(Gravity.CENTER_VERTICAL);
		
		row.addView(img);
		row.addView(text);
		
		return row;
	}
	
	@Override
	public void setContentView(final int layoutResID)
	{
		RelativeLayout view_container= (RelativeLayout) findViewById(R.id.view_container);
        getLayoutInflater().inflate(layoutResID, view_container, true);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
            	toggle_sidebar();
            	
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	boolean continue_is_open = false;
	
	public void toggleContinueSearch(View v)
	{
		toggleContinueSearch();
	}
	
	public void toggleContinueSearch()
	{
		if(saved_searches.size() == 0)
		{
			Toast.makeText(getApplicationContext(), "No saved searches yet. Add some on the options menu in any online search.", Toast.LENGTH_SHORT).show();
			
			return;
		}
		
		if(!continue_is_open)
		{
			open_continue();
		}
		else
		{
			close_continue();
		}
	}
	
	public void open_continue()
	{
		final FrameLayout wrapper = (FrameLayout) findViewById(R.id.savedSearchWrapper);
		final ImageView arrow = (ImageView) findViewById(R.id.continue_arrow);
		
		Animation a = new Animation()
		{
		    @Override
		    protected void applyTransformation(float interpolatedTime, Transformation t)
		    {
		    	ViewGroup.LayoutParams drawerParams = (ViewGroup.LayoutParams) wrapper.getLayoutParams();
		    	
		    	int height = dpToPx((36+1)*saved_searches.size());
		    	
		        drawerParams.height = (int) (interpolatedTime * height);
		        wrapper.setLayoutParams(drawerParams);
		        
		        arrow.setRotation(270f + (90f*interpolatedTime));
		    }
		};

		a.setDuration(300);
		wrapper.startAnimation(a);
		
		continue_is_open = true;
	}
	
	public void close_continue()
	{
		final FrameLayout wrapper = (FrameLayout) findViewById(R.id.savedSearchWrapper);
		final ImageView arrow = (ImageView) findViewById(R.id.continue_arrow);
		
		Animation a = new Animation()
		{
		    @Override
		    protected void applyTransformation(float interpolatedTime, Transformation t)
		    {
		    	interpolatedTime = 1f - interpolatedTime;
		    	
		    	ViewGroup.LayoutParams drawerParams = (ViewGroup.LayoutParams) wrapper.getLayoutParams();

		    	int height = dpToPx((36+1)*saved_searches.size());
		    	
		        drawerParams.height = (int) (interpolatedTime * height);
		        wrapper.setLayoutParams(drawerParams);
		        
		        arrow.setRotation(270f + (90f*interpolatedTime));
		    }
		};

		a.setDuration(300);
		wrapper.startAnimation(a);
		
		continue_is_open = false;
	}
	
	boolean sidemenu_is_open = false;
	
	public void toggle_sidebar()
	{
		if(!sidemenu_is_open)
		{
			open_sidemenu();
		}
		else
		{
			close_sidemenu();
		}
	}
	
	public void open_sidemenu()
	{
		final FrameLayout wrapper = (FrameLayout) findViewById(R.id.sidemenu_wrapper);
		final LinearLayout sidemenu = (LinearLayout) findViewById(R.id.sidemenu);
		final FrameLayout close_sidemenu_area = (FrameLayout) findViewById(R.id.close_sidemenu_area);
		
		Animation a = new Animation()
		{
		    @Override
		    protected void applyTransformation(float interpolatedTime, Transformation t)
		    {
		    	RelativeLayout.LayoutParams drawerParams = (RelativeLayout.LayoutParams) wrapper.getLayoutParams();
		    	
		    	int width = sidemenu.getWidth();
		    	
		        drawerParams.width = (int) (interpolatedTime * width);
		        wrapper.setLayoutParams(drawerParams);
		        
		        if(interpolatedTime < 0.001f)
		        {
		        	close_sidemenu_area.setVisibility(FrameLayout.GONE);
		        }
		        else
		        {
		        	close_sidemenu_area.setVisibility(FrameLayout.VISIBLE);
		        }
		        
		        close_sidemenu_area.setAlpha(interpolatedTime/2f);
		    }
		};

		a.setDuration(300);
		wrapper.startAnimation(a);
		
		sidemenu_is_open = true;
	}
	
	public void close_sidemenu(View v)
	{
		close_sidemenu();
	}
	
	public void close_sidemenu()
	{
		final FrameLayout wrapper = (FrameLayout) findViewById(R.id.sidemenu_wrapper);
		final LinearLayout sidemenu = (LinearLayout) findViewById(R.id.sidemenu);
		final FrameLayout close_sidemenu_area = (FrameLayout) findViewById(R.id.close_sidemenu_area);
		
		Animation a = new Animation()
		{
		    @Override
		    protected void applyTransformation(float interpolatedTime, Transformation t)
		    {
		    	interpolatedTime = 1f - interpolatedTime;
		    	
		    	RelativeLayout.LayoutParams drawerParams = (RelativeLayout.LayoutParams) wrapper.getLayoutParams();
		    	
		    	int width = sidemenu.getWidth();
		    	
		        drawerParams.width = (int) (interpolatedTime * width);
		        wrapper.setLayoutParams(drawerParams);
		        
		        if(interpolatedTime < 0.001f)
		        {
		        	close_sidemenu_area.setVisibility(FrameLayout.GONE);
		        }
		        else
		        {
		        	close_sidemenu_area.setVisibility(FrameLayout.VISIBLE);
		        }
		        
		        close_sidemenu_area.setAlpha(interpolatedTime/2f);
		    }
		};

		a.setDuration(300);
		wrapper.startAnimation(a);
		
		sidemenu_is_open = false;
	}
	
	class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if(
                	(Math.abs(e1.getY() - e2.getY()) <= SWIPE_MAX_OFF_PATH) &&
                	(e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) &&
                	(Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
                )
                {
                    toggle_sidebar();
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e)
        {
        	return true;
        }
    }
	
	public void login(View v)
	{
		Toast.makeText(getApplicationContext(), "Feature not implemented yet.", Toast.LENGTH_SHORT).show();
	}
	
	public void dummy(View v)
	{
	}
	
	@Override
	public void onBackPressed()
	{
		if(sidemenu_is_open)
		{
			close_sidemenu();
		}
		else
		{
			super.onBackPressed();
		}
	}
}
