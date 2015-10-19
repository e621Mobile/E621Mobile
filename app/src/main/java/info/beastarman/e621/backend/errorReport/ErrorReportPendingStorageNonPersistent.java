package info.beastarman.e621.backend.errorReport;

import java.util.ArrayList;

/**
 * Created by beastarman on 10/19/2015.
 */
public class ErrorReportPendingStorageNonPersistent implements ErrorReportPendingStorageInterface
{
	ArrayList<ErrorReportReport> reports = new ArrayList<ErrorReportReport>();

	@Override
	public void addReport(ErrorReportReport report)
	{
		reports.add(report);
	}

	@Override
	public ArrayList<ErrorReportReport> getReports()
	{
		return (ArrayList<ErrorReportReport>) reports.clone();
	}
}
