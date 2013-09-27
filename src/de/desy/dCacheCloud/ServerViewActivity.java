package de.desy.dCacheCloud;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.zekjur.davsync.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.apache.ivy.util.Message;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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
	private URL url1;
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

    public List retrieveListing(URL url, String htmlText, boolean includeFiles, boolean includeDirectories)
            throws IOException {
        List urlList = new ArrayList();

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
            if (href.startsWith("http:") || href.startsWith("https:")) {
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
            }

            // exclude those where they do not match
            // href will never be truncated, text may be truncated by apache
            // may have a '.' from either the extension (.jar) or "..&gt;"
            int dotIndex = text.indexOf('.');

            if (((dotIndex != -1) && !href.startsWith(text.substring(0, dotIndex)))
                    || ((dotIndex == -1) 
                            && !href.toLowerCase(Locale.US).equals(text.toLowerCase(Locale.US)))) {
                continue;
            }

            boolean directory = href.endsWith("/");

            if ((directory && includeDirectories) || (!directory && includeFiles)) {
                
            	/* Test Root-Dir und User-dir ausschlieﬂen */
            	if (!href.equals("/") && !href.equals(String.format("%s/", user)))
            	{
                	
                	URL child = new URL(url, href);
                    urlList.add(child);
                    Message.debug("ApacheURLLister found URL=[" + child + "].");
            	}
            	/* */
            }
        }

        return urlList;
    }
	
	private void test() {
		final Context context = this;
	
        setContentView(R.layout.file_list);
        listView = (ListView) findViewById(R.id.listView1);
        
		SharedPreferences preferences = getSharedPreferences("net.zekjur.davsync_preferences", Context.MODE_PRIVATE);
		
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
		
	    try {
			url1 = new URL(preferences.getString("webdav_url", null));
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
	    
		httpGet = new HttpGet(preferences.getString("webdav_url", null));
		
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
			serverDir = retrieveListing(new URL(preferences.getString("webdav_url", null)), response, true, true);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
        Vector<String> urlVector = new Vector<String>();
        for (int i = 0; i < serverDir.size(); i++)
        {	
        	urlVector.add(serverDir.get(i).toString().substring(url1.toString().length())); 
      	            	
            /*final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData((Uri) serverDir.get(i));
            intent.setType("image/png");         
            
            final List<ResolveInfo> matches = getPackageManager().queryIntentActivities(intent, 0);
            for (ResolveInfo match : matches) {
                final Drawable icon = match.loadIcon(getPackageManager());
                //final CharSequence label = match.loadLabel(getPackageManager());
            }*/
        	
        	//Log.i(serverDir.get(i).toString(), "ha");
        	//Log.i(urlVector.lastElement().toString(), "bla");
        }
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, urlVector));
        
        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	// Aufrufen der Datei im Browser?
    		    /*Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
    		    intent.putExtra("url", url1.toString() + parent.getItemAtPosition(position).toString());
    		    startActivity(intent);*/
    		    //finish();
            	Log.i("Hello!", "Clicked! YAY!");
            	
            	Intent intent = new Intent();
            	intent.setAction(android.content.Intent.ACTION_VIEW);
            	
            	String urlString = url1.toString() + parent.getItemAtPosition(position);
            	String extension = "";
            	int i = urlString.lastIndexOf('.');
            	if (i > 0) {
            		extension = urlString.substring(i+1);
            	}
            	
            	intent.setData(Uri.parse((url1.toString() + parent.getItemAtPosition(position))));
            	startActivity(intent); 
            }
        });
        
        listView.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            	Log.i("Hello!", "LONGClicked! YAY!");
            	
        		Intent intent = new Intent(context, DownloadService.class);
        		intent.putExtra("url", url1 + parent.getItemAtPosition(position).toString());
        		intent.putExtra("filename", parent.getItemAtPosition(position).toString());
        		startService(intent);
            	
            	//DownloadFromUrl(url1 + parent.getItemAtPosition(position).toString(), parent.getItemAtPosition(position).toString());
                return true;
            }
        });		
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	 	
	 	test();
	}
}