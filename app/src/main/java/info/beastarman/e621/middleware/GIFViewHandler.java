package info.beastarman.e621.middleware;

import info.beastarman.e621.views.GIFView;

import java.io.InputStream;

import android.view.View;
import android.view.ViewGroup;

public class GIFViewHandler extends ImageHandler
{
	GIFView gifView;
	
	public GIFViewHandler(GIFView gifView, View loader)
	{
		super(loader);
		this.gifView = gifView;
	}
	
	@Override
	public void handleInputStream(InputStream in)
	{
		try
		{
			gifView.initializeView(in);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			ViewGroup v = ((ViewGroup)this.gifView.getParent()); 
			
			v.removeView(this.gifView);
		}
	}
}
