package info.beastarman.e621.views;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.AttributeSet;
import android.widget.VideoView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MediaInputStreamPlayer extends MediaPlayer
{
	static File directory = null;
	final String UNIQUE_ID = "1KB6R4492MA4VYN05QKI";
	File _f = null;
	InputStreamCheckListener inputStreamCheckListener = null;

	public MediaInputStreamPlayer()
	{
		super();

		synchronized(MediaInputStreamPlayer.class)
		{
			if(directory == null)
			{
				directory = new File(Environment.getExternalStoragePublicDirectory("VideoInputStreamView"), UNIQUE_ID);

				if(!directory.exists())
				{
					directory.mkdirs();
				}

				for(File f : directory.listFiles())
				{
					f.delete();
				}
			}
		}
	}

	private File getNewFile() throws IOException
	{
		if(_f != null && _f.exists())
		{
			_f.delete();
		}

		_f = new File(directory, UUID.randomUUID().toString());
		if(_f.exists())
		{
			_f.delete();
		}

		_f.createNewFile();

		return _f;
	}

	public String getFilePath()
	{
		if(_f != null)
		{
			return _f.getAbsolutePath();
		}

		return null;
	}

	public void setVideoInputStream(final InputStream in) throws IOException
	{
		final File f = getNewFile();

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				byte[] buffer = new byte[1024];
				int len;

				try
				{
					OutputStream out = new BufferedOutputStream(new FileOutputStream(f));

					while((len = in.read(buffer)) != -1)
					{
						out.write(buffer, 0, len);
					}

					out.close();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}

				try
				{
					setDataSource(f.getAbsolutePath());
					prepareAsync();

					if(inputStreamCheckListener != null)
					{
						inputStreamCheckListener.onCheck(true);
					}
				}
				catch(IOException e)
				{
					e.printStackTrace();

					if(inputStreamCheckListener != null)
					{
						inputStreamCheckListener.onCheck(false);
					}
				}
			}
		}).start();
	}

	public void close()
	{
		seekTo(0);
		stop();
		release();

		if(_f != null && _f.exists())
		{
			_f.delete();
		}
	}

	public void setInputStreamCheckListener(InputStreamCheckListener inputStreamCheckListener)
	{
		this.inputStreamCheckListener = inputStreamCheckListener;
	}

	public static abstract class InputStreamCheckListener
	{
		public abstract void onCheck(boolean isOk);
	}
}
