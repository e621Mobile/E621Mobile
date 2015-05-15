package info.beastarman.e621.api.dtext;

import android.graphics.Bitmap;

import java.io.IOException;
import java.io.InputStream;

import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.middleware.E621Middleware;

public class DTextThumb extends DTextObject
{
	public int id;

	public DTextThumb(final int id)
	{
		this.id = id;
	}

	E621Image img = null;
	public synchronized E621Image getImage() throws IOException
	{
		if(img == null)
		{
			img = E621Middleware.getInstance().post__show(id);
		}

		return img;
	}

	public Bitmap getBitmap(int w, int h)
	{
		E621Middleware e621 = E621Middleware.getInstance();
		InputStream is = null;

		try
		{
			is = e621.getImage(getImage(), E621Image.PREVIEW);

			return e621.decodeFile(is,w,h);
		}
		catch (IOException e)
		{
			e.printStackTrace();

			return null;
		}
		finally
		{
			if(is != null)
			{
				try
				{
					is.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}
