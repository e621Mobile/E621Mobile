package info.beastarman.e621.backend.errorReport;

import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import info.beastarman.e621.backend.PersistentHttpClient;

/**
 * Created by beastarman on 10/17/2015.
 */
public class ErrorReportManager
{
	private File basePath;
	private static ErrorReportAPI api = new ErrorReportAPI(new PersistentHttpClient(new DefaultHttpClient(),3), "http://beastarman.info/report/");
	private String app_id;
	private String user = "user";
	ErrorReportStorageInterface errorReportStorage = new ErrorReportStorageNonPersistent();
	ErrorReportPendingStorageInterface pendingStorageInterface;

	public ErrorReportManager(String app_id, File basePath)
	{
		this.app_id = app_id;
		this.basePath = basePath;
		this.pendingStorageInterface = new ErrorReportPendingStorage(new File(basePath,"reportStorage/"));
	}

	public void sendReport(ErrorReportReport report)
	{
		try
		{
			ErrorReportReportResponse response = api.report(app_id, report.text, report.log, report.tags);

			if(response.isSuccessfull())
			{
				errorReportStorage.addReport(response.getHash());
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
					errorReportStorage.addReport(response.getHash());
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
