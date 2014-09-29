package info.beastarman.e621.middleware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class E621SyncReciever extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		final E621Middleware e621 = E621Middleware.getInstance(context);
		
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				if(e621.syncOnlyOnWiFi() && !e621.isWifiConnected())
				{
					return;
				}
				
				e621.sync();
			}
		}).start();
	}
}
