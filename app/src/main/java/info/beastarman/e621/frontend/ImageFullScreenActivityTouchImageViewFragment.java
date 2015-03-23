package info.beastarman.e621.frontend;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
		if(image == null)
		{
			return;
		}

		try
		{
			final ProgressBar p = (ProgressBar) rl.findViewById(R.id.progressBar);

			final E621Image img = E621Middleware.getInstance().post__show(image.getId());

			while(getActivity() == null)
			{
				try
				{
					Thread.currentThread().sleep(1000);
				} catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
			}

			if(img.file_ext.equals("jpg") ||
				img.file_ext.equals("png") ||
				(img.file_ext.equals("gif") && !E621Middleware.getInstance().playGifs()))
			{
				final Pair<Integer, Integer> size = img.getSize(E621Middleware.getInstance().getFileDownloadSize());

				final float scale = Math.max(1, Math.max(size.left / 2048f, size.right / 2048f));

				Log.d(E621Middleware.LOG_TAG,""+scale);

				getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						final TouchImageView t = new TouchImageView(rl.getContext());
						t.setId(R.id.image_id);
						t.setTag(img.id);
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
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

						float scale = Math.max((float) img.width / size.x, (float) img.height / size.y);

						int w = (int) (img.width / scale);
						int h = (int) (img.height / scale);

						final GIFView gifView = new GIFView(rl.getContext());
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(w, h);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
						gifView.setLayoutParams(params);
						gifView.pause();
						rl.addView(gifView);

						gifView.setOnLongClickListener(new View.OnLongClickListener()
						{
							@Override
							public boolean onLongClick(View view)
							{
								gifView.toggle();

								return true;
							}
						});

						gifView.setOnClickListener(toggleListener());

						GIFViewHandler handler = new GIFViewHandler(gifView, p);

						new Thread(new ImageLoadRunnable(handler, img, E621Middleware.getInstance(), E621Middleware.getInstance().getFileDownloadSize())).start();
					}
				});
			}
			else
			{
				getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						final LinearLayout ll = new LinearLayout(rl.getContext());
						ll.setOrientation(LinearLayout.VERTICAL);
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
						ll.setLayoutParams(params);
						rl.addView(ll);

						ll.setOnLongClickListener(new View.OnLongClickListener()
						{
							@Override
							public boolean onLongClick(View view)
							{
								Intent i = new Intent();
								i.setAction(Intent.ACTION_VIEW);
								i.setData(Uri.parse(img.file_url));
								startActivity(i);

								return true;
							}
						});

						ll.setOnClickListener(toggleListener());

						ImageView error_image = new ImageView(rl.getContext());
						error_image.setBackgroundResource(android.R.drawable.ic_menu_report_image);
						LinearLayout.LayoutParams rel_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						rel_params.gravity = Gravity.CENTER_HORIZONTAL;
						error_image.setLayoutParams(rel_params);
						ll.addView(error_image);

						TextView tv = new TextView(rl.getContext());
						tv.setText("File not supported (yet).\nHold here to try opening it with another app.");
						tv.setGravity(Gravity.CENTER_HORIZONTAL);
						tv.setPadding(32, 32, 32, 32);
						ll.addView(tv);

						((ViewGroup) p.getParent()).removeView(p);
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
