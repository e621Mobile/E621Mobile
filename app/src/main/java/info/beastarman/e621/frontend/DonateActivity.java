package info.beastarman.e621.frontend;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import info.beastarman.e621.R;
import info.beastarman.e621.qrcode.Contents;
import info.beastarman.e621.qrcode.QRCodeEncoder;

public class DonateActivity extends BaseActivity
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
				
				ClipData clip = ClipData.newPlainText("simple text", paypalEmail.getText());
				
				clipboard.setPrimaryClip(clip);
				
				Toast.makeText(getApplicationContext(), "Address copied to the clipboard", Toast.LENGTH_SHORT).show();
			}
		});
		
		final EditText bitcoinWallet = (EditText) findViewById(R.id.bitcoinWallet);
		bitcoinWallet.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				
				ClipData clip = ClipData.newPlainText("simple text", bitcoinWallet.getText());
				
				clipboard.setPrimaryClip(clip);
				
				Toast.makeText(getApplicationContext(), "Address copied to the clipboard", Toast.LENGTH_SHORT).show();
			}
		});
		
		final ImageView bitcoinQRCode = (ImageView) findViewById(R.id.bitcoinQRCode);
		
		String qrData = "bitcoin:" + getString(R.string.bitcoin);
		int qrCodeDimention = 512;

		QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrData, null,
															   Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimention);

		try
		{
			Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
			bitcoinQRCode.setImageBitmap(bitmap);
		}
		catch(WriterException e)
		{
			e.printStackTrace();
		}
	}
	
	public void paypalDonate(View v)
	{
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(getString(R.string.paypal_donation_link)));
		startActivity(i);
	}
	
	public void bitcoinDonate(View v)
	{
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse("bitcoin:" + getString(R.string.bitcoin)));
		
		try
		{
			startActivity(i);
		}
		catch(ActivityNotFoundException e)
		{
			Toast.makeText(getApplicationContext(), "No bitcoin application found", Toast.LENGTH_SHORT).show();
		}
	}
}
