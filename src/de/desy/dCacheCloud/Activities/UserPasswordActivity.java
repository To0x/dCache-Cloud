package de.desy.dCacheCloud.Activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import de.desy.dCacheCloud.R;

public class UserPasswordActivity extends Activity implements OnClickListener {

	private TextView tvUserName;
	private TextView tvPassword;
	private EditText etUserName;
	private EditText etPassword;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_user_password);
		tvUserName = (TextView)findViewById(R.id.tvUserName);
		tvPassword = (TextView)findViewById(R.id.tvPassword);
		etUserName = (EditText)findViewById(R.id.etUserName);
		etPassword = (EditText)findViewById(R.id.etPassword);
		
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		
		// TODO
		super.onResume();
	}

	@Override
	public void onClick(View v) {

		switch (v.getId())
		{
		case R.id.btnUserPassOk:
			
			// TODO
			break;
		}
	}
}
