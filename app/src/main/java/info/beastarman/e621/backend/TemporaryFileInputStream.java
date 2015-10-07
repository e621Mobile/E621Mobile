package info.beastarman.e621.backend;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import info.beastarman.e621.middleware.E621Middleware;

public class TemporaryFileInputStream extends FileInputStream
{
	File file;

	public TemporaryFileInputStream(File file) throws FileNotFoundException
	{
		super(file);
		this.file = file;
	}

	public TemporaryFileInputStream(String path) throws FileNotFoundException
	{
		this(new File(path));
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
