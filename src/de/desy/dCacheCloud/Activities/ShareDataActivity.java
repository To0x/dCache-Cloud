package de.desy.dCacheCloud.Activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import de.desy.dCacheCloud.R;

public class ShareDataActivity extends Activity implements android.view.View.OnClickListener {

	private String content;
	private TextView tvData;
	private TextView tvInfo;
	private Button btnCopy;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		content = getIntent().getExtras().getString("DATA");
		setContentView(R.layout.activity_data_to_share);
		tvData = (TextView)findViewById(R.id.tvShareData);
		tvInfo = (TextView) findViewById(R.id.tvInfo);
		btnCopy = (Button) findViewById(R.id.btnCopyToClipboard);
		btnCopy.setOnClickListener(this);
		
		super.onCreate(savedInstanceState);
	}
	
	

	@Override
	protected void onResume() {

		tvData.setText(content);
		tvInfo.setText("Please send this Data to your Friend");
		super.onResume();
	}



	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {

		switch (v.getId())
		{
		case R.id.btnCopyToClipboard:
			int sdk = android.os.Build.VERSION.SDK_INT;
			if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
			    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			    clipboard.setText("text to clip");
			} else {
			    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
			    android.content.ClipData clip = android.content.ClipData.newPlainText("text label","text to clip");
			    clipboard.setPrimaryClip(clip);
			}
			
			Toast.makeText(getApplicationContext(), "Data copied!", Toast.LENGTH_LONG).show();
			finish();
		}
		
	}

	
	
}
