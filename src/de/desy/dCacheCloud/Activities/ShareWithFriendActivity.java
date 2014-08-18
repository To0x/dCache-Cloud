package de.desy.dCacheCloud.Activities;

import java.util.List;
import javax.crypto.SecretKey;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.desy.dCacheCloud.CryptoHelper;
import de.desy.dCacheCloud.DatabaseHelper;
import de.desy.dCacheCloud.KeyStoreHelper;
import de.desy.dCacheCloud.R;

public class ShareWithFriendActivity extends Activity implements OnItemClickListener {

	private DatabaseHelper dbh = null;
	private ListView lvFriends = null;
	private boolean friendsFound = true;
	private String fileName = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.file_list);
		dbh = new DatabaseHelper(getApplicationContext());
		lvFriends = (ListView) findViewById(R.id.listView1);
		lvFriends.setOnItemClickListener(this);
		fileName = getIntent().getStringExtra("fileName");
		
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		List<String> friends  = dbh.getAllFriends();
		
		if (friends.size() == 0)
		{
			friendsFound = false;
			friends.add("no friends found");
		}
		
		lvFriends.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, friends));
		super.onResume();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// if no friends were found, then the message cant be clicked!
		if (!friendsFound)
			return;
		
		String usersPublic = dbh.getPersonPublicKey(lvFriends.getItemAtPosition(position).toString());
		share(usersPublic);
	}
	
	@SuppressWarnings("unused")
	private void share(String usersPublic)
	{
		SecretKey fileAESKey = KeyStoreHelper.getKey(fileName);
		CryptoHelper.encryptAsymmetric("");
		/*
		 * TODO:
		 * Encrypt (fileName + fileAES) with usersPublic
		 * read digital Signature --> do hash and encrypt with own private!
		 */
		return;
	}
}
