package info.beastarman.e621.frontend;

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
	
	public ImageViewHandler(ImageView imgView, View loader)
	{
		this.imgView = imgView;
		this.loader = loader;
	}
	
	@Override
	public void handleMessage(Message msg)
	{
		try
		{
			InputStream in = (InputStream)msg.obj;
			Bitmap bitmap = BitmapFactory.decodeStream(in);
			in.close();
			
			int width = imgView.getLayoutParams().width;
			int height = imgView.getLayoutParams().height;
			
			this.imgView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, width, height, false));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			ViewGroup v = ((ViewGroup)this.imgView.getParent()); 
			
			v.removeView(this.imgView);
		}
		finally
		{
			if(this.loader != null)
			{
				ViewGroup v = ((ViewGroup)this.loader.getParent()); 
				
				v.removeView(this.loader);
			}
		}
	}
}