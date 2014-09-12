package info.beastarman.e621.middleware;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ImageViewHandler extends ImageHandler
{
	public ImageView imgView;
	public DisplayMetrics dm;
	
	private static Semaphore s = new Semaphore(3);
	
	public ImageViewHandler(ImageView imgView, View loader)
	{
		super(loader);
		this.imgView = imgView;
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
        
        try {
			s.acquire();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
        
        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize=scale;
        Bitmap bitmap_temp = BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, o2);
        
        if(width == bitmap_temp.getWidth() && height == bitmap_temp.getHeight())
        {
        	s.release();
        	
        	return bitmap_temp;
        }
        else
        {
	        Bitmap ret = Bitmap.createScaledBitmap(bitmap_temp,width,height,false);
	        
	        bitmap_temp.recycle();
	        
	        s.release();
	        
	        return ret;
        }
	}
	
	@Override
	public synchronized void handleInputStream(InputStream in)
	{
		try
		{
			Bitmap bitmap = decodeFile(in, imgView.getLayoutParams().width, imgView.getLayoutParams().height);
			
			this.imgView.setImageBitmap(bitmap);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			ViewGroup v = ((ViewGroup)this.imgView.getParent()); 
			
			v.removeView(this.imgView);
		}
	}
}