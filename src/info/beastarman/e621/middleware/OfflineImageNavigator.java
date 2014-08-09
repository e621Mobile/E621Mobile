package info.beastarman.e621.middleware;

import java.util.ArrayList;

public class OfflineImageNavigator implements ImageNavigator
{
	private static final long serialVersionUID = -1860597960377199668L;
	
	E621DownloadedImage img;
	int position;
	String query;
	
	public OfflineImageNavigator(E621DownloadedImage img, int position, String query)
	{
		this.img = img;
		this.position = position;
		this.query = query;
	}
	
	@Override
	public ImageNavigator next()
	{
		ArrayList<E621DownloadedImage> ret = E621Middleware.getInstance(null).localSearch(position+1, 1, query);
		
		if(ret.size() > 0)
		{
			return new OfflineImageNavigator(ret.get(0),position+1,query);
		}
		
		return null;
	}

	@Override
	public ImageNavigator prev()
	{
		if(position <= 0)
		{
			return null;
		}
		
		ArrayList<E621DownloadedImage> ret = E621Middleware.getInstance(null).localSearch(position-1, 1, query);
		
		if(ret.size() > 0)
		{
			return new OfflineImageNavigator(ret.get(0),position-1,query);
		}
		
		return null;
	}
	
	@Override
	public Integer getId() {
		return img.getId();
	}
	
	public String toString()
	{
		return String.valueOf(getId());
	}
}
