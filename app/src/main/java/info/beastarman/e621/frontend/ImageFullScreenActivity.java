package info.beastarman.e621.frontend;

import android.content.Intent;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.middleware.ImageNavigator;
import info.beastarman.e621.views.ZoomableRelativeLayout;

public class ImageFullScreenActivity extends BaseActivity
{
	public static String NAVIGATOR = "navigator";
	public static String INTENT = "intent";

	public ImageNavigator image;
	public Intent intent;

	E621Image img = null;

	ScaleGestureDetector scaleGestureDetector;
	GestureDetector onTapListener;

	boolean scaling = false;

	int IMAGE_CHUNK_SIZE = 128;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_full_screen_activity);

		image = (ImageNavigator) getIntent().getSerializableExtra(NAVIGATOR);

		setTitle("#" + image.getId());

		intent = (Intent) getIntent().getParcelableExtra(INTENT);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		ColorDrawable bg = new ColorDrawable(getResources().getColor(R.color.BackgroundColor));
		bg.setAlpha(128);

		getActionBar().setBackgroundDrawable(bg);

		getWindow().getDecorView().setSystemUiVisibility(getUIInvisible());

		scaleGestureDetector = new ScaleGestureDetector(this, new OnPinchListener());
		onTapListener = new GestureDetector(this,new OnTapListener());
	}

	private int getUIInvisible()
	{
		int ret = View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

		if(Build.VERSION.SDK_INT > 18)
		{
			ret |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}

		return ret;
	}

	private int getUIVisible()
	{
		int ret = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

		return ret;
	}

	protected void onStart()
	{
		super.onStart();

		View zoomableLayout = findViewById(R.id.imageWrapper);

		zoomableLayout.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					scaling = false;
				}

				if(onTapListener.onTouchEvent(event))
				{
					return true;
				}
				else if(scaleGestureDetector.onTouchEvent(event))
				{
					return true;
				}
				return false;
			}
		});

		getWindow().getDecorView().post(new Runnable()
		{
			@Override
			public void run()
			{
				retrieveImage();
			}
		});
	}

	private void retrieveImage()
	{
		if(img == null)
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						img = e621.post__show(image.getId());

						updateImage();
					} catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}).start();
		}
		else
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					updateImage();
				}
			}).start();
		}
	}

	private float getImageScale()
	{
		int w;
		int h;

		switch (e621.getFileDownloadSize())
		{
			case E621Image.PREVIEW:
				w = img.preview_width;
				h = img.preview_height;
				break;
			case E621Image.SAMPLE:
				w = img.sample_width;
				h = img.sample_height;
				break;
			default:
				w = img.width;
				h = img.height;
				break;
		}

		float scale = ((float)w) / getWindow().getDecorView().getWidth();
		scale = Math.max(scale, ((float)h) / getWindow().getDecorView().getHeight());
		scale = Math.max(scale,1f);

		return scale;
	}

	private void updateImage()
	{
		showImage();
	}

	private void showImage()
	{
		final TableLayout tableLayout = (TableLayout) findViewById(R.id.imageViewTable);
		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

		int w;
		int h;

		switch (e621.getFileDownloadSize())
		{
			case E621Image.PREVIEW:
				w = img.preview_width;
				h = img.preview_height;
				break;
			case E621Image.SAMPLE:
				w = img.sample_width;
				h = img.sample_height;
				break;
			default:
				w = img.width;
				h = img.height;
				break;
		}

		int w_parts = (int)Math.ceil(w/(double)IMAGE_CHUNK_SIZE);
		int h_parts = (int)Math.ceil(h/(double)IMAGE_CHUNK_SIZE);

		final float scale = getImageScale();
		final int hh = h;
		final int ww = w;

		final ZoomableRelativeLayout zoomableRelativeLayout = (ZoomableRelativeLayout) findViewById(R.id.imageWrapper);

		zoomableRelativeLayout.post(new Runnable()
		{
			@Override
			public void run()
			{
				zoomableRelativeLayout.setPivotPadding(
						(int)(getWidth() - (ww/scale))/2,
						(int)(getHeight() - (hh/scale))/2,
						(int)(getWidth() - (ww/scale))/2,
						(int)(getHeight() - (hh/scale))/2
				);
			}
		});

		final ArrayList<ArrayList<ImageView>> imageViewList = new ArrayList<ArrayList<ImageView>>();

		InputStream is = e621.getImage(img, e621.getFileDownloadSize());

		BitmapRegionDecoder decoder = null;

		try
		{
			decoder = BitmapRegionDecoder.newInstance(is, false);
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		for(int j=0; j<h_parts; j++)
		{
			ArrayList<ImageView> localArray = new ArrayList<ImageView>();

			for(int i=0; i<w_parts; i++)
			{
				ImageView iv = new ImageView(this);

				int wa = i*IMAGE_CHUNK_SIZE;
				int ha = j*IMAGE_CHUNK_SIZE;

				int wz = (i+1 == w_parts? w :(i+1)*IMAGE_CHUNK_SIZE);
				int hz = (j+1 == h_parts? h :(j+1)*IMAGE_CHUNK_SIZE);

				iv.setImageBitmap(decoder.decodeRegion(new Rect(wa, ha, wz, hz), null));

				iv.setLayoutParams(new TableRow.LayoutParams((int)Math.ceil((wz - wa) / scale), (int)Math.ceil((hz - ha) / scale)));

				try
				{
					is.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}

				localArray.add(iv);
			}

			imageViewList.add(localArray);
		}

		decoder.recycle();

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				tableLayout.removeAllViews();

				for (int i = 0; i < imageViewList.size(); i++)
				{
					ArrayList<ImageView> localArray = imageViewList.get(i);

					TableRow row = new TableRow(ImageFullScreenActivity.this);

					for (int j = 0; j < localArray.size(); j++)
					{
						row.addView(localArray.get(j));
					}

					tableLayout.addView(row);
				}

				progressBar.setVisibility(View.GONE);
			}
		});
	}

	private boolean visible = false;

	private void toggleVisibility()
	{
		if(visible)
		{
			hideUI();
		}
		else
		{
			showUI();

		}
	}

	private void showUI()
	{
		getWindow().getDecorView().setSystemUiVisibility(getUIVisible());

		visible = true;
	}

	private void hideUI()
	{
		getWindow().getDecorView().setSystemUiVisibility(getUIInvisible());

		visible = false;
	}

	private class OnTapListener extends GestureDetector.SimpleOnGestureListener
	{
		@Override
		public boolean onSingleTapConfirmed(MotionEvent motionEvent)
		{
			toggleVisibility();

			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
		{
			if(!scaling)
			{
				float scale = getImageScale();

				ZoomableRelativeLayout zoomableRelativeLayout = (ZoomableRelativeLayout) ImageFullScreenActivity.this.findViewById(R.id.imageWrapper);

				zoomableRelativeLayout.move(distanceX*scale,distanceY*scale);
			}

			return false;
		}

		int doubleTapState = 0;

		@Override
		public boolean onDoubleTap(MotionEvent motionEvent)
		{
			ZoomableRelativeLayout zoomableRelativeLayout= (ZoomableRelativeLayout) ImageFullScreenActivity.this.findViewById(R.id.imageWrapper);

			if(doubleTapState == 0)
			{
				zoomableRelativeLayout.smoothScaleCenter(getImageScale());
			}
			else
			{
				zoomableRelativeLayout.reset();
			}

			doubleTapState = (doubleTapState+1)%2;

			return true;
		}
	}

	private class OnPinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
	{
		float currentSpan;
		float startFocusX;
		float startFocusY;

		float scale;

		public boolean onScaleBegin(ScaleGestureDetector detector)
		{
			currentSpan = detector.getCurrentSpan();
			startFocusX = detector.getFocusX();
			startFocusY = detector.getFocusY();

			scale = getImageScale();

			return true;
		}

		public boolean onScale(ScaleGestureDetector detector)
		{
			scaling = true;

			ZoomableRelativeLayout zoomableRelativeLayout= (ZoomableRelativeLayout) ImageFullScreenActivity.this.findViewById(R.id.imageWrapper);

			zoomableRelativeLayout.relativeScale(detector.getCurrentSpan() / currentSpan, startFocusX, startFocusY);

			currentSpan = detector.getCurrentSpan();

			return true;
		}

		public void onScaleEnd(ScaleGestureDetector detector)
		{
			ZoomableRelativeLayout zoomableRelativeLayout= (ZoomableRelativeLayout) ImageFullScreenActivity.this.findViewById(R.id.imageWrapper);

			zoomableRelativeLayout.release();
		}
	}
}
