package info.beastarman.e621.frontend;

import info.beastarman.e621.R;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class ErrorReportActivity extends Activity
{
	public static String LOG = "log";
	
	String log;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_error_report);
		
		log = getIntent().getExtras().getString(LOG);
		
		if(log != null)
		{
			PackageManager manager = this.getPackageManager();
			PackageInfo info = null;
			
			try
			{
				info = manager.getPackageInfo (this.getPackageName(), 0);
			}
			catch(NameNotFoundException e2)
			{}
			
			String model = Build.MODEL;
			
			if (!model.startsWith(Build.MANUFACTURER))
			{
				model = Build.MANUFACTURER + " " + model;
			}
			
			log =	"Android version: " +  Build.VERSION.SDK_INT + "\n" + 
					"Model: " + model + "\n" + 
					"App version: " + (info == null ? "(null)" : info.versionCode) + "\n" + 
					log;
		}
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		
		if(log != null && !log.trim().equals(""))
		{
			TextView logArea = (TextView) findViewById(R.id.logcat);
			logArea.setText(log.replace("\n", "\n\n").trim());
		}
		
		ScrollView parentScrollView = (ScrollView) findViewById(R.id.parent_scroll);
		ScrollView childScrollView = (ScrollView) findViewById(R.id.child_scroll);
		
		parentScrollView.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				findViewById(R.id.child_scroll).getParent().requestDisallowInterceptTouchEvent(false);
		        return false;
			}
		});
		
		childScrollView.setOnTouchListener(new View.OnTouchListener()
		{

	        @Override
	        public boolean onTouch(View v, MotionEvent event)
	        {
		        v.getParent().requestDisallowInterceptTouchEvent(true);
		        return false;
		    }
		});
	}
	
	public void doNotSendReport(View v)
	{
		end();
	}
	
	public void sendReport(View v)
	{
		end();
	}
	
	@Override
	public void onBackPressed()
	{
		end();
	}
	
	public void end()
	{
		Intent i = new Intent(this,MainActivity.class);
		startActivity(i);
		
		finish();
	}
}
