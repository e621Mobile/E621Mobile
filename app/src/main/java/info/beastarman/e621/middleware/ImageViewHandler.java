package info.beastarman.e621.middleware;

import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

		return E621Middleware.decodeFile(in,width,height);
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