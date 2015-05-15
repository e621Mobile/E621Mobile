package info.beastarman.e621.middleware;

import java.io.Serializable;

public abstract class ImageNavigator implements Serializable
{
	public ImageNavigator getRelative(int diff)
	{
		ImageNavigator in = this;

		while(diff > 0)
		{
			in = in.next();

			diff--;
		}

		while(diff < 0)
		{
			in = in.prev();

			diff++;
		}

		return in;
	}

	public abstract Integer getPosition();
	public abstract Integer getCount();

	public abstract ImageNavigator next();
	public abstract ImageNavigator prev();
	public abstract Integer getId();
}
