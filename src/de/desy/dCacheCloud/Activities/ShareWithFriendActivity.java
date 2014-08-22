package de.desy.dCacheCloud.Activities;

import java.security.PublicKey;
import java.util.List;

import javax.crypto.SecretKey;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
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
		
		PublicKey usersPublic = dbh.getPersonPublicKey(lvFriends.getItemAtPosition(position).toString());
		share(usersPublic);
	}
	
	private void share(PublicKey key)
	{
		SecretKey fileAESKey = KeyStoreHelper.getKey(fileName);
		
		JSONObject json = new JSONObject();
		try
		{
			json.put("name", fileName);
			json.put("key", Base64.encode(fileAESKey.getEncoded(), Base64.DEFAULT));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		
		String message = json.toString();
		String messageHash = CryptoHelper.hash(message);
		
		// TODO: signature lenght = 1024?
		byte[] signature = CryptoHelper.encryptAsymmetric(messageHash, true, KeyStoreHelper.getOwnPriv());
		// TODO: String + byte[] ?!? --> what happened here?
		byte[] encrypted = CryptoHelper.encryptAsymmetric(message + signature, false, key);
		String ownPubHash = CryptoHelper.hash(CryptoHelper.PublicKeyToString(KeyStoreHelper.getOwnPub()));
		
		String output = Base64.encodeToString(encrypted, Base64.DEFAULT);
		output += ownPubHash;
//		Toast.makeText(getApplicationContext(), output, Toast.LENGTH_LONG).show();
		
		Intent intent = new Intent(getApplicationContext(), ShareDataActivity.class);
		intent.putExtra("DATA", output);
		startActivity(intent);
		finish();
		
		return;
	}
}
