package info.beastarman.e621.views;

import info.beastarman.e621.R;

import java.util.HashSet;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

public class LazyRunScrollView extends ObservableScrollView
{
	private HashSet<ThreadTrigger> threads = new HashSet<ThreadTrigger>();
	private int max_y= 0;
	private int step = 1;
	
	public LazyRunScrollView(Context context) {
		super(context);
		
		final LazyRunScrollView self = this;
		final LazyRunScrollViewListener t = new LazyRunScrollViewListener();
		
		setScrollViewListener(t);
		
		post(new Runnable()
		{
			@Override
			public void run() {
				t.onScrollChanged(self, 0, 0, 0, 0);
			}
		});
	}
	
	public LazyRunScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.LazyRunScrollView, 0, 0);
		
		try
        {
            step = a.getInteger(R.styleable.LazyRunScrollView_step, 1);
        }
        finally
        {
            a.recycle();
        }
		
		final LazyRunScrollView self = this;
		final LazyRunScrollViewListener t = new LazyRunScrollViewListener();
		
		setScrollViewListener(t);
		
		post(new Runnable()
		{
			@Override
			public void run() {
				t.onScrollChanged(self, 0, 0, 0, 0);
			}
		});
	}
	
	public LazyRunScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.LazyRunScrollView, 0, 0);
		
		try
        {
            step = a.getInteger(R.styleable.LazyRunScrollView_step, 1);
        }
        finally
        {
            a.recycle();
        }
		
		final LazyRunScrollView self = this;
		final LazyRunScrollViewListener t = new LazyRunScrollViewListener();
		
		setScrollViewListener(t);
		
		post(new Runnable()
		{
			@Override
			public void run() {
				t.onScrollChanged(self, 0, 0, 0, 0);
			}
		});
	}
	
	public void addThread(Thread thread, int trigger)
	{
		ThreadTrigger t = new ThreadTrigger(thread,trigger);
		
		if(!t.start(max_y))
		{
			threads.add(t);
		}
	}
	
	private class LazyRunScrollViewListener implements ScrollViewListener
	{
		private Semaphore s = new Semaphore(1);
		
		@Override
		public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int old_x, int old_y)
		{
			try
			{
				if(!s.tryAcquire())
				{
					return;
				}
				
				int new_y = y + scrollView.getHeight();
				
				new_y = (int)(Math.ceil(((double)new_y)/step)*step);
				
				if(new_y > max_y)
				{
					max_y = new_y;
					
					for(ThreadTrigger t : threads)
					{
						t.start(max_y);
					}
				}
			}
			finally
			{
				s.release();
			}
		}
	}
	
	private class ThreadTrigger
	{
		Thread t;
		Integer trigger;
		
		ThreadTrigger(Thread t, int trigger)
		{
			this.t = t;
			this.trigger = trigger;
		}
		
		public boolean start(int event)
		{
			if(trigger != null && event >= trigger)
			{
				t.start();
				trigger = null;
				
				return true;
			}
			
			return false;
		}
	}
}
