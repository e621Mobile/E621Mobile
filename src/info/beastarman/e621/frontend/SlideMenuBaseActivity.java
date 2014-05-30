package info.beastarman.e621.frontend;

import info.beastarman.e621.R;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.RelativeLayout;

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
        
        RelativeLayout fullLayout= (RelativeLayout) getLayoutInflater().inflate(R.layout.activity_base, null);
        super.setContentView(fullLayout);
        
        gestureDetector = new GestureDetector(this, new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
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
	
	public void sidebar()
	{
		Log.d("Msg","AEEEE!");
	}
	
	class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
                {
                    sidebar();
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
}
