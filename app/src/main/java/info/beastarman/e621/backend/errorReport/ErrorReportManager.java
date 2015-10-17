package info.beastarman.e621.backend.errorReport;

import org.apache.http.impl.client.DefaultHttpClient;

import info.beastarman.e621.backend.PersistentHttpClient;

/**
 * Created by beastarman on 10/17/2015.
 */
public class ErrorReportManager
{
	private static ErrorReportAPI api = new ErrorReportAPI(new PersistentHttpClient(new DefaultHttpClient(),3), "http://beastarman.info/report/");
	private String app_id;

	public ErrorReportManager(String app_id)
	{
		this.app_id = app_id;
	}


}
