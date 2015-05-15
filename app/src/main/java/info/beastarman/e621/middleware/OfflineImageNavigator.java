package info.beastarman.e621.middleware;

import android.util.Log;

import java.util.ArrayList;

public class OfflineImageNavigator extends ImageNavigator
{
	private static final long serialVersionUID = -1860597960377199668L;

	ArrayList<E621DownloadedImage> images = null;
	Long imagesKey = null;

	int position;
	String query;

	public OfflineImageNavigator(int position, String query)
	{
		this.position = position;
		this.query = query;
	}

	public OfflineImageNavigator(int position, String query, ArrayList<E621DownloadedImage> images)
	{
		this.images = images;
		this.position = position;
		this.query = query;
	}

	public OfflineImageNavigator(int position, String query, long imagesKey)
	{
		this.imagesKey = imagesKey;
		this.position = position;
		this.query = query;
	}

	private ArrayList<E621DownloadedImage> getImages()
	{
		if(images == null)
		{
			if(imagesKey != null)
			{
				images = (ArrayList<E621DownloadedImage>) E621Middleware.getInstance().getStorage().returnKey(imagesKey);
			}
			else
			{
				images = E621Middleware.getInstance().localSearch(0, -1, query);
			}
		}

		return images;
	}

	@Override
	public Integer getPosition()
	{
		if(position >= getImages().size())
		{
			position = getImages().size()-1;
		}

		if(position < 0)
		{
			position = 0;
		}

		return position;
	}

	@Override
	public Integer getCount()
	{
		return getImages().size();
	}

	@Override
	public ImageNavigator next()
	{
		if(getPosition() < getImages().size()-1)
		{
			return new OfflineImageNavigator(getPosition()+1,query,E621Middleware.getInstance().getStorage().rent(getImages()));
		}
		else
		{
			Log.d(E621Middleware.LOG_TAG,"Next " + getPosition() + " " + getImages().size());

			return null;
		}
	}

	@Override
	public ImageNavigator prev()
	{
		if(getPosition() > 0)
		{
			return new OfflineImageNavigator(getPosition()-1,query,E621Middleware.getInstance().getStorage().rent(getImages()));
		}
		else
		{
			Log.d(E621Middleware.LOG_TAG,"Prev " + getPosition() + " " + getImages().size());

			return null;
		}
	}
	
	@Override
	public Integer getId()
	{
		ArrayList<E621DownloadedImage> dImages = getImages();

		if(dImages.size() == 0)
		{
			return 0;
		}

		return dImages.get(getPosition()).getId();
	}
	
	public String toString()
	{
		return String.valueOf(getId());
	}
}
