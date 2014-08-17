package de.desy.dCacheCloud.Activities;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import de.desy.dCacheCloud.DownloadService;
import de.desy.dCacheCloud.R;
import de.desy.dCacheCloud.ServerHelper;
 
public class ServerViewActivity extends Activity {

	// Defines begin //
	private static boolean HTTPLOG = false;
	// Defines End //
	
	private ListView listView;
	private EditText et;
	private ImageButton imageButtonRefresh;
	private ImageButton imageButtonSort;
	private ImageButton imageButtonSearch;
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
    
    private int sortState = 2; // 1 = sorted alphabetically, 2 = sorted by file type
    
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
		
		SharedPreferences preferences = getSharedPreferences("de.desy.dCacheCloud_preferences", Context.MODE_PRIVATE);
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
        
        if (sortState == 1) {
        	sortAlphabetically();
        }
        else {
        	sortFilesByType();
        }
        
        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				et.setVisibility(View.GONE);

        		if (isNetworkConnected()) {
	            	String currentItem = parent.getItemAtPosition(position).toString();            	
	            	if (!currentItem.substring(currentItem.length() - 1).equals("/")) { // Wenn das angeklickte Item kein Ordner ist...
		            	/*Intent intent = new Intent();
		            	intent.setAction(android.content.Intent.ACTION_VIEW);
		            	intent.setData(Uri.parse((url1.toString() + currentItem.replaceAll(" ", "%20"))));
		            	startActivity(intent);*/ 	            	
		        		Intent intent = new Intent(context, DownloadService.class);
		        		intent.putExtra("url", url1 + currentItem.replaceAll(" ", "%20"));
		        		intent.putExtra("filename", currentItem);
		        		startService(intent);
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
        		} else {
        			Toast.makeText(getApplicationContext(), "You are not connected to the internet!", Toast.LENGTH_LONG).show();
        		}
            }
        });
        
        /*listView.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener()
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
        });	*/	
	}
	
    public boolean isNetworkConnected() {
        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED;
   }
    
//    
//	public void addListenerOnImageButtonSort() {
//		et.setVisibility(View.GONE);
//
//        imageButtonSort = (ImageButton) findViewById(R.id.imageButtonSort);
//        imageButtonSort.setOnClickListener(new OnClickListener() {
// 
//			@Override
//			public void onClick(View arg0) {
//				ServerViewActivity.this.openOptionsMenu(); 				
//			}
//		});
//	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    super.onCreateOptionsMenu(menu);
		//SubMenu subAlphabetical = menu.addSubMenu(0, 0, 0, "Alphabetisch sortieren");
		//SubMenu subType = menu.addSubMenu(0, 1, 0, "Nach Typ sortieren");
		SubMenu sort = menu.addSubMenu(0, 0, 0, "Sortieren");
		sort.add(0, 3, 0, "Alphabetisch sortieren");
		sort.add(0, 4, 0, "Nach Typ sortieren");
		
		SubMenu refresh = menu.addSubMenu(0, 1, 0, "Aktualisieren");
		SubMenu search = menu.addSubMenu(0, 2, 0, "Suchen");
	    return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch(item.getItemId()) {
			case 0: 
//				sortAlphabetically();
//				sortState = 1;
			break;
			case 1:
//				sortFilesByType();
//				sortState = 2;
				refresh();
			break;
			case 2:
				search();
			break;
			case 3:
				sortAlphabetically();
			break;
			case 4:
				sortFilesByType();
			break;
		}
		return true;
	}	
	
	private void sortAlphabetically() {
		Vector<String> urlVector = new Vector<String>();
		for (int i = 0; i < listView.getAdapter().getCount(); i++) {
        	urlVector.add((String) listView.getAdapter().getItem(i));
		}		
		Collections.sort(urlVector);
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, urlVector));		
	}
	
	private void sortFilesByType() {
		
		sortAlphabetically(); // damit auch bei der Typsortierung eine alphabetische Ordnung herrscht
		
		Vector<String> urlVector = new Vector<String>();
		for (int i = 0; i < listView.getAdapter().getCount(); i++) {
        	urlVector.add((String) listView.getAdapter().getItem(i));
		}		
		
		Collections.sort(urlVector, new Comparator<String>() {
		    @Override
		    public int compare(String s1, String s2) {
		        final int s1Dot = s1.lastIndexOf('.');
		        final int s2Dot = s2.lastIndexOf('.');
		        if ((s1Dot == -1) == (s2Dot == -1)) { // both or neither
		            s1 = s1.substring(s1Dot + 1);
		            s2 = s2.substring(s2Dot + 1);
		            return s1.compareTo(s2);
		        } else if (s1Dot == -1) { // only s2 has an extension, so s1 goes first
		            return -1;
		        } else { // only s1 has an extension, so s1 goes second
		            return 1;
		        }
		    }
		});
				
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, urlVector));		
	}	
			
	public void refresh() {
		et.setVisibility(View.GONE);

//        imageButtonRefresh = (ImageButton) findViewById(R.id.imageButtonRefresh);
//        imageButtonRefresh.setOnClickListener(new OnClickListener() {
// 
//			@Override
//			public void onClick(View arg0) {
 			   test();
//			}
//		});
	}
	
	public void search() {
//		imageButtonSearch = (ImageButton) findViewById(R.id.imageButtonSearch);
//		imageButtonSearch.setOnClickListener(new OnClickListener() {
// 
//			@Override
//			public void onClick(View arg0) {

				et.setVisibility(View.VISIBLE);
				et.requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
				
				et.addTextChangedListener(new TextWatcher() {
	     
				    @Override
				    public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
				        // When user changed the Text
				        ((Filterable) listView.getAdapter()).getFilter().filter(cs);  
				    }
				     
				    @Override
				    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				        // TODO Auto-generated method stub
				    }
			
					@Override
					public void afterTextChanged(Editable arg0) {
						// TODO Auto-generated method stub
					}
				});
				
				et.setOnEditorActionListener(new OnEditorActionListener() {        
				    @Override
				    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				        if(actionId==EditorInfo.IME_ACTION_DONE){
							et.clearFocus();
							et.setVisibility(View.GONE);
							InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
							imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
							return true;
				        }
				    return false;
				    }
				});
				
				et.setOnFocusChangeListener(new OnFocusChangeListener() {          
					public void onFocusChange(View v, boolean hasFocus) {
						if (!hasFocus) {
							et.setVisibility(View.GONE);
						}
					}
				});			
//			}
//		});	
	}
		
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	 	
		url1 = (URL) getIntent().getExtras().get("url");
		
        setContentView(R.layout.file_list);
        listView = (ListView) findViewById(R.id.listView1);
		et = (EditText)findViewById(R.id.inputSearch); 

//		addListenerOnImageButtonRefresh();
//		addListenerOnImageButtonSort();
//		addListenerOnImageButtonSearch();		
		
	 	test();
	}
}