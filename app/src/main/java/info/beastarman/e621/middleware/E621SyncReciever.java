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

		if(e621.syncOnlyOnWiFi() && !e621.isWifiConnected())
		{
			return;
		}

		Intent i = new Intent(context, E621SyncService.class);
		context.startService(i);
	}


}
