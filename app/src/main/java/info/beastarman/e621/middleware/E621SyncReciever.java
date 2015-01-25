package info.beastarman.e621.middleware;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import info.beastarman.e621.R;

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

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setContentTitle("E621 Sync");
		builder.setContentText("Sync in progress");
		builder.setOngoing(true);

		final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(R.id.syncNotificationId,builder.build());
		
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					e621.sync();
				}
				finally
				{
					notificationManager.cancel(R.id.syncNotificationId);
				}
			}
		}).start();
	}
}
