package info.beastarman.e621.frontend;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.io.IOException;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.backend.Pair;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.GIFViewHandler;
import info.beastarman.e621.middleware.ImageLoadRunnable;
import info.beastarman.e621.middleware.ImageNavigator;
import info.beastarman.e621.middleware.ImageNavilagorLazyRelative;
import info.beastarman.e621.middleware.TouchImageViewHandler;
import info.beastarman.e621.views.GIFView;
import info.beastarman.e621.views.TouchImageView;

public class ImageFullScreenActivityTouchImageViewFragment extends Fragment
{
	public static String LAZY_POSITION = "lazy";
	ImageNavigator image;

	public static ImageFullScreenActivityTouchImageViewFragment fromImageNavigator(ImageNavilagorLazyRelative navigator)
	{
		ImageFullScreenActivityTouchImageViewFragment ret = new ImageFullScreenActivityTouchImageViewFragment();

		Bundle bundle = new Bundle();

		bundle.putSerializable(LAZY_POSITION, navigator);

		ret.setArguments(bundle);

		return ret;
	}

	private View.OnClickListener toggleListener()
	{
		return new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				Activity a = getActivity();

				if (a instanceof ImageFullScreenActivity)
				{
					((ImageFullScreenActivity) a).toggleVisibility();
				}
			}
		};
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState)
	{
		final RelativeLayout rl = (RelativeLayout) inflater.inflate(R.layout.image_full_screen_static_image, container, false);

		rl.setOnClickListener(toggleListener());
		rl.findViewById(R.id.progressBar).setOnClickListener(toggleListener());

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				image = ((ImageNavilagorLazyRelative) getArguments().getSerializable(LAZY_POSITION)).getImageNavigator();
				getImage(rl);
			}
		}).start();

		return rl;
	}

	private void getImage(final RelativeLayout rl)
	{
		try
		{
			final ProgressBar p = (ProgressBar) rl.findViewById(R.id.progressBar);

			final E621Image img = E621Middleware.getInstance().post__show(image.getId());

			if(img.file_ext.equals("jpg") ||
				img.file_ext.equals("png") ||
				(img.file_ext.equals("gif") && !E621Middleware.getInstance().playGifs()))
			{
				final Pair<Integer, Integer> size = img.getSize(E621Middleware.getInstance().getFileDownloadSize());

				final float scale = Math.max(1, Math.max(size.left / 2048f, size.right / 2048f));

				getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						final TouchImageView t = new TouchImageView(rl.getContext());
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
						t.setLayoutParams(params);
						rl.addView(t);

						t.setOnClickListener(toggleListener());

						TouchImageViewHandler handler = new TouchImageViewHandler(t, p, (int) (size.left / scale), (int) (size.right / scale));

						new Thread(new ImageLoadRunnable(handler, img, E621Middleware.getInstance(), E621Middleware.getInstance().getFileDownloadSize())).start();
					}
				});
			}
			else if(img.file_ext.equals("gif"))
			{
				getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						Display display = getActivity().getWindowManager().getDefaultDisplay();
						Point size = new Point();
						display.getSize(size);

						float scale = Math.max((float)img.width/size.x,(float)img.height/size.y);

						int w = (int)(img.width/scale);
						int h = (int)(img.height/scale);

						final GIFView gifView = new GIFView(rl.getContext());
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(w,h);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
						gifView.setLayoutParams(params);
						gifView.pause();
						rl.addView(gifView);

						gifView.setOnLongClickListener(new View.OnLongClickListener()
						{
							@Override
							public boolean onLongClick(View view)
							{
								Activity a = getActivity();

								if (a instanceof ImageFullScreenActivity)
								{
									((ImageFullScreenActivity) a).toggleVisibility();

									return true;
								}

								return false;
							}
						});

						gifView.setOnClickListener(new View.OnClickListener()
						{
							@Override
							public void onClick(View view)
							{
								gifView.toggle();
							}
						});

						GIFViewHandler handler = new GIFViewHandler(gifView,p);

						new Thread(new ImageLoadRunnable(handler, img, E621Middleware.getInstance(), E621Middleware.getInstance().getFileDownloadSize())).start();
					}
				});
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
