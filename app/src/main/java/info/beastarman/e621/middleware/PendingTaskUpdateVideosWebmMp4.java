package info.beastarman.e621.middleware;

import java.util.ArrayList;

import info.beastarman.e621.backend.Pair;
import info.beastarman.e621.backend.PendingTask;

public class PendingTaskUpdateVideosWebmMp4 extends PendingTask
{
	E621Middleware e621;

	public PendingTaskUpdateVideosWebmMp4(E621Middleware e621)
	{
		this.e621 = e621;
	}

	@Override
	protected boolean runTask()
	{
		trigger(this);

		ArrayList<E621DownloadedImage> videos = e621.localSearch(0, -1, "type:webm");
		int i=0;

		for(E621DownloadedImage image : videos)
		{
			if(isCancelled())
			{
				return false;
			}
			else
			{
				e621.deleteImage(image.getId());
				e621.saveImage(image.getId());

				trigger(new Pair<Integer,Integer>(i,videos.size()));

				i++;
			}
		}

		return true;
	}
}
