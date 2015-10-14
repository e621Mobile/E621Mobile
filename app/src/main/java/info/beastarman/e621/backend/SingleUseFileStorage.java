package info.beastarman.e621.backend;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by beastarman on 10/13/2015.
 */
public class SingleUseFileStorage
{
	File basePath;

	public SingleUseFileStorage(File basePath)
	{
		this.basePath = basePath;

		for(File f : basePath.listFiles())
		{
			f.delete();
		}
	}

	public TemporaryFileInputStream store(InputStream source) throws IOException
	{
		File f;

		do
		{
			f = new File(basePath, UUID.randomUUID().toString());
		}
		while(f.exists());

		f.createNewFile();

		OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
		IOUtils.copy(source,out);
		out.close();

		return new TemporaryFileInputStream(f);
	}

	public SingleUseFile getSingleUseFile()
	{
		File f;

		do
		{
			f = new File(basePath, UUID.randomUUID().toString());
		}
		while(f.exists());

		try
		{
			f.createNewFile();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}

		return new SingleUseFile(f);
	}

	public static class SingleUseFile
	{
		File file;

		public SingleUseFile(File file)
		{
			this.file = file;
		}

		public FileOutputStream getOutputStream() throws FileNotFoundException
		{
			return new FileOutputStream(file);
		}

		public TemporaryFileInputStream getInputStream() throws FileNotFoundException
		{
			TemporaryFileInputStream in = new TemporaryFileInputStream(file);
			file = null;
			return in;
		}
	}
}
