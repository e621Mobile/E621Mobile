package info.beastarman.e621.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

import java.util.Calendar;

public class NoHorizontalScrollView extends HorizontalScrollView
{
	private static final int MAX_CLICK_DURATION = 200;
	private long startClickTime;

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
					this.performClick();
				}
		}

		return super.onTouchEvent(ev);
	}
}
