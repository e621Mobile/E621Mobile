package info.beastarman.e621.frontend;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.io.IOException;

import info.beastarman.e621.R;
import info.beastarman.e621.views.VideoControllerView;

public class FullScreenVideoActivity extends BaseActivity implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener, VideoControllerView.MediaPlayerControl
{
	public static final String VIDEO_PATH = "VIDEO_PATH";
	public static final String VIDEO_POSITION = "VIDEO_POSITION";
	public static final int RESULT_VIDEO_POSITION = 357;

	SurfaceView videoSurface;
	MediaPlayer player;
	VideoControllerView controller;

	String path;
	int position;

	int screenWidth;
	int screenHeight;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_screen_video);

		path = getIntent().getStringExtra(VIDEO_PATH);

		if(path == null)
		{
			finish();
			return;
		}

		position = getIntent().getIntExtra(VIDEO_POSITION, 0);

		int visibility = View.SYSTEM_UI_FLAG_FULLSCREEN
								 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
								 | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
								 | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
								 | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

		if(Build.VERSION.SDK_INT > 18)
		{
			visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}

		getWindow().getDecorView().setSystemUiVisibility(visibility);

		screenWidth = getWindowManager().getDefaultDisplay().getWidth();
		screenHeight = getWindowManager().getDefaultDisplay().getHeight();

		videoSurface = (SurfaceView) findViewById(R.id.videoSurface);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		findViewById(R.id.videoSurfaceContainer).setLayoutParams(params);

		SurfaceHolder videoHolder = videoSurface.getHolder();
		videoHolder.addCallback(this);

		player = new MediaPlayer();
		controller = new VideoControllerView(this);

		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try
		{
			player.setDataSource(path);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		player.setOnPreparedListener(this);

		videoSurface.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent)
			{
				controller.show();

				return true;
			}
		});
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		controller.show();
		return false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		player.setDisplay(holder);
		player.prepareAsync();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{

	}

	@Override
	public void onPrepared(MediaPlayer mp)
	{
		controller.setMediaPlayer(this);
		controller.setAnchorView((FrameLayout) findViewById(R.id.videoSurfaceContainer));
		player.start();
		player.seekTo(position);
		player.setLooping(true);
		controller.show(0);

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

	@Override
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
		return player.getCurrentPosition();
	}

	@Override
	public int getDuration()
	{
		return player.getDuration();
	}

	@Override
	public boolean isPlaying()
	{
		return player.isPlaying();
	}

	@Override
	public void pause()
	{
		player.pause();
	}

	@Override
	public void seekTo(int i)
	{
		player.seekTo(i);
	}

	@Override
	public void start()
	{
		player.start();
	}

	@Override
	public boolean isFullScreen()
	{
		return true;
	}

	@Override
	public void toggleFullScreen()
	{
		Intent result = new Intent();
		result.putExtra(VIDEO_POSITION, player.getCurrentPosition());
		setResult(RESULT_VIDEO_POSITION, result);

		finish();
	}

	@Override
	public void onBackPressed()
	{
		Intent result = new Intent();
		result.putExtra(VIDEO_POSITION, player.getCurrentPosition());
		setResult(RESULT_VIDEO_POSITION, result);

		super.onBackPressed();
	}
}