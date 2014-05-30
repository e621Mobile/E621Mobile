package info.beastarman.e621.frontend;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class BaseActivity extends Activity implements UncaughtExceptionHandler
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Thread.setDefaultUncaughtExceptionHandler(this);
	}
	
	@Override
	public void uncaughtException (Thread thread, Throwable e)
	{
		e.printStackTrace();
		
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
