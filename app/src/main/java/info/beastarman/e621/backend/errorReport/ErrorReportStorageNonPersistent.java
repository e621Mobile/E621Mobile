package info.beastarman.e621.backend.errorReport;

import java.util.ArrayList;

/**
 * Created by beastarman on 10/19/2015.
 */
public class ErrorReportStorageNonPersistent implements ErrorReportStorageInterface
{
	ArrayList<String> reports = new ArrayList<String>();

	@Override
	public void addReport(String report)
	{
		reports.add(report);
	}

	@Override
	public ArrayList<String> getReports()
	{
		return (ArrayList<String>) reports.clone();
	}
}
