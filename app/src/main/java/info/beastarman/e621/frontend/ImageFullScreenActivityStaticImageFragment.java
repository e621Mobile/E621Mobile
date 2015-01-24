package info.beastarman.e621.frontend;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.ImageNavigator;
import info.beastarman.e621.middleware.ImageNavilagorLazyRelative;
import info.beastarman.e621.views.ZoomableRelativeLayout;

public class ImageFullScreenActivityStaticImageFragment extends Fragment
{
	public static String POSITION = "pos";
	public static String LAZY_POSITION = "lazy";
	ImageNavigator image;
	private ZoomableRelativeLayout zoomableRelativeLayout;

	ScaleGestureDetector scaleGestureDetector;
	GestureDetector onTapListener;

	E621Image img = null;

	boolean scaling = false;

	int IMAGE_CHUNK_SIZE = 512;

	public static ImageFullScreenActivityStaticImageFragment fromImageNavigator(ImageNavilagorLazyRelative navigator)
	{
		ImageFullScreenActivityStaticImageFragment ret = new ImageFullScreenActivityStaticImageFragment();

		Bundle bundle = new Bundle();

		bundle.putSerializable(LAZY_POSITION, navigator);

		ret.setArguments(bundle);

		return ret;
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState)
	{
		zoomableRelativeLayout = (ZoomableRelativeLayout) inflater.inflate(R.layout.image_full_screen_static_image, container, false);

		if(getArguments().containsKey(POSITION))
		{
			image = (ImageNavigator) getArguments().getSerializable(POSITION);
		}
		else
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					image = ((ImageNavilagorLazyRelative) getArguments().getSerializable(LAZY_POSITION)).getImageNavigator();

					zoomableRelativeLayout.post(new Runnable()
					{
						@Override
						public void run()
						{
							getImage();
						}
					});
				}
			}).start();
		}

		return zoomableRelativeLayout;
	}

	@Override
	public void onStart()
	{
		super.onStart();

		if(image != null) getImage();
	}

	private void getImage()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					img = E621Middleware.getInstance().post__show(image.getId());
				} catch (IOException e)
				{
					e.printStackTrace();
				}

				showImage();

				zoomableRelativeLayout.post(new Runnable()
				{
					@Override
					public void run()
					{
						scaleGestureDetector = new ScaleGestureDetector(getActivity(), new OnPinchListener());
						onTapListener = new GestureDetector(getActivity(), new OnTapListener());

						zoomableRelativeLayout.setOnTouchListener(new View.OnTouchListener()
						{
							@Override
							public boolean onTouch(View v, MotionEvent event)
							{
								if (event.getAction() == MotionEvent.ACTION_DOWN)
								{
									scaling = false;
								}

								if (onTapListener.onTouchEvent(event))
								{
									return true;
								}
								else if (scaleGestureDetector.onTouchEvent(event))
								{
									return true;
								}

								return false;
							}
						});
					}
				});
			}
		}).start();
	}

	ArrayList<ImageView> recyclableImageViews = new ArrayList<ImageView>();

	public ImageView gimmeRecyclableImageView()
	{
		Context ctx = getActivity();

		if(ctx == null) return null;

		ImageView iv = new ImageView(ctx);

		recyclableImageViews.add(iv);

		return iv;
	}

	@Override
	public void onStop()
	{
		for(ImageView iv : recyclableImageViews)
		{
			Drawable drawable = iv.getDrawable();
			if (drawable instanceof BitmapDrawable)
			{
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				Bitmap bitmap = bitmapDrawable.getBitmap();

				if(bitmap != null && !bitmap.isRecycled())
				{
					bitmap.recycle();
				}
			}
		}

		recyclableImageViews.clear();

		((TableLayout)zoomableRelativeLayout.findViewById(R.id.progressBar)).removeAllViews();

		super.onStop();
	}

	private void showImage()
	{
		final TableLayout tableLayout = (TableLayout) zoomableRelativeLayout.findViewById(R.id.progressBar);
		final ProgressBar progressBar = (ProgressBar) zoomableRelativeLayout.findViewById(R.id.progressBar);

		InputStream is = E621Middleware.getInstance().getImage(img, E621Middleware.getInstance().getFileDownloadSize());

		BitmapRegionDecoder decoder = null;

		try
		{
			decoder = BitmapRegionDecoder.newInstance(is, false);
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		int w = decoder.getWidth();
		int h = decoder.getHeight();

		int sscale = 1;

		while(w*h > sscale*sscale*5000000)
		{
			sscale *= 2;
		}

		w/=sscale;
		h/=sscale;

		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inSampleSize=sscale;

		int w_parts = (int)Math.ceil(w/(double)IMAGE_CHUNK_SIZE);
		int h_parts = (int)Math.ceil(h/(double)IMAGE_CHUNK_SIZE);

		final float scale = getImageScale();
		final int hh = h * sscale;
		final int ww = w * sscale;

		zoomableRelativeLayout.post(new Runnable()
		{
			@Override
			public void run()
			{
				zoomableRelativeLayout.setPivotPadding((int) (zoomableRelativeLayout.getWidth() - (ww / scale)) / 2,
						(int) (zoomableRelativeLayout.getHeight() - (hh / scale)) / 2,
						(int) (zoomableRelativeLayout.getWidth() - (ww / scale)) / 2,
						(int) (zoomableRelativeLayout.getHeight() - (hh / scale)) / 2);
			}
		});

		final ArrayList<ArrayList<ImageView>> imageViewList = new ArrayList<ArrayList<ImageView>>();

		for(int j=0; j<h_parts; j++)
		{
			ArrayList<ImageView> localArray = new ArrayList<ImageView>();

			for(int i=0; i<w_parts; i++)
			{
				ImageView iv = gimmeRecyclableImageView();

				if(iv == null) return;

				int wa = i*IMAGE_CHUNK_SIZE*sscale;
				int ha = j*IMAGE_CHUNK_SIZE*sscale;

				int wz = (i+1 == w_parts? w :(i+1)*IMAGE_CHUNK_SIZE)*sscale;
				int hz = (j+1 == h_parts? h :(j+1)*IMAGE_CHUNK_SIZE)*sscale;

				iv.setImageBitmap(decoder.decodeRegion(new Rect(wa, ha, wz, hz), o));

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

		zoomableRelativeLayout.post(new Runnable()
		{
			@Override
			public void run()
			{
				tableLayout.removeAllViews();

				for (int i = 0; i < imageViewList.size(); i++)
				{
					ArrayList<ImageView> localArray = imageViewList.get(i);

					TableRow row = new TableRow(zoomableRelativeLayout.getContext());

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

	private float getImageScale()
	{
		int w;
		int h;

		switch (E621Middleware.getInstance().getFileDownloadSize())
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

		try
		{
			float scale = ((float)w) / getResources().getDisplayMetrics().widthPixels;
			scale = Math.max(scale, ((float)h) / getResources().getDisplayMetrics().heightPixels);
			scale = Math.max(scale,1f);

			return scale;
		}
		catch(IllegalStateException e)
		{
			return 1;
		}
	}

	private class OnTapListener extends GestureDetector.SimpleOnGestureListener
	{
		@Override
		public boolean onSingleTapConfirmed(MotionEvent motionEvent)
		{
			((ImageFullScreenActivity)getActivity()).toggleVisibility();

			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
		{
			if(!scaling)
			{
				float scale = getImageScale();

				zoomableRelativeLayout.move(distanceX*scale,distanceY*scale);
			}

			return false;
		}

		int doubleTapState = 0;

		@Override
		public boolean onDoubleTap(MotionEvent motionEvent)
		{
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

			zoomableRelativeLayout.relativeScale(detector.getCurrentSpan() / currentSpan, startFocusX, startFocusY);

			currentSpan = detector.getCurrentSpan();

			return true;
		}

		public void onScaleEnd(ScaleGestureDetector detector)
		{
			zoomableRelativeLayout.release();
		}
	}
}
