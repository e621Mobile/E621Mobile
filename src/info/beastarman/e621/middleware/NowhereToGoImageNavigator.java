package info.beastarman.e621.middleware;

public class NowhereToGoImageNavigator implements ImageNavigator
{
	Integer id;
	
	public NowhereToGoImageNavigator(Integer id)
	{
		this.id = id;
	}

	@Override
	public ImageNavigator next()
	{
		return null;
	}

	@Override
	public ImageNavigator prev()
	{
		return null;
	}

	@Override
	public Integer getId()
	{
		return id;
	}

}
