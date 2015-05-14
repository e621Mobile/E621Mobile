package info.beastarman.e621.frontend;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.backend.Pair;
import info.beastarman.e621.middleware.E621DownloadedImage;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.GIFViewHandler;
import info.beastarman.e621.middleware.ImageLoadRunnable;
import info.beastarman.e621.middleware.ImageNavigator;
import info.beastarman.e621.middleware.ImageNavilagorLazyRelative;
import info.beastarman.e621.middleware.TouchImageViewHandler;
import info.beastarman.e621.views.GIFView;
import info.beastarman.e621.views.MediaInputStreamPlayer;
import info.beastarman.e621.views.SurfaceViewDetach;
import info.beastarman.e621.views.TouchImageView;
import info.beastarman.e621.views.VideoControllerView;

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

	public View onCreateView(final LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState)
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
				getImage(inflater,rl);
			}
		}).start();

		return rl;
	}

	private void getImage(final LayoutInflater inflater, final RelativeLayout rl)
	{
		if(image == null)
		{
			return;
		}

		try
		{
			final ProgressBar p = (ProgressBar) rl.findViewById(R.id.progressBar);

            E621DownloadedImage dImg = E621Middleware.getInstance().localGet(image.getId());

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

            final String file_ext;
            final int id;
            final Pair<Integer, Integer> imageSize;
            final String file_url;

            if(dImg == null)
            {
                final E621Image img = E621Middleware.getInstance().post__show(image.getId());

                file_ext = img.file_ext;
                id = img.id;
                imageSize = img.getSize(E621Middleware.getInstance().getFileDownloadSize());
                file_url = img.file_url;
            }
            else
            {
                file_ext = dImg.getType();
                id = dImg.getId();
                imageSize = new Pair<Integer, Integer>(dImg.width,dImg.height);
                file_url = null;
            }

			if(file_ext.equals("jpg") ||
				file_ext.equals("png") ||
				(file_ext.equals("gif") && !E621Middleware.getInstance().playGifs()))
			{
				final float scale = Math.max(1, Math.max(imageSize.left / 2048f, imageSize.right / 2048f));

				getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						final TouchImageView t = new TouchImageView(rl.getContext());
						t.setId(R.id.image_id);
						t.setTag(id);
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
						t.setLayoutParams(params);
						rl.addView(t);

						t.setOnClickListener(toggleListener());

						TouchImageViewHandler handler = new TouchImageViewHandler(t, p, (int) (imageSize.left / scale), (int) (imageSize.right / scale));

						new Thread(new ImageLoadRunnable(handler, id, E621Middleware.getInstance(), E621Middleware.getInstance().getFileDownloadSize())).start();
					}
				});
			}
			else if(file_ext.equals("gif"))
			{
				getActivity().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						Display display = getActivity().getWindowManager().getDefaultDisplay();
						Point size = new Point();
						display.getSize(size);

						float scale = Math.max((float) imageSize.left / size.x, (float) imageSize.right / size.y);

						int w = (int) (imageSize.left / scale);
						int h = (int) (imageSize.right / scale);

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

						new Thread(new ImageLoadRunnable(handler, id, E621Middleware.getInstance(), E621Middleware.getInstance().getFileDownloadSize())).start();
					}
				});
			}
			else if(file_ext.equals("webm"))
			{
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        final int screenWidth = getActivity().getWindowManager().getDefaultDisplay().getWidth();
                        final int screenHeight = getActivity().getWindowManager().getDefaultDisplay().getHeight();

                        final View v = inflater.inflate(R.layout.image_full_screen_video_layout,null);
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
                        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                        v.setLayoutParams(params);

                        final MediaInputStreamPlayer player = new MediaInputStreamPlayer();
                        final VideoControllerView controller = new VideoControllerView(rl.getContext());

                        final SurfaceViewDetach videoSurface = (SurfaceViewDetach) v.findViewById(R.id.videoSurface);
                        videoSurface.setTag(id);
                        videoSurface.setOnSeekListener(new SurfaceViewDetach.OnSeekListener() {
                            @Override
                            public void onSeek(View v, int position) {
                                player.seekTo(position);
                                player.start();
                            }
                        });
                        videoSurface.setOnDetachedFromWindowListener(new SurfaceViewDetach.OnDetachedFromWindowListener() {
                            @Override
                            public void onDetach(View v) {
                                player.close();
                            }
                        });

                        SurfaceHolder videoHolder = videoSurface.getHolder();
                        videoHolder.addCallback(new SurfaceHolder.Callback() {
                            @Override
                            public void surfaceCreated(SurfaceHolder surfaceHolder) {

                            }

                            @Override
                            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3)
                            {
                                player.setDisplay(surfaceHolder);
                            }

                            @Override
                            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

                            }
                        });

                        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mediaPlayer) {
                                controller.setMediaPlayer(new VideoControllerView.MediaPlayerControl() {
                                    public boolean canPause() {
                                        return true;
                                    }

                                    @Override
                                    public boolean canSeekBackward() {
                                        return true;
                                    }

                                    @Override
                                    public boolean canSeekForward() {
                                        return true;
                                    }

                                    @Override
                                    public int getBufferPercentage() {
                                        return 0;
                                    }

                                    @Override
                                    public int getCurrentPosition() {
                                        try
                                        {
                                            return player.getCurrentPosition();
                                        }
                                        catch (Throwable t)
                                        {
                                            return 0;
                                        }
                                    }

                                    @Override
                                    public int getDuration() {
                                        try
                                        {
                                            return player.getDuration();
                                        }
                                        catch (Throwable t)
                                        {
                                            return 1;
                                        }
                                    }

                                    @Override
                                    public boolean isPlaying()
                                    {
                                        try
                                        {
                                            return player.isPlaying();
                                        }
                                        catch(Throwable t)
                                        {
                                            return true;
                                        }
                                    }

                                    @Override
                                    public void pause()
                                    {
                                        try
                                        {
                                            player.pause();
                                        }
                                        catch(Throwable t)
                                        {

                                        }
                                    }

                                    @Override
                                    public void seekTo(int i)
                                    {
                                        try
                                        {
                                            player.seekTo(i);
                                        }
                                        catch(Throwable t)
                                        {

                                        }
                                    }

                                    @Override
                                    public void start() {
                                        try
                                        {
                                            player.start();
                                        }
                                        catch(Throwable t)
                                        {

                                        }
                                    }

                                    @Override
                                    public boolean isFullScreen() {
                                        return false;
                                    }

                                    @Override
                                    public void toggleFullScreen()
                                    {
                                        pause();

                                        String path = player.getFilePath();
                                        int position = getCurrentPosition();

                                        Intent i = new Intent(getActivity(),FullScreenVideoActivity.class);
                                        i.putExtra(FullScreenVideoActivity.VIDEO_PATH,path);
                                        i.putExtra(FullScreenVideoActivity.VIDEO_POSITION,position);
                                        startActivityForResult(i,FullScreenVideoActivity.RESULT_VIDEO_POSITION);
                                    }
                                });
                                controller.setAnchorView((FrameLayout) v);
                                controller.show(0);
                                player.setLooping(true);

                                int videoWidth = player.getVideoWidth();
                                int videoHeight = player.getVideoHeight();
                                float videoProportion = (float) videoWidth / (float) videoHeight;

                                float screenProportion = (float) screenWidth / (float) screenHeight;

                                // Get the SurfaceView layout parameters
                                final android.view.ViewGroup.LayoutParams lp = videoSurface.getLayoutParams();
                                if (videoProportion > screenProportion) {
                                    lp.width = screenWidth;
                                    lp.height = (int) ((float) screenWidth / videoProportion);
                                } else {
                                    lp.width = (int) (videoProportion * (float) screenHeight);
                                    lp.height = screenHeight;
                                }
                                // Commit the layout parameters
                                videoSurface.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        videoSurface.setLayoutParams(lp);
                                    }
                                });
                            }
                        });

                        new Thread(new Runnable() {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    InputStream in = E621Middleware.getInstance().getVideo(id);

                                    if(in != null)
                                    {
                                        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                        player.setVideoInputStream(in);
                                    }

                                    p.setVisibility(View.GONE);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                        v.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                controller.show();

                                return true;
                            }
                        });

                        rl.addView(v);
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

						ll.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                final Intent i = new Intent();
                                i.setAction(Intent.ACTION_VIEW);

                                if (file_url != null) {
                                    i.setData(Uri.parse(file_url));
                                    startActivity(i);
                                } else {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                E621Image img = E621Middleware.getInstance().post__show(id);
                                                i.setData(Uri.parse(img.file_url));

                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        startActivity(i);
                                                    }
                                                });
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }).start();
                                }

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
