package info.beastarman.e621.middleware;

import android.os.Handler;
import android.os.Message;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;

public class ImageHandler extends Handler
{
	public View loader;
	
	public ImageHandler(View loader)
	{
		this.loader = loader;
	}
	
	protected void handleInputStream(InputStream in)
	{
	}
	
	@Override
	public void handleMessage(Message msg)
	{
		InputStream in = (InputStream) msg.obj;
		
		handleInputStream(in);

		try
		{
			if(in != null)
			{
				in.close();
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		if(this.loader != null)
		{
			this.loader.setVisibility(View.GONE);
		}
	}
}