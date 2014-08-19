package de.desy.dCacheCloud.Activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.desy.dCacheCloud.DatabaseHelper;
import de.desy.dCacheCloud.R;

public class FriendFoundActivity extends Activity {

	private EditText friendName;
	private DatabaseHelper oh;
	private String public_key;
	private TextView tvFriendHash;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_friend_found);
		friendName = (EditText)findViewById(R.id.editTextFriendName);
		tvFriendHash = (TextView)findViewById(R.id.textViewFriendFingerprint);
		
		Bundle b = getIntent().getExtras();
		public_key = b.getString("KEY");
		tvFriendHash.setText(b.getString("HASH"));
		oh = new DatabaseHelper(this);
		
	}
	
	public void onClick(View view)
	{
		switch (view.getId())
		{
		case R.id.buttonFriendOK:
			if (friendName.getText().toString().equals(""))
			{
				Toast.makeText(this, "no Name set", Toast.LENGTH_LONG).show();
			}
			else
			{
				// TODO:
				oh.setPersonPublicKey(friendName.getText().toString(), public_key, tvFriendHash.getText().toString());
				this.finish();
			}
		}
	}
}
