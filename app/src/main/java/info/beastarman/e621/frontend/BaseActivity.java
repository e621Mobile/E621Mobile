package info.beastarman.e621.frontend;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Set;

import info.beastarman.e621.middleware.E621Middleware;

public class BaseActivity extends Activity implements UncaughtExceptionHandler
{
	public E621Middleware e621;
	
	protected int dpToPx(int dp)
	{
	    DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
	    int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));       
	    return px;
	}
	
	private String safeObjToStr(Object obj)
	{
		if(obj == null) return "null";
		
		return obj.toString();
	}
	
	private String safeObjToClassName(Object obj)
	{
		if(obj == null) return "null";
		
		return obj.getClass().getName();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        String analyticsPath = this.getClass().getName()+"?";

        Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onCreate() " + this.getClass().getName());
		
		Intent intent = getIntent();
		if(intent != null)
		{
			Bundle bundle = intent.getExtras();
			
			if(bundle != null)
			{
				Set<String> keys = bundle.keySet();
				
				for(String key : keys)
				{
					Object value = bundle.get(key);
					Log.i(E621Middleware.LOG_TAG, "\t" + key + ": <" + safeObjToStr(value) + "> from class <" + safeObjToClassName(value) + ">");

                    analyticsPath += key + "=" + safeObjToStr(value) + "&";
				}
			}
		}

        Tracker t = ((E621Application) getApplication()).getTracker();

        t.setScreenName(analyticsPath.substring(0,analyticsPath.length()-2));

        t.send(new HitBuilders.AppViewBuilder().build());
		
		super.onCreate(savedInstanceState);
		
		e621 = E621Middleware.getInstance(this);
		
		Thread.setDefaultUncaughtExceptionHandler(this);
	}
	
	@Override
	protected void onDestroy()
	{
		Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onDestroy() " + this.getClass().getName());
		
		super.onDestroy();
	}
	
	@Override
	protected void onStart()
	{
		Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onStart() " + this.getClass().getName());
		
		super.onStart();
	}
	
	@Override
	protected void onStop()
	{
		Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onStop() " + this.getClass().getName());
		
		super.onStop();
	}
	
	@Override
	protected void onResume()
	{
		Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onResume() " + this.getClass().getName());
		
		super.onResume();
	}
	
	@Override
	protected void onPause()
	{
		Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onPause() " + this.getClass().getName());
		
		super.onPause();
	}
	
	@Override
	protected void onRestart()
	{
		Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onRestart() " + this.getClass().getName());
		
		super.onRestart();
	}
	
	public void uncaughtException()
	{
		try {
			String[] get_pid = {
				"sh",
				"-c",
				"ps | grep info.beastarman.e621"
			};
			
			Process process = Runtime.getRuntime().exec(get_pid);
			String pid = IOUtils.toString(process.getInputStream());
			
			pid = pid.substring(10,15);
			
			String[] get_log = {
				"sh",
				"-c",
				"logcat -d -v time | grep -e " + pid + " -e " + E621Middleware.LOG_TAG + " 2> /dev/null"
			};
			
			process = Runtime.getRuntime().exec(get_log);
			String log = IOUtils.toString(process.getInputStream());
			
			Intent intent = new Intent(getApplicationContext(), ErrorReportActivity.class);
			intent.putExtra(ErrorReportActivity.LOG, log);
			startActivity(intent);
		} catch (IOException e1)
		{
			e1.printStackTrace();
		}
		finally
		{
			System.exit(0);
		}
	}
	
	@Override
	public void uncaughtException(Thread thread, Throwable e)
	{
		Log.e(E621Middleware.LOG_TAG + "_Exception",Log.getStackTraceString(e));
		
		uncaughtException();
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
        
        Bitmap ret = Bitmap.createScaledBitmap(bitmap_temp,width,height,false);
        
        bitmap_temp.recycle();
        
        return ret;
	}
	
	private Bitmap decodeFile(Bitmap bmp, int width, int height)
	{
		if(width == bmp.getWidth() && height == bmp.getHeight())
		{
			return bmp.copy(bmp.getConfig(),false);
		}
		
		Bitmap ret = Bitmap.createScaledBitmap(bmp,width,height,false);
        
        return ret;
	}
	
	public void drawInputStreamToImageView(final InputStream in, final ImageView imgView)
	{
		final ImageViewHandler handler = new ImageViewHandler(imgView);
		
		new Thread(new Runnable()
		{
			public void run()
			{
				Bitmap bitmap = decodeFile(in, imgView.getLayoutParams().width, imgView.getLayoutParams().height);
				
				Message msg = handler.obtainMessage();
		    	msg.obj = bitmap;
		    	handler.sendMessage(msg);
			}
		}).start();
	}
	
	public void drawInputStreamToImageView(final Bitmap bmp, final ImageView imgView)
	{
		final ImageViewHandler handler = new ImageViewHandler(imgView);
		
		new Thread(new Runnable()
		{
			public void run()
			{
				Bitmap bitmap = decodeFile(bmp, imgView.getLayoutParams().width, imgView.getLayoutParams().height);
				bmp.recycle();
				
				Message msg = handler.obtainMessage();
		    	msg.obj = bitmap;
		    	handler.sendMessage(msg);
			}
		}).start();
	}
	
	private static class ImageViewHandler extends Handler
	{
		private ImageView imgView;
		
		public ImageViewHandler(ImageView imgView)
		{
			this.imgView = imgView;
		}
		
		public void handleMessage(Message msg)
		{
			this.imgView.setBackgroundResource(0);
			this.imgView.setImageBitmap((Bitmap)msg.obj);
		}
	}
}
