package info.beastarman.e621.middleware;

import java.io.Serializable;

public class ImageNavilagorLazyRelative implements Serializable
{
	ImageNavigator imageNavigator;
	int pos;

	public ImageNavilagorLazyRelative(ImageNavigator imageNavigator, int pos)
	{
		this.imageNavigator = imageNavigator;
		this.pos = pos;
	}

	public ImageNavigator getImageNavigator()
	{
		return imageNavigator.getRelative(pos);
	}
}
