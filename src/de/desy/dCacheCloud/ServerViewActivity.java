package de.desy.dCacheCloud;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.zekjur.davsync.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.ivy.util.Message;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
 
public class ServerViewActivity extends Activity {

	// Defines begin //
	private static boolean HTTPLOG = false;
	// Defines End //
	
	private ListView listView;
	private URL url1 = null;
	private HttpGet httpGet = null;
	private String user = null;
 
	private DefaultHttpClient httpClient = null;
	
	// http://www.mkyong.com/regular-expressions/how-to-extract-html-links-with-regular-expression/
	// new expression for file-extracting
	
    private static final Pattern PATTERN = Pattern.compile(
            "<a[^>]*href=\"([^\"]*)\"[^>]*>(?:<[^>]+>)*?([^<>]+?)(?:<[^>]+>)*?</a>",
            Pattern.CASE_INSENSITIVE);
    
    private static final Pattern HTML_A_TAG_PATTERN = Pattern.compile("(?i)<a([^>]+)>(.+?)</a>");
    private static final Pattern HTML_A_HREF_TAG_PATTERN = Pattern.compile(
      "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))");
    
    public List retrieveListing(URL url, String htmlText) throws IOException {
        List urlList = new ArrayList();

        int whereToBegin = htmlText.lastIndexOf("<h1>File System</h1>"); 
        htmlText = htmlText.substring(whereToBegin);       
                
        Matcher matcherTag = HTML_A_TAG_PATTERN.matcher(htmlText);
                
                
        while (matcherTag.find()) {
          String href = matcherTag.group(1);
          String linkText = matcherTag.group(2);
          
          Matcher matcherLink = HTML_A_HREF_TAG_PATTERN.matcher(href);
          
          while (matcherLink.find()) {
            String link = matcherLink.group(1);
          }
        }
        
        Matcher matcher = PATTERN.matcher(htmlText);

        while (matcher.find()) {
            // get the href text and the displayed text
            String href = matcher.group(1);
            String text = matcher.group(2);

            if ((href == null) || (text == null)) {
                // the groups were not found (shouldn't happen, really)
                continue;
            }

            text = text.trim();
            
            // handle complete URL listings
            /*if (href.startsWith("http:") || href.startsWith("https:")) {
                try {
                    href = new URL(href).getPath();
                    if (!href.startsWith(url.getPath())) {
                        // ignore URLs which aren't children of the base URL
                        continue;
                    }
                    href = href.substring(url.getPath().length());
                } catch (Exception ignore) {
                    // incorrect URL, ignore
                    continue;
                }
            }

            if (href.startsWith("../")) {
                // we are only interested in sub-URLs, not parent URLs, so skip this one
                continue;
            }
            
            // absolute href: convert to relative one
            if (href.startsWith("/")) {
                int slashIndex = href.substring(0, href.length() - 1).lastIndexOf('/');
                href = href.substring(slashIndex + 1);
            }

            // relative to current href: convert to simple relative one
            if (href.startsWith("./")) {
                href = href.substring("./".length());
            }*/

        	URL child = new URL(url, text);
            urlList.add(child);                 
        }

        return urlList;
    }
	
	private void test() {
		final Context context = this;
	
        setContentView(R.layout.file_list);
        listView = (ListView) findViewById(R.id.listView1);
        	
		try {
			httpClient = ServerHelper.getClient();
		} catch (GeneralSecurityException e) {
			Log.d("SECURITY", String.format("General Security Error: %s", e.toString()));
			e.printStackTrace();
		} catch (IOException e1) {
			Log.d("Unknown", String.format("Error: %s", e1.toString()));
			e1.printStackTrace();
		}
		
		/* handler test */
	    ResponseHandler<String> handler = new ResponseHandler<String>() {
	        public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
	            HttpEntity entity = response.getEntity();
	            String html; 

	            if (entity != null) {
	                html = EntityUtils.toString(entity);
	                return html;
	            } else {
	                return null;
	            }
	        }
	    };
		/* test ende */
		
		SharedPreferences preferences = getSharedPreferences("net.zekjur.davsync_preferences", Context.MODE_PRIVATE);
		httpGet = new HttpGet(url1.toString());
		user = preferences.getString("webdav_user", null);
		String password = preferences.getString("webdav_password", null);
		
		ServerHelper.setCredentials(httpClient, httpGet, user, password);
		
		String response = null;
		
		try {
			response = httpClient.execute(httpGet, handler);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		List serverDir = null;
		try {
			serverDir = retrieveListing(url1, response);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
        Vector<String> urlVector = new Vector<String>();
        for (int i = 0; i < serverDir.size(); i++)
        {	
        	urlVector.add(serverDir.get(i).toString().substring(url1.toString().length())); 
        }
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, urlVector));
        
        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	
            	String currentItem = parent.getItemAtPosition(position).toString().replaceAll(" ", "%20");            	
            	if (!currentItem.substring(currentItem.length() - 1).equals("/")) { // Wenn das angeklickte Item kein Ordner ist...
	            	Intent intent = new Intent();
	            	intent.setAction(android.content.Intent.ACTION_VIEW);
	            	intent.setData(Uri.parse((url1.toString() + currentItem)));
	            	startActivity(intent); 	            	
            	}
            	else { // ... wenn das angeklickte Item ein Ordner ist
            		/* neue ServerViewActivity */
            		if (android.os.Build.VERSION.SDK_INT > 10) {
            			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            			StrictMode.setThreadPolicy(policy);
            		}
        		    Intent intent = new Intent(context, ServerViewActivity.class);
					try {
						intent.putExtra("url", new URL(url1.toString() + currentItem.replaceAll(" ", "%20")));
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
        		    startActivity(intent);
        		    /* Ende neue ServerViewActivity */
            	}
            }
        });
        
        listView.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            	
            	String currentItem = parent.getItemAtPosition(position).toString(); 
            	
            	if (!currentItem.substring(currentItem.length() - 1).equals("/")) {	            	
	        		Intent intent = new Intent(context, DownloadService.class);
	        		intent.putExtra("url", url1 + currentItem.replaceAll(" ", "%20"));
	        		intent.putExtra("filename", currentItem);
	        		startService(intent);
            	}
            	else {
            		Log.d("Hey", "Du kannst keinen Ordner downloaden!");
            	}
                return true;
            }
        });		
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	 	
		url1 = (URL) getIntent().getExtras().get("url");
	 	test();
	}
}