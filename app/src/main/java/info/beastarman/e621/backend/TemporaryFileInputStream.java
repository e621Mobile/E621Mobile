package info.beastarman.e621.backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TemporaryFileInputStream extends FileInputStream
{
	File file;

	public TemporaryFileInputStream(File file) throws FileNotFoundException
	{
		super(file);
		this.file = file;
	}

	long mark = -1;

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public synchronized void mark(int readlimit) {
		try {
			mark = getChannel().position();
		} catch (IOException ex)
		{
			mark = -1;
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		if (mark == -1) {
			throw new IOException("not marked");
		}
		getChannel().position(mark);
	}

	public void resetInputStream() throws IOException
	{
		getChannel().position(0);
	}

	@Override
	public void close()
	{
		try
		{
			super.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(file != null)
			{
				final File deleteme = file;
				file = null;

				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						deleteme.delete();
					}
				}).start();
			}
		}
	}
}
