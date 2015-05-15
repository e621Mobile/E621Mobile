package info.beastarman.e621.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.View;

/**
 * Created by beastarman on 4/25/2015.
 */
public class SurfaceViewDetach extends SurfaceView
{
	OnDetachedFromWindowListener listener = null;
	OnSeekListener slistener = null;

	public SurfaceViewDetach(Context context)
	{
		super(context);
	}

	public SurfaceViewDetach(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public SurfaceViewDetach(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public void setOnDetachedFromWindowListener(OnDetachedFromWindowListener _listener)
	{
		listener = _listener;
	}

	@Override
	protected void onDetachedFromWindow()
	{
		if(listener != null)
		{
			listener.onDetach(this);
		}

		super.onDetachedFromWindow();
	}

	public void setOnSeekListener(OnSeekListener _slistener)
	{
		slistener = _slistener;
	}

	public void seek(int i)
	{
		if(slistener != null)
		{
			slistener.onSeek(this, i);
		}
	}

	public static interface OnDetachedFromWindowListener
	{
		public void onDetach(View v);
	}

	public static interface OnSeekListener
	{
		public void onSeek(View v, int position);
	}
}
