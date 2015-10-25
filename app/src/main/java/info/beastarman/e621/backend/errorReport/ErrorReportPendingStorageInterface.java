package info.beastarman.e621.backend.errorReport;

import java.util.ArrayList;

/**
 * Created by beastarman on 10/19/2015.
 */
public interface ErrorReportPendingStorageInterface
{
	void addReport(ErrorReportReport report);

	ArrayList<ErrorReportReport> getReports();

	void removeReport(String hash);
}
