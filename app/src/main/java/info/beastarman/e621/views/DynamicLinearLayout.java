package info.beastarman.e621.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Adapter;
import android.widget.LinearLayout;

public class DynamicLinearLayout extends LinearLayout
{
	public DynamicLinearLayout(Context context)
	{
		super(context);
	}

	public DynamicLinearLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public DynamicLinearLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	Adapter _adapter = null;
	int loaded = 0;

	public void setAdapter(Adapter adapter)
	{
		_adapter = adapter;
		loaded = 0;
		removeAllViews();
		loadMore(10);
	}

	public boolean loadMore(int howMuch)
	{
		boolean ret = false;

		if(_adapter == null) return ret;

		while(loaded < _adapter.getCount() &&  howMuch-->0)
		{
			addView(_adapter.getView(loaded++,null,this));

			ret = true;
		}

		return ret;
	}
}
