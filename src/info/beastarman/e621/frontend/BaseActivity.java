package info.beastarman.e621.frontend;

import info.beastarman.e621.middleware.E621Middleware;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

public class BaseActivity extends Activity implements UncaughtExceptionHandler
{
	public E621Middleware e621;
	
	protected int dpToPx(int dp)
	{
	    DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
	    int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));       
	    return px;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		e621 = E621Middleware.getInstance(getApplicationContext());
		
		Thread.setDefaultUncaughtExceptionHandler(this);
	}
	
	@Override
	public void uncaughtException (Thread thread, Throwable e)
	{
		Log.e("Msg",Log.getStackTraceString(e)); 
		
		try {
			String[] get_pid = {
				"sh",
				"-c",
				"ps | grep info.beastarman.e621 | cut -c10-15"
			};
			
			Process process = Runtime.getRuntime().exec(get_pid);
			String pid = IOUtils.toString(process.getInputStream());
			
			String[] get_log = {
				"sh",
				"-c",
				"logcat -d -v time | grep " + pid + " 2> /dev/null"
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
}
