package info.beastarman.e621.frontend;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.errorReport.ErrorReportReport;

/**
 * Created by beastarman on 10/27/2015.
 */
public class ErrorReportListActivity extends BaseActivity
{
	ArrayList<ErrorReportReport> reports;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_error_report_list_layout);

		reports = e621.getErrorReportManager().getReports();

		if(!reports.isEmpty())
		{
			findViewById(R.id.noReportsText).setVisibility(View.GONE);
			LinearLayout reportsView = (LinearLayout) findViewById(R.id.reportsLayout);

			for(ErrorReportReport report : reports)
			{
				reportsView.addView(generateReportView(report));
			}
		}
	}

	private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-d HH:mm:ss", Locale.US);

	View generateReportView(final ErrorReportReport report)
	{
		final View v = getLayoutInflater().inflate(R.layout.activity_error_report_list_item_layout,null,false);

		if(report.text == null || report.text.isEmpty())
		{
			report.text = "No description";
		}

		final TextView textView = (TextView)v.findViewById(R.id.text);
		textView.setText(report.text);

		if(report.time != null) ((TextView)v.findViewById(R.id.date)).setText(DATE_FORMAT.format(report.time));
		else v.findViewById(R.id.date).setVisibility(View.GONE);

		v.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				Intent i = new Intent(ErrorReportListActivity.this,ErrorReportMessagesActivity.class);
				i.putExtra(ErrorReportMessagesActivity.REPORT_HASH,report.hash);

				startActivity(i);
			}
		});

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				if(e621.getErrorReportManager().hasUnreadMessages(report.hash) > 0)
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							textView.setTypeface(null, Typeface.BOLD);

							LinearLayout ll = (LinearLayout) v.getParent();
							ll.removeView(v);
							ll.addView(v,0);
						}
					});
				}
			}
		}).start();

		return v;
	}
}
