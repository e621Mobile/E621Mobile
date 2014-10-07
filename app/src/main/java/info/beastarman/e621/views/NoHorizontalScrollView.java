package info.beastarman.e621.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

import java.util.Calendar;

import info.beastarman.e621.middleware.E621Middleware;

public class NoHorizontalScrollView extends HorizontalScrollView
{
	public NoHorizontalScrollView(Context context)
	{
		super(context);
	}

	public NoHorizontalScrollView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public NoHorizontalScrollView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	private static final int MAX_CLICK_DURATION = 200;
	private long startClickTime;

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		switch(ev.getAction())
		{
			case MotionEvent.ACTION_MOVE:
				return false;
			case MotionEvent.ACTION_DOWN:
				startClickTime = Calendar.getInstance().getTimeInMillis();
				break;
			case MotionEvent.ACTION_UP:
				long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;

				if(clickDuration < MAX_CLICK_DURATION)
				{
					Log.d(E621Middleware.LOG_TAG,"123123123123213213");

					this.performClick();
				}
		}

		return super.onTouchEvent(ev);
	}
}
