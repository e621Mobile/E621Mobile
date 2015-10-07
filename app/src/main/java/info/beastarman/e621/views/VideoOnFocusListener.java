package info.beastarman.e621.views;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import info.beastarman.e621.R;
import info.beastarman.e621.frontend.FullScreenVideoActivity;
import info.beastarman.e621.frontend.ImageFullScreenActivity;
import info.beastarman.e621.frontend.ImageFullScreenActivityTouchImageViewFragment;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.ImageNavigator;

/**
 * Created by beastarman on 9/19/2015.
 */
public class VideoOnFocusListener implements FocusableRelativeLayout.OnFocusListener
{
	private ImageFullScreenActivityTouchImageViewFragment fragment;
	private ImageNavigator image;
	private View v = null;
	private MediaInputStreamPlayer player = null;
	private String pathTemp = null;

	public VideoOnFocusListener(ImageFullScreenActivityTouchImageViewFragment fragment, ImageNavigator image)
	{
		this.image = image;
		this.fragment = fragment;
	}

	int disableFocusToggle = 0;

	@Override
	public void onSetFocus(final FocusableRelativeLayout rl)
	{
		if(disableFocusToggle>0)
		{
			disableFocusToggle--;
			return;
		}

		fragment.getActivity().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				final int screenWidth = fragment.getActivity().getWindowManager().getDefaultDisplay().getWidth();
				final int screenHeight = fragment.getActivity().getWindowManager().getDefaultDisplay().getHeight();

				v = fragment.getActivity().getLayoutInflater().inflate(R.layout.image_full_screen_video_layout, null);
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				v.setLayoutParams(params);

				player = new MediaInputStreamPlayer();
				final VideoControllerView controller = new VideoControllerView(rl.getContext());

				final SurfaceViewDetach videoSurface = (SurfaceViewDetach) v.findViewById(R.id.videoSurface);
				videoSurface.setTag(image.getId());
				videoSurface.setOnSeekListener(new SurfaceViewDetach.OnSeekListener()
				{
					@Override
					public void onSeek(View v, int position)
					{
						player.seekTo(position);
						player.start();
					}
				});
				videoSurface.setOnDetachedFromWindowListener(new SurfaceViewDetach.OnDetachedFromWindowListener()
				{
					@Override
					public void onDetach(View v)
					{
						player.close();
					}
				});

				SurfaceHolder videoHolder = videoSurface.getHolder();
				videoHolder.addCallback(new SurfaceHolder.Callback()
				{
					@Override
					public void surfaceCreated(SurfaceHolder surfaceHolder)
					{

					}

					@Override
					public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3)
					{
						player.setDisplay(surfaceHolder);
					}

					@Override
					public void surfaceDestroyed(SurfaceHolder surfaceHolder)
					{

					}
				});

				player.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
				{
					@Override
					public void onPrepared(final MediaPlayer mediaPlayer)
					{
						final VideoControllerView.MediaPlayerControl mediaPlayerControl = new VideoControllerView.MediaPlayerControl()
						{
							public boolean canPause()
							{
								return true;
							}

							@Override
							public boolean canSeekBackward()
							{
								return true;
							}

							@Override
							public boolean canSeekForward()
							{
								return true;
							}

							@Override
							public int getBufferPercentage()
							{
								return 0;
							}

							@Override
							public int getCurrentPosition()
							{
								try
								{
									return player.getCurrentPosition();
								}
								catch(Throwable t)
								{
									return 0;
								}
							}

							@Override
							public int getDuration()
							{
								try
								{
									return player.getDuration();
								}
								catch(Throwable t)
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
							public void start()
							{
								try
								{
									player.start();
								}
								catch(Throwable t)
								{

								}
							}

							@Override
							public boolean isFullScreen()
							{
								return false;
							}

							@Override
							public void toggleFullScreen()
							{
								pause();

								pathTemp = player.getFilePath();
								int position = getCurrentPosition();

								Intent i = new Intent(fragment.getActivity(), FullScreenVideoActivity.class);
								i.putExtra(FullScreenVideoActivity.VIDEO_PATH, pathTemp);
								i.putExtra(FullScreenVideoActivity.VIDEO_POSITION, position);

								fragment.startActivityForResult(i, FullScreenVideoActivity.RESULT_VIDEO_POSITION);

								pauseView(rl);
							}
						};

						controller.setMediaPlayer(mediaPlayerControl);
						controller.setAnchorView((FrameLayout) v);
						controller.show(0);
						player.setLooping(true);
						player.seekTo(0);
						player.start();

						int videoWidth = player.getVideoWidth();
						int videoHeight = player.getVideoHeight();
						float videoProportion = (float) videoWidth / (float) videoHeight;

						float screenProportion = (float) screenWidth / (float) screenHeight;

						// Get the SurfaceView layout parameters
						final android.view.ViewGroup.LayoutParams lp = videoSurface.getLayoutParams();
						if(videoProportion > screenProportion)
						{
							lp.width = screenWidth;
							lp.height = (int) ((float) screenWidth / videoProportion);
						}
						else
						{
							lp.width = (int) (videoProportion * (float) screenHeight);
							lp.height = screenHeight;
						}
						// Commit the layout parameters
						videoSurface.post(new Runnable()
						{
							@Override
							public void run()
							{
								videoSurface.setLayoutParams(lp);
								mediaPlayerControl.pause();
								controller.show(0);
							}
						});
					}
				});

				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							InputStream in = E621Middleware.getInstance().getVideo(image.getId());

							player.setInputStreamCheckListener(new MediaInputStreamPlayer.InputStreamCheckListener()
							{
								@Override
								public void onCheck(boolean isOk)
								{
									if(!isOk)
									{
										final ProgressBar p = (ProgressBar) rl.findViewById(R.id.progressBar);

										p.post(new Runnable()
										{
											@Override
											public void run()
											{
												p.setVisibility(View.GONE);

												Toast.makeText(fragment.getActivity(), "Could not load video", Toast.LENGTH_SHORT).show();
											}
										});
									}
								}
							});

							player.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
							{
								@Override
								public void onCompletion(MediaPlayer mediaPlayer)
								{
									mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
								}
							});

							if(in != null)
							{
								player.setVideoInputStream(in);
							}
						}
						catch(IOException e)
						{
							e.printStackTrace();
						}
					}
				}).start();

				v.setOnTouchListener(new View.OnTouchListener()
				{
					@Override
					public boolean onTouch(View view, MotionEvent motionEvent)
					{
						Activity a = fragment.getActivity();

						if(a instanceof ImageFullScreenActivity)
						{
							ImageFullScreenActivity act = ((ImageFullScreenActivity) a);

							if(act.visible) act.hideUI();
						}

						controller.show();

						return true;
					}
				});

				rl.addView(v);
			}
		});
	}

	@Override
	public void onUnsetFocus(FocusableRelativeLayout rl)
	{
		if(disableFocusToggle>0)
		{
			return;
		}

		rl.removeView(v);
	}

	public void pauseView(FocusableRelativeLayout rl)
	{
		player.deleteOnClose = false;
		rl.removeView(v);

		disableFocusToggle=1;
	}

	public void restore(final FocusableRelativeLayout rl, final int position)
	{
		fragment.getActivity().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				final int screenWidth = fragment.getActivity().getWindowManager().getDefaultDisplay().getWidth();
				final int screenHeight = fragment.getActivity().getWindowManager().getDefaultDisplay().getHeight();

				v = fragment.getActivity().getLayoutInflater().inflate(R.layout.image_full_screen_video_layout, null);
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				v.setLayoutParams(params);

				player = new MediaInputStreamPlayer();
				final VideoControllerView controller = new VideoControllerView(rl.getContext());

				final SurfaceViewDetach videoSurface = (SurfaceViewDetach) v.findViewById(R.id.videoSurface);
				videoSurface.setTag(image.getId());
				videoSurface.setOnSeekListener(new SurfaceViewDetach.OnSeekListener()
				{
					@Override
					public void onSeek(View v, int position)
					{
						player.seekTo(position);
						player.start();
					}
				});
				videoSurface.setOnDetachedFromWindowListener(new SurfaceViewDetach.OnDetachedFromWindowListener()
				{
					@Override
					public void onDetach(View v)
					{
						player.close();
					}
				});

				SurfaceHolder videoHolder = videoSurface.getHolder();
				videoHolder.addCallback(new SurfaceHolder.Callback()
				{
					@Override
					public void surfaceCreated(SurfaceHolder surfaceHolder)
					{
					}

					@Override
					public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3)
					{
						player.setDisplay(surfaceHolder);
					}

					@Override
					public void surfaceDestroyed(SurfaceHolder surfaceHolder)
					{

					}
				});

				player.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
				{
					@Override
					public void onPrepared(MediaPlayer mediaPlayer)
					{
						VideoControllerView.MediaPlayerControl mediaPlayerControl = new VideoControllerView.MediaPlayerControl()
						{
							public boolean canPause()
							{
								return true;
							}

							@Override
							public boolean canSeekBackward()
							{
								return true;
							}

							@Override
							public boolean canSeekForward()
							{
								return true;
							}

							@Override
							public int getBufferPercentage()
							{
								return 0;
							}

							@Override
							public int getCurrentPosition()
							{
								try
								{
									return player.getCurrentPosition();
								}
								catch(Throwable t)
								{
									return 0;
								}
							}

							@Override
							public int getDuration()
							{
								try
								{
									return player.getDuration();
								}
								catch(Throwable t)
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
							public void start()
							{
								try
								{
									player.start();
								}
								catch(Throwable t)
								{

								}
							}

							@Override
							public boolean isFullScreen()
							{
								return false;
							}

							@Override
							public void toggleFullScreen()
							{
								pause();

								int position = getCurrentPosition();

								Intent i = new Intent(fragment.getActivity(), FullScreenVideoActivity.class);
								i.putExtra(FullScreenVideoActivity.VIDEO_PATH, pathTemp);
								i.putExtra(FullScreenVideoActivity.VIDEO_POSITION, position);
								fragment.startActivityForResult(i, FullScreenVideoActivity.RESULT_VIDEO_POSITION);

								pauseView(rl);
							}
						};

						controller.setMediaPlayer(mediaPlayerControl);
						controller.setAnchorView((FrameLayout) v);
						controller.show(0);
						player.setLooping(true);
						player.seekTo(position);
						player.start();

						int videoWidth = player.getVideoWidth();
						int videoHeight = player.getVideoHeight();
						float videoProportion = (float) videoWidth / (float) videoHeight;

						float screenProportion = (float) screenWidth / (float) screenHeight;

						// Get the SurfaceView layout parameters
						final android.view.ViewGroup.LayoutParams lp = videoSurface.getLayoutParams();
						if(videoProportion > screenProportion)
						{
							lp.width = screenWidth;
							lp.height = (int) ((float) screenWidth / videoProportion);
						}
						else
						{
							lp.width = (int) (videoProportion * (float) screenHeight);
							lp.height = screenHeight;
						}
						// Commit the layout parameters
						videoSurface.post(new Runnable()
						{
							@Override
							public void run()
							{
								videoSurface.setLayoutParams(lp);
							}
						});
					}
				});

				try
				{
					player.setDataSource(pathTemp);
					player.prepareAsync();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}

				v.setOnTouchListener(new View.OnTouchListener()
				{
					@Override
					public boolean onTouch(View view, MotionEvent motionEvent)
					{
						Activity a = fragment.getActivity();

						if(a instanceof ImageFullScreenActivity)
						{
							ImageFullScreenActivity act = ((ImageFullScreenActivity) a);

							if(act.visible) act.hideUI();
						}

						controller.show();

						return true;
					}
				});

				rl.addView(v);
			}
		});
	}
}
