package info.beastarman.e621;

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ImageViewHandler extends Handler
{
	public ImageView imgView;
	public DisplayMetrics dm;
	public View loader;
	
	public ImageViewHandler(ImageView imgView, DisplayMetrics dm, View loader)
	{
		this.imgView = imgView;
		this.dm = dm;
		this.loader = loader;
	}
	
	@Override
	public void handleMessage(Message msg)
	{
		try
		{
			InputStream in = (InputStream)msg.obj;
			Bitmap bitmap = BitmapFactory.decodeStream(in);
			
			int width = dm.widthPixels;
			int height = width * bitmap.getHeight() / bitmap.getWidth();
			
			this.imgView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, width, height, false));
		}
		catch (Exception e)
		{
			((ViewGroup)this.imgView.getParent()).removeView(this.imgView);
		}
		finally
		{
			if(this.loader != null)
			{
				((ViewGroup)this.loader.getParent()).removeView(this.loader);
			}
		}
	}
}