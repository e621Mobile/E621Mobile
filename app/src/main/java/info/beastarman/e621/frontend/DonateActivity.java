package info.beastarman.e621.frontend;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.DonationManager;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.qrcode.Contents;
import info.beastarman.e621.qrcode.QRCodeEncoder;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

public class DonateActivity extends BaseActivity implements Runnable
{
	@Override
    protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_donate);
		
		TextView t = (TextView) findViewById(R.id.buy_me_porn);
		t.setText(Html.fromHtml(this.getString(R.string.buy_me_porn)));
		
		final EditText paypalEmail = (EditText) findViewById(R.id.paypalEmail);
		paypalEmail.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				
				ClipData clip = ClipData.newPlainText("simple text",paypalEmail.getText());
				
				clipboard.setPrimaryClip(clip);
				
				Toast.makeText(getApplicationContext(), "Address copied to the clipboard", Toast.LENGTH_SHORT).show();
			}
		});

		new Thread(this).start();
    }
	
	public void paypalDonate(View v)
	{
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(getString(R.string.paypal_donation_link)));
		startActivity(i);
	}

	@Override
	public void run()
	{
		Log.d(E621Middleware.LOG_TAG, String.valueOf(e621.getDonationManager().getTotalDonations()));

		for(DonationManager.Donator d : e621.getDonationManager().getOldestDonators())
		{
			Log.d(E621Middleware.LOG_TAG,d.name);
		}
	}
}
