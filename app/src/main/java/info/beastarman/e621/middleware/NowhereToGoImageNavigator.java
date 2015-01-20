package info.beastarman.e621.middleware;

public class NowhereToGoImageNavigator extends ImageNavigator
{
	private static final long serialVersionUID = 8158998755587453735L;
	
	Integer id;
	
	public NowhereToGoImageNavigator(Integer id)
	{
		this.id = id;
	}

	@Override
	public Integer getPosition()
	{
		return 0;
	}

	@Override
	public Integer getCount()
	{
		return 1;
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
