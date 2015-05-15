package info.beastarman.e621.middleware;

import android.os.Message;

import java.io.InputStream;

import info.beastarman.e621.api.E621Image;

public class ImageLoadRunnable implements Runnable
{
	ImageHandler handler;
	E621Image img;
    int id;
	E621Middleware e621;
	int size;

    public ImageLoadRunnable(ImageHandler handler, E621Image img, E621Middleware e621, int size)
    {
        this.handler = handler;
        this.img = img;
        this.e621 = e621;
        this.size = size;
    }

    public ImageLoadRunnable(ImageHandler handler, int id, E621Middleware e621, int size)
    {
        this.handler = handler;
        this.id = id;
        this.e621 = e621;
        this.size = size;
    }
	
	@Override
	public void run()
	{
		InputStream in;

        if(img != null)
        {
            in = e621.getImage(img, size);
        }
        else
        {
            in = e621.getImage(id, size);
        }

        Message msg = handler.obtainMessage();
    	msg.obj = in;

    	handler.sendMessage(msg);
	}
}