package de.desy.dCacheCloud.Activities;

import java.math.BigInteger;
import java.security.MessageDigest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import de.desy.dCacheCloud.DatabaseHelper;
import de.desy.dCacheCloud.R;
import de.desy.dCacheCloud.QRCode.Contents;
import de.desy.dCacheCloud.QRCode.QRCodeEncoder;

public class ProfileActivity extends Activity {

	private ImageView ivQR;
	private TextView tvFingerPrint;

	
	public ProfileActivity(){}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.profile);
		ivQR = (ImageView)findViewById(R.id.imageViewQR);
		tvFingerPrint = (TextView)findViewById(R.id.textViewFingerPrint);
		
		tvFingerPrint.setText("Fingerprint");
		
				 
	   //Find screen size
	   WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
	   Display display = manager.getDefaultDisplay();
	   Point point = new Point();
	   display.getSize(point);
	   int width = point.x;
	   int height = point.y;
	   int smallerDimension = width < height ? width : height;
//	   smallerDimension = smallerDimension * 3/4;
	 
	   DatabaseHelper oh = new DatabaseHelper(this);
	   String content = oh.getOwnPublicKey();
	   String hashContent = oh.getOwnHashKey();

	   //Encode with a QR Code image
	   QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(content, 
	             null, 
	             Contents.Type.TEXT,  
	             BarcodeFormat.QR_CODE.toString(), 
	             smallerDimension);
	   try {
	    Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
	    ivQR.setImageBitmap(bitmap);
	    tvFingerPrint.setText("Fingerprint: " + hashContent);
	 
	   } catch (WriterException e) {
	    e.printStackTrace();
	   }
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
}
