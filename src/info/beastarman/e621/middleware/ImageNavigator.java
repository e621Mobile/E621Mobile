package info.beastarman.e621.middleware;

public interface ImageNavigator
{
	ImageNavigator next();
	ImageNavigator prev();
	String getId();
}
