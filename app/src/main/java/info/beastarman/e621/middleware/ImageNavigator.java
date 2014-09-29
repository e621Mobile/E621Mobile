package info.beastarman.e621.middleware;

import java.io.Serializable;

public interface ImageNavigator extends Serializable
{
	ImageNavigator next();
	ImageNavigator prev();
	Integer getId();
}
