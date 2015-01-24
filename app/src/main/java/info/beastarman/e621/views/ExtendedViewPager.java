package info.beastarman.e621.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import info.beastarman.e621.R;

public class ExtendedViewPager extends ViewPager
{
	public ExtendedViewPager(Context context) {
		super(context);
	}

	public ExtendedViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
		if (v instanceof TouchImageView)
		{
			return ((TouchImageView) v).canScrollHorizontallyFroyo(-dx);
		}
		else if(v.findViewById(R.id.touchImageView) != null)
		{
			return ((TouchImageView) v.findViewById(R.id.touchImageView)).canScrollHorizontallyFroyo(-dx);
		}
		else
		{
			return super.canScroll(v, checkV, dx, x, y);
		}
	}
}
