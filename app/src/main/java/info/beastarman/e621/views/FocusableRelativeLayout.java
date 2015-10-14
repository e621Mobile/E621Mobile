package info.beastarman.e621.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class FocusableRelativeLayout extends RelativeLayout
{
	public FocusableRelativeLayout(Context context)
	{
		super(context);
	}

	public FocusableRelativeLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public FocusableRelativeLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	OnFocusListener onFocusListener = null;

	public void setOnFocusListener(OnFocusListener onFocusListener)
	{
		this.onFocusListener = onFocusListener;
	}

	public OnFocusListener getOnFocusListener()
	{
		return onFocusListener;
	}

	boolean focus = false;

	public void setFocus(boolean focus)
	{
		if(focus == this.focus) return;

		this.focus = focus;

		if(onFocusListener!=null)
		{
			if(focus)
			{
				onFocusListener.onSetFocus(this);
			}
			else
			{
				onFocusListener.onUnsetFocus(this);
			}
		}
	}

	public boolean getFocus()
	{
		return focus;
	}

	public interface OnFocusListener
	{
		void onSetFocus(FocusableRelativeLayout v);

		void onUnsetFocus(FocusableRelativeLayout v);
	}
}
