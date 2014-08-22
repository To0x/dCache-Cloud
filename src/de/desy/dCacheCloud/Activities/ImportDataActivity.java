package de.desy.dCacheCloud.Activities;

import android.app.Activity;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
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
			
			String content =etData.getText().toString();
			
			if (!content.equals(""))
			{
				// TODO: parse!
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
