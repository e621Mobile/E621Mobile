package info.beastarman.e621.frontend;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import info.beastarman.e621.R;

public class FeedbackActivity extends ErrorReportActivity
{
	@Override
	protected void onStart()
	{
		super.onStart();
		
		TextView title = (TextView) findViewById(R.id.title);
		title.setText("Send feedback.");
		
		TextView text = (TextView) findViewById(R.id.text);
		text.setText("Please use the area below to type your message. Usage statistics will be sent and analysed in case you are facing any problem.");
		
		EditText area = (EditText) findViewById(R.id.errorDescription);
		area.setHint("Type your message here.");
	}
	
	@Override
	public void sendReport(View v)
	{
		super.sendReport(v);
		
		Toast.makeText(this,"Thank you for the feedback!",Toast.LENGTH_SHORT).show();
	}
}
