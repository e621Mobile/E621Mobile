package info.beastarman.e621.middleware;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
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
	
	private Bitmap decodeFile(InputStream in, int width, int height)
	{
		byte[] data;
		
		try {
			data = IOUtils.toByteArray(in);
		} catch (IOException e) {
			return null;
		}
		
        //Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(new ByteArrayInputStream(data),null,o);

        //Find the correct scale value. It should be the power of 2.
        int scale=1;
        while(o.outWidth/scale/2>=width && o.outHeight/scale/2>=height)
        {
        	scale*=2;
        }
        
        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize=scale;
        Bitmap bitmap_temp = BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, o2);
        
        return Bitmap.createScaledBitmap(bitmap_temp,width,height,false);
	}
	
	@Override
	public void handleMessage(Message msg)
	{
		try
		{
			InputStream in = (InputStream)msg.obj;
			Bitmap bitmap = decodeFile(in, imgView.getLayoutParams().width, imgView.getLayoutParams().height);
			in.close();
			
			this.imgView.setImageBitmap(bitmap);
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