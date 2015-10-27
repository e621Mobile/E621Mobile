package info.beastarman.e621.backend.errorReport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by beastarman on 10/17/2015.
 */
public class ErrorReportManager
{
	private static ErrorReportAPI api = new ErrorReportAPI("http://beastarman.info/report/");
	private String app_id;
	private String user = "user";
	ErrorReportStorageInterface errorReportStorage;
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

	public ArrayList<ErrorReportMessage> getAndUpdateUnreadMessages()
	{
		return getUnreadMessages(true);
	}

	public ArrayList<ErrorReportMessage> getUnreadMessages(boolean update)
	{
		ArrayList<ErrorReportReport> reports = errorReportStorage.getReports();
		ArrayList<ErrorReportMessage> newMessages = new ArrayList<ErrorReportMessage>();

		for(ErrorReportReport report : reports)
		{
			try
			{
				ErrorReportGetMessagesResponse response = api.getMessages(report.hash);

				int lastId = errorReportStorage.getLastMessageID(report.hash);

				newMessages.addAll(response.messages.subList(lastId,response.maxId));

				if(update) errorReportStorage.updateLastMessageID(report.hash, response.maxId);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

		return newMessages;
	}

	public int hasUnreadMessages(String report)
	{
		int ret = 0;

		try
		{
			ErrorReportGetMessagesResponse response = api.getMessages(report);

			ret = response.maxId - errorReportStorage.getLastMessageID(report);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		return ret;
	}

	public ArrayList<ErrorReportReport> getReports()
	{
		ArrayList<ErrorReportReport> ret = errorReportStorage.getReports();

		Collections.sort(ret, new Comparator<ErrorReportReport>()
		{
			@Override
			public int compare(ErrorReportReport a, ErrorReportReport b)
			{
				long _a = 0;
				long _b = 0;

				if(a.time != null) _a = a.time.getTime();
				if(b.time != null) _b = b.time.getTime();

				return (int) (_b - _a);
			}
		});

		return ret;
	}

	public ErrorReportReport getReport(String reportHash)
	{
		return errorReportStorage.getReport(reportHash);
	}

	public ErrorReportGetMessagesResponse getMessages(String hash) throws IOException
	{
		return api.getMessages(hash);
	}
}
