package info.beastarman.e621.backend.errorReport;

import android.util.Log;

import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import info.beastarman.e621.backend.PersistentHttpClient;
import info.beastarman.e621.middleware.E621Middleware;

/**
 * Created by beastarman on 10/17/2015.
 */
public class ErrorReportManager
{
	private static ErrorReportAPI api = new ErrorReportAPI(new PersistentHttpClient(new DefaultHttpClient(),3), "http://beastarman.info/report/");
	private String app_id;
	private String user = "user";
	ErrorReportStorageInterface errorReportStorage = new ErrorReportStorageNonPersistent();
	ErrorReportPendingStorageInterface pendingStorageInterface;

	public ErrorReportManager(String app_id, File basePath)
	{
		this.app_id = app_id;
		basePath.mkdirs();
		this.pendingStorageInterface = new ErrorReportPendingStorage(new File(basePath,"pendingReportStorage/"));
		this.errorReportStorage = new ErrorReportStorage(new File(basePath,"reportStorage/"));
	}

	public void sendReport(ErrorReportReport report)
	{
		try
		{
			ErrorReportReportResponse response = api.report(app_id, report.text, report.log, report.tags);

			if(response.isSuccessfull())
			{
				report.hash = response.getHash();

				errorReportStorage.addReport(report);
			}
			else
			{
				Log.d(E621Middleware.LOG_TAG, "Oh crap");
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();

			pendingStorageInterface.addReport(report);
		}
	}

	public void sendPendingReports()
	{
		ArrayList<ErrorReportReport> reports = pendingStorageInterface.getReports();

		for(ErrorReportReport report : reports)
		{
			try
			{
				ErrorReportReportResponse response = api.report(app_id, report.text, report.log, report.tags);

				pendingStorageInterface.removeReport(report.hash);

				if(response.isSuccessfull())
				{
					report.hash = response.getHash();

					errorReportStorage.addReport(report);
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
