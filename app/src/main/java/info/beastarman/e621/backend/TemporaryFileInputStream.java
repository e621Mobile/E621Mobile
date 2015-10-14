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

	public void resetInputStream() throws IOException
	{
		getChannel().position(0);
	}

	@Override
	public void close() throws IOException
	{
		try
		{
			super.close();
		}
		finally
		{
			if(file != null)
			{
				file.delete();
				file = null;
			}
		}
	}
}
