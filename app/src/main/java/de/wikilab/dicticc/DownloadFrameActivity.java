/**
 *  This file is part of DICTiCC.
 *  
 *  DICTiCC is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  DICTiCC is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with DICTiCC.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Copyright (c) 2011 by Max Weller <dicticc@max-weller.de>
 */

package de.wikilab.dicticc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class DownloadFrameActivity extends Activity {
	private WebView webview;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		webview = new WebView(this);
		setContentView(webview);
		webview.getSettings().setJavaScriptEnabled(true);
		final Activity activity = this;
		webview.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				activity.setProgress(progress * 100);
			}
		});
		webview.setWebViewClient(new WebViewClient() {
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				Toast.makeText(activity,description, Toast.LENGTH_SHORT).show();
			}
		});
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DownloadFrameActivity.this);
		
		webview.loadUrl(getString(R.string.dictionary_download_url));
		
		new AlertDialog.Builder(DownloadFrameActivity.this)
		.setTitle(R.string.download_info_dlg_title)
		.setMessage(R.string.download_info_dlg_content)
		.setPositiveButton(R.string.download_info_dlg_okbutton, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.show();
		
		/*
		Intent callingIntent = getIntent();
		if (callingIntent != null) {
			Bundle e = callingIntent.getExtras();
			if(prefs.getBoolean("use_debug", false)) {
				
				
				String[] val = new String[e.keySet().size()];
				int i=0;
				for(String key : e.keySet()) {
					val[i++] = key+": "+String.valueOf(e.get(key));
				}
				
				new AlertDialog.Builder(ShareActivity.this)
				.setItems(val, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
					}
				})
				.setTitle("sent data")
				.show();
			}
			String url = prefs.getString("share_target_url", "http://labs.max-weller.de/urlout.php?URL=$U&Titel=$T");
			url = url.replace("$U",URLEncoder.encode(callingIntent.getStringExtra(Intent.EXTRA_TEXT))).replace("$T", URLEncoder.encode(callingIntent.getStringExtra(Intent.EXTRA_SUBJECT)));
			((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setText(url);
			
			webview.loadUrl(url);
		}
		*/
		//
	}
	//Zurück Button
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
			webview.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	/*
//Menü Items

@Override 
public boolean onCreateOptionsMenu(Menu menu) { 
	super.onCreateOptionsMenu(menu);
	MenuItem ITEM1 = menu.add(0, 0, 0, "ITEM1");
	MenuItem ITEM2 = menu.add(0, 1, 0, "ITEM2");
	MenuItem ITEM3 = menu.add(0, 2, 0, "ITEM3");
	MenuItem ITEM4 = menu.add(0, 3, 0, "ITEM4");
	MenuItem ITEM5 = menu.add(0, 4, 0, "ITEM5");
	return true; 
}
//Menü Actions
@Override 
public boolean onOptionsItemSelected(MenuItem item) { 
	switch (item.getItemId()) {
	case 0:
		ACTION1
		break;
	case 1:
		ACTION2
		break; 
	case 2:
		ACTION3
		break; 
	case 3:
		ACTION4
		break;
	case 4:
		ACTION5
		break; 
	} 
	return false;
}
	 */
}