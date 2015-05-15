package info.beastarman.e621.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;

public class ZoomableRelativeLayout extends RelativeLayout
{
	float mScaleFactor = 1;
	float mPivotX;
	float mPivotY;

	float MIN_SCALE = 1f;
	float MAX_SCALE = 20f;
	int leftPadding = 0;
	int topPadding = 0;
	int rightPadding = 0;
	int downPadding = 0;

	public ZoomableRelativeLayout(Context context)
	{
		super(context);
	}

	public ZoomableRelativeLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public ZoomableRelativeLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public void setPivotPadding(int left, int top, int right, int down)
	{
		leftPadding = left;
		topPadding = top;
		rightPadding = right;
		downPadding = down;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		mPivotX = getWidth() / 2;
		mPivotY = getHeight() / 2;
	}

	protected void dispatchDraw(Canvas canvas)
	{
		mPivotX = Math.min(Math.max(mPivotX, leftPadding), getWidth() - rightPadding);
		mPivotY = Math.min(Math.max(mPivotY, topPadding), getHeight() - downPadding);

		canvas.save(Canvas.MATRIX_SAVE_FLAG);

		if(mScaleFactor <= 1)
		{
			canvas.scale(mScaleFactor, mScaleFactor, getWidth() / 2, getHeight() / 2);
		}
		else
		{
			canvas.scale(mScaleFactor, mScaleFactor, mPivotX, mPivotY);
		}

		super.dispatchDraw(canvas);
		canvas.restore();
	}

	public void scale(float scaleFactor, float pivotX, float pivotY)
	{
		mScaleFactor = scaleFactor;
		mPivotX = pivotX;
		mPivotY = pivotY;
		this.invalidate();
	}

	public void relativeScale(float scaleFactor, float pivotX, float pivotY)
	{
		mScaleFactor *= scaleFactor;

		pivotX = (((pivotX - mPivotX) * scaleFactor) + mPivotX);
		pivotY = (((pivotY - mPivotY) * scaleFactor) + mPivotY);

		if(scaleFactor >= 1)
		{
			mPivotX = mPivotX + (pivotX - mPivotX) * (1 - 1 / scaleFactor);
			mPivotY = mPivotY + (pivotY - mPivotY) * (1 - 1 / scaleFactor);
		}
		else
		{
			pivotX = getWidth() / 2;
			pivotY = getHeight() / 2;

			mPivotX = mPivotX + (pivotX - mPivotX) * (1 - scaleFactor);
			mPivotY = mPivotY + (pivotY - mPivotY) * (1 - scaleFactor);
		}

		this.invalidate();
	}

	public void move(float x, float y)
	{
		mPivotX += x / mScaleFactor;
		mPivotY += y / mScaleFactor;

		invalidate();
	}

	public void reset()
	{
		if(mScaleFactor != 1f)
		{
			final float startScaleFactor = mScaleFactor;

			Animation a = new Animation()
			{
				@Override
				protected void applyTransformation(float interpolatedTime, Transformation t)
				{
					scale(startScaleFactor + (1f - startScaleFactor) * interpolatedTime, mPivotX, mPivotY);
				}
			};

			a.setDuration(300);
			startAnimation(a);
		}
	}

	public void smoothScaleCenter(final float scale)
	{
		final float startScaleFactor = mScaleFactor;
		final float startPivotX = mPivotX;
		final float startPivotY = mPivotY;

		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				scale(
							 startScaleFactor + (scale - startScaleFactor) * interpolatedTime,
							 startPivotX + (getWidth() / 2 - startPivotX) * interpolatedTime,
							 startPivotY + (getHeight() / 2 - startPivotY) * interpolatedTime
				);
			}
		};

		a.setDuration(300);
		startAnimation(a);
	}

	public void smoothScale(final float scale)
	{
		final float startScaleFactor = mScaleFactor;

		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				scale(
							 startScaleFactor + (scale - startScaleFactor) * interpolatedTime,
							 mPivotX,
							 mPivotY
				);
			}
		};

		a.setDuration(300);
		startAnimation(a);
	}

	public void release()
	{
		if(mScaleFactor < MIN_SCALE)
		{
			smoothScale(MIN_SCALE);
		}
		else if(mScaleFactor > MAX_SCALE)
		{
			smoothScale(MAX_SCALE);
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev)
	{
		return true;
	}
}