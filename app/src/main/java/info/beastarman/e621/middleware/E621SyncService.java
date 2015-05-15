package info.beastarman.e621.middleware;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.backend.Pair;

public class E621SyncService extends IntentService
{
	public E621SyncService()
	{
		super("E621SyncService");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, startId, startId);

		return START_STICKY;
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		Context context = getApplicationContext();

		final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		try
		{
			final E621Middleware e621 = E621Middleware.getInstance(context);

			final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

			builder.setSmallIcon(R.drawable.ic_launcher);
			builder.setContentTitle("E621 Sync");
			builder.setContentText("Sync in progress");
			builder.setOngoing(false);

			notificationManager.notify(R.id.syncNotificationId, builder.build());

			EventManager em = new EventManager()
			{
				String lastMsg = "Sync in progress";
				String extra = "";

				@Override
				public void onTrigger(Object obj)
				{
					if (obj instanceof E621Middleware.SyncState)
					{
						if (obj == E621Middleware.SyncState.REPORTS)
						{
							lastMsg = "Sending remaining reports";
						}
						else if (obj == E621Middleware.SyncState.FAILED_DOWNLOADS)
						{
							lastMsg = "Fixing failed downloads";
						}
						else if (obj == E621Middleware.SyncState.CHECKING_FOR_UPDATES)
						{
							lastMsg = "Checking for updates";
						}
						else if (obj == E621Middleware.SyncState.BACKUP)
						{
							lastMsg = "Creating new backup";
						}
						else if (obj == E621Middleware.SyncState.INTERRUPTED_SEARCHES)
						{
							lastMsg = "Updating interrupted searches";
						}
						else if (obj == E621Middleware.SyncState.FINISHED)
						{
							notificationManager.cancel(R.id.syncNotificationId);

							return;
						}
					}
					else if (obj instanceof E621DownloadedImages.UpdateStates)
					{
						if (obj == E621DownloadedImages.UpdateStates.CLEANING)
						{
							lastMsg = "Cleaning metadata";
						}
						else if (obj == E621DownloadedImages.UpdateStates.TAG_SYNC)
						{
							lastMsg = "Synchronizing tags";
						}
						else if (obj == E621DownloadedImages.UpdateStates.TAG_ALIAS_SYNC)
						{
							lastMsg = "Synchronizing tag aliases";
						}
						else if (obj == E621DownloadedImages.UpdateStates.IMAGE_TAG_SYNC)
						{
							lastMsg = "Synchronizing image tags";
						}
						else if (obj == E621DownloadedImages.UpdateStates.IMAGE_TAG_DB)
						{
							lastMsg = "Saving image tags into database";
						}
					}

					if (obj instanceof Pair)
					{
						Pair<String, String> pair = ((Pair<String, String>) obj);

						extra = " (" + pair.left + "/" + pair.right + ")";
					}
					else
					{
						extra = "";
					}

					builder.setContentText(lastMsg + extra);
					notificationManager.notify(R.id.syncNotificationId, builder.build());
				}
			};

			e621.sync(em);
		}
		finally
		{
			notificationManager.cancel(R.id.syncNotificationId);
		}
	}
}
