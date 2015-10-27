package info.beastarman.e621.frontend;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.errorReport.ErrorReportGetMessagesResponse;
import info.beastarman.e621.backend.errorReport.ErrorReportMessage;
import info.beastarman.e621.backend.errorReport.ErrorReportReport;

/**
 * Created by beastarman on 10/27/2015.
 */
public class ErrorReportMessagesActivity extends BaseActivity
{
	public static final String REPORT_HASH = "REPORT_HASH";
	ErrorReportReport report = null;
	ErrorReportGetMessagesResponse messages = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_error_report_messages_layout);

		report = e621.getErrorReportManager().getReport(getIntent().getStringExtra(REPORT_HASH));

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					messages = e621.getErrorReportManager().getMessages(report.hash);
				}
				catch(IOException e)
				{
				}

				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						showMessages();
					}
				});
			}
		}).start();
	}

	private void showMessages()
	{
		LinearLayout messageArea = (LinearLayout) findViewById(R.id.messageArea);

		messageArea.addView(getMessageView("Original report:\n\n" + report.text, true));

		if(messages != null)
		{
			for(ErrorReportMessage message : messages.getMessages())
			{
				messageArea.addView(getMessageView(message.text,message.author.equals("user")));
			}
		}
	}

	View getMessageView(String message, boolean fromUser)
	{
		View v = getLayoutInflater().inflate(R.layout.activity_error_report_messages_item_layout,null,false);

		((TextView)v.findViewById(R.id.message)).setText(message);

		return v;
	}

	public void sendMessage(final View _)
	{
		final String text = ((EditText)findViewById(R.id.messageInput)).getText().toString();
		LinearLayout messageArea = (LinearLayout) findViewById(R.id.messageArea);

		final View v = getMessageView(text, true);

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					e621.getErrorReportManager().sendMessage(report.hash,text);
				}
				catch(IOException e)
				{
					e.printStackTrace();

					((ViewGroup)v.getParent()).removeView(v);

					Toast.makeText(ErrorReportMessagesActivity.this,"COuld not send emssage",Toast.LENGTH_SHORT).show();
				}
			}
		}).start();

		messageArea.addView(v);
		((EditText)findViewById(R.id.messageInput)).setText("");
	}
}
