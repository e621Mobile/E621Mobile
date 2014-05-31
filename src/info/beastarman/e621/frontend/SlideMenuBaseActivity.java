package info.beastarman.e621.frontend;

import info.beastarman.e621.R;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.view.animation.Transformation;

public class SlideMenuBaseActivity extends BaseActivity
{
	private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	
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
		    }
		};

		a.setDuration(300);
		wrapper.startAnimation(a);
		
		close_sidemenu_area.setVisibility(FrameLayout.VISIBLE);
		
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
		    	RelativeLayout.LayoutParams drawerParams = (RelativeLayout.LayoutParams) wrapper.getLayoutParams();
		    	
		    	int width = sidemenu.getWidth();
		    	
		        drawerParams.width = (int) ((1.0 - interpolatedTime) * width);
		        wrapper.setLayoutParams(drawerParams);
		    }
		};

		a.setDuration(300);
		wrapper.startAnimation(a);
		
		close_sidemenu_area.setVisibility(FrameLayout.GONE);
		
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
		Log.d("Msg","LOGIN!");
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
