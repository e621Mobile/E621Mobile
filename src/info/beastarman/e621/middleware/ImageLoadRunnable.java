package info.beastarman.e621.middleware;

import info.beastarman.e621.api.E621Image;

import java.io.InputStream;

import android.os.Message;
import android.util.Log;

public class ImageLoadRunnable implements Runnable
{
	ImageHandler handler;
	E621Image img;
	E621Middleware e621;
	int size;
	
	public ImageLoadRunnable(ImageHandler handler, E621Image img, E621Middleware e621, int size)
	{
		this.handler = handler;
		this.img = img;
		this.e621 = e621;
		this.size = size;
	}
	
	@Override
	public void run() {
		InputStream in = e621.getImage(img, size);
    	Message msg = handler.obtainMessage();
    	msg.obj = in;
    	handler.sendMessage(msg);
	}
}