package info.beastarman.e621.middleware;

import java.io.Serializable;

public class E621DownloadedImage implements Serializable
{
	private static final long serialVersionUID = 4777254945052251604L;
	
	public String filename = "";
	public int width = 1;
	public int height = 1;
	
	public E621DownloadedImage(String filename, int width, int height)
	{
		this.filename = filename;
		this.width = width;
		this.height = height;
	}
	
	public String getId()
	{
		return filename.split("\\.")[0];
	}
}
