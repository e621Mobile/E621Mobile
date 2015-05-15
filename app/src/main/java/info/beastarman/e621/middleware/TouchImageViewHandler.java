package info.beastarman.e621.middleware;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

import info.beastarman.e621.views.TouchImageView;

public class TouchImageViewHandler extends ImageHandler
{
	private static Semaphore s = new Semaphore(3);
	public TouchImageView imgView;
	public int w;
	public int h;

	public TouchImageViewHandler(TouchImageView imgView, View loader, int _w, int _h)
	{
		super(loader);
		this.imgView = imgView;
		w = _w;
		h = _h;
	}

	private Bitmap decodeFile(InputStream in, int width, int height)
	{
		if(width == 0 || height == 0)
		{
			return null;
		}

		try
		{
			s.acquire();
		}
		catch(InterruptedException e)
		{
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
		BitmapFactory.decodeStream(in, null, o);

		try
		{
			in.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();

			return null;
		}

		//Find the correct scale value. It should be the power of 2.
		int scale = 1;
		while(((float) o.outWidth / scale) / 2 >= width && ((float) o.outHeight / scale) / 2 >= height)
		{
			scale *= 2;
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
		}
		catch(IOException e)
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
			Bitmap ret = Bitmap.createScaledBitmap(bitmap_temp, width, height, false);

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
			int width = w;
			int height = h;

			double scale = Math.max(1, Math.max(width / 2048, height / 2048));

			Bitmap bitmap = decodeFile(in, (int) (width / scale), (int) (height / scale));

			this.imgView.setImageBitmap(bitmap);

			this.imgView.invalidate();
			this.imgView.setZoom(0.9999999f);
		}
		catch(Exception e)
		{
			e.printStackTrace();

			ViewGroup v = ((ViewGroup) this.imgView.getParent());

			v.removeView(this.imgView);
		}
	}
}