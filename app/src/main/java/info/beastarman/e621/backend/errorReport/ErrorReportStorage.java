package info.beastarman.e621.backend.errorReport;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by beastarman on 10/26/2015.
 */
public class ErrorReportStorage implements ErrorReportStorageInterface
{
	File basePath;
	private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-d HH:mm:ss.SZZZZZ", Locale.US);

	public ErrorReportStorage(File basePath)
	{
		this.basePath = basePath;
		this.basePath.mkdirs();
	}

	@Override
	public void addReport(ErrorReportReport report)
	{
		JSONObject jsonObject = new JSONObject();

		try
		{
			jsonObject.put("text",report.text);
			jsonObject.put("log",report.log);
			jsonObject.put("hash",report.hash);
			jsonObject.put("time",DATE_FORMAT.format(report.time));

			JSONArray jsonArray = new JSONArray();

			for(String tag : report.tags)
			{
				jsonArray.put(tag);
			}

			jsonObject.put("tags",jsonArray);
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}

		File output = new File(basePath,report.hash);

		try
		{
			output.createNewFile();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		try
		{
			OutputStream out = new BufferedOutputStream(new FileOutputStream(output));
			out.write(jsonObject.toString().getBytes());
			out.close();
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public ArrayList<ErrorReportReport> getReports()
	{
		File[] files = basePath.listFiles(new FilenameFilter()
		{
			@Override
			public boolean accept(File file, String s)
			{
				return !s.startsWith(".");
			}
		});

		ArrayList<ErrorReportReport> reports = new ArrayList<ErrorReportReport>();

		for(File f : files)
		{
			try
			{
				InputStream is = new BufferedInputStream(new FileInputStream(f));
				reports.add(new ErrorReportReport(new JSONObject(IOUtils.toString(is))));
				is.close();
			}
			catch(FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			catch(JSONException e)
			{
				e.printStackTrace();
			}
		}

		return reports;
	}

	@Override
	public int getLastMessageID(String reportHash)
	{
		try
		{
			File f = new File(basePath,reportHash);

			if(!f.exists()) return 0;

			InputStream is = new BufferedInputStream(new FileInputStream(f));
			JSONObject jsonObject = new JSONObject(IOUtils.toString(is));
			is.close();

			return jsonObject.optInt("lastMessageID",0);
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}

		return 0;
	}

	@Override
	public void updateLastMessageID(String reportHash, int messageID)
	{
		try
		{
			File f = new File(basePath,reportHash);

			if(!f.exists()) return;

			InputStream is = new BufferedInputStream(new FileInputStream(f));
			ErrorReportReport report = new ErrorReportReport(new JSONObject(IOUtils.toString(is)));
			is.close();

			OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
			out.write(generateJsonObject(report,messageID).toString().getBytes());
			out.close();
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public ErrorReportReport getReport(String reportHash)
	{
		try
		{
			File f = new File(basePath,reportHash);

			if(!f.exists()) return null;

			InputStream is = new BufferedInputStream(new FileInputStream(f));
			JSONObject jsonObject = new JSONObject(IOUtils.toString(is));
			is.close();

			return new ErrorReportReport(jsonObject);
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	private JSONObject generateJsonObject(ErrorReportReport report)
	{
		return generateJsonObject(report,0);
	}

	private JSONObject generateJsonObject(ErrorReportReport report, int lastMessageID)
	{
		JSONObject jsonObject = null;

		try
		{
			jsonObject = new JSONObject();

			jsonObject.put("text",report.text);
			jsonObject.put("log",report.log);
			jsonObject.put("hash",report.hash);
			jsonObject.put("time",DATE_FORMAT.format(report.time!=null?report.time:new Date()));
			jsonObject.put("lastMessageID",lastMessageID);

			JSONArray jsonArray = new JSONArray();

			for(String tag : report.tags)
			{
				jsonArray.put(tag);
			}

			jsonObject.put("tags",jsonArray);
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}

		return jsonObject;
	}
}
