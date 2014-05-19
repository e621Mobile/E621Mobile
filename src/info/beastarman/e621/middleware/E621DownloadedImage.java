package info.beastarman.e621.middleware;

public class E621DownloadedImage
{
	public String filename = "";
	public int width = 1;
	public int height = 1;
	
	public E621DownloadedImage(String filename, int width, int height)
	{
		this.filename = filename;
		this.width = width;
		this.height = height;
	}
}
