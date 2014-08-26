package de.desy.dCacheCloud.Activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import de.desy.dCacheCloud.CryptoHelper;
import de.desy.dCacheCloud.DatabaseHelper;
import de.desy.dCacheCloud.KeyStoreHelper;
import de.desy.dCacheCloud.R;

public class ImportDataActivity extends Activity implements android.view.View.OnClickListener{

	private EditText etData = null;
	private Button btnDataOk = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		setContentView(R.layout.activity_import_data);
		etData = (EditText)findViewById(R.id.etData);
		btnDataOk = (Button)findViewById(R.id.btnDataOk);
		btnDataOk.setOnClickListener(this);
		
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onClick(View v) {

		switch (v.getId())
		{
		case R.id.btnDataOk:
			
			String content = etData.getText().toString();
			
			if (!content.equals(""))
			{
				DatabaseHelper dbh = new DatabaseHelper(getApplicationContext());
				
				// TODO: parse!
				byte[] encrypted = Base64.decode(content, Base64.DEFAULT);
				byte[] decrypted = CryptoHelper.decryptAsymmetric(encrypted, false, KeyStoreHelper.getOwnPriv());
				// decrypted is split in signature and message
				byte[] signature = new byte[1024];
				byte[] message = new byte[decrypted.length - 1024];
				System.arraycopy(decrypted, decrypted.length - 1024, signature, 0, 1024);
				System.arraycopy(decrypted, 0, message, 0, decrypted.length - 1024);
				
				byte[] messageHash = CryptoHelper.decryptAsymmetric(signature, true, dbh.getPersonPublicKey("???"));
				
				Toast.makeText(getApplicationContext(), content, Toast.LENGTH_LONG).show();
			}
			else
			{
				Toast.makeText(getApplicationContext(), "Please import data!", Toast.LENGTH_LONG).show();
			}
			
			break;
		}
		
	}

	
	
}
