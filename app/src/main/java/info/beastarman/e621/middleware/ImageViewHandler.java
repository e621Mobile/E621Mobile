package info.beastarman.e621.middleware;

import android.app.ActionBar;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

import info.beastarman.e621.R;

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
		if(width == 0 || height == 0 || in == null)
		{
			return null;
		}

		try {
			s.acquire();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		byte[] bytes = null;

		try
		{
			bytes = IOUtils.toByteArray(in);
		}
		catch(IOException e)
		{
			e.printStackTrace();

			return null;
		}

		in = new ByteArrayInputStream(bytes);

		//Decode image size
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(in,null,o);

		try
		{
			in.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();

			return null;
		}

		height = (int) (width * (((double)o.outHeight) / o.outWidth));

		//Find the correct scale value. It should be the power of 2.
		int scale=1;
		while(((float)o.outWidth/scale)/2>=width && ((float)o.outHeight/scale)/2>=height)
		{
			scale*=2;
		}

		Bitmap bitmap_temp = null;

		InputStream in2 = new ByteArrayInputStream(bytes);

		//Decode with inSampleSize
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;
		bitmap_temp = BitmapFactory.decodeStream(in2, null, o2);

		try
		{
			in2.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		if(bitmap_temp == null)
		{
			s.release();

			return null;
		}

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
			int width = imgView.getLayoutParams().width;
			int height = imgView.getLayoutParams().height;
			
			double scale = Math.max(1,Math.max(width/2048,height/2048));
			
			Bitmap bitmap = decodeFile(in, (int)(width/scale), (int)(height/scale));

			if(bitmap != null)
			{
				ViewGroup.LayoutParams params = this.imgView.getLayoutParams();
				params.width = imgView.getWidth();
				params.height = (int) (params.width * (((double)bitmap.getHeight()) / bitmap.getWidth()));
				this.imgView.setLayoutParams(params);

				this.imgView.setImageBitmap(bitmap);
			}
			else
			{
				this.imgView.setImageResource(R.drawable.bad_image);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			ViewGroup v = ((ViewGroup)this.imgView.getParent()); 
			
			v.removeView(this.imgView);
		}
	}
}