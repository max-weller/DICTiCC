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


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SearchActivity extends Activity {
	
	final int REQ_DICTIONARY_SELECT = 1;
	
	String dictFile,dictTitle;
	ProgressDialog progdialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SearchActivity.this);
        dictFile = pref.getString("dictFile", null);
        dictTitle = pref.getString("dictTitle", null);
        
        updateTitleBar();
        
        final EditText searchbox = (EditText) findViewById(R.id.searchbox);
        final Button dictselectbutton = (Button) findViewById(R.id.dictselectbutton);
        final ListView content = (ListView) findViewById(R.id.contentlist);
        
        DictListAdapter lista = new DictListAdapter(SearchActivity.this, 0, new String[][] {new String[] {"H", "Bitte einen Suchbegriff eingeben!"}});
        lista.listHeaderId = R.layout.listitem_groupheader;
        lista.listContentId = R.layout.listitem_translation;
        content.setAdapter(lista);
        
        searchbox.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                	doSearch();
                	return true;
                } else {
                    return false;
                }
            }
        });
        
        dictselectbutton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectDictionary();
			}
		});
    }
    
    void selectDictionary() {
    	startActivityForResult(new Intent(SearchActivity.this, DictionarySelectActivity.class), REQ_DICTIONARY_SELECT);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	if (savedInstanceState != null) {
        	if (savedInstanceState.getBoolean("restartSearch", false) == true) {
        		doSearch();
        	}
        }
    }
    

    void doSearch() {
        final EditText searchbox = (EditText) findViewById(R.id.searchbox);
    	if (dictFile == null) {
    		Toast.makeText(SearchActivity.this, "Bitte ein Wörterbuch auswählen!", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	String searchTerm = searchbox.getText().toString();
    	if (searchTerm.length() < 2) {
    		Toast.makeText(SearchActivity.this, "Bitte einen Suchbegriff eingeben!", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	searchbox.clearFocus();
    	
    	//progdialog = ProgressDialog.show(SearchActivity.this, null, "Wörterbuch wird durchsucht...", true);
    	
    	DictionarySearcher ds = new DictionarySearcher();
    	ds.targetList = (ListView) findViewById(R.id.contentlist);
    	ds.execute(dictFile, searchTerm);
        return;
    }
    

    void updateTitleBar() {
    	if (dictTitle != null) {
    		setTitle(getString(R.string.app_name) + " - " + dictTitle);
    	} else {
    		setTitle(getString(R.string.app_name) + " - kein Wörterbuch gewählt");
    	}
    }
    
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	outState.putBoolean("restartSearch", true);
    	super.onSaveInstanceState(outState);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.searchactivity_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.dictselectbutton:
            selectDictionary();
            return true;
        case R.id.import_dictionary:
            
            return true;
        case R.id.settings:
        	startActivity(new Intent(SearchActivity.this, PrefsActivity.class));
            return true;
        case R.id.help:
            startActivity(new Intent(SearchActivity.this, AboutScreenActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	switch(requestCode) {
    	case REQ_DICTIONARY_SELECT:
    		if (resultCode == RESULT_OK) {
    			dictFile = data.getStringExtra(Intent.EXTRA_TEXT);
    			dictTitle = data.getStringExtra("title");
    			SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(SearchActivity.this).edit();
    			ed.putString("dictFile", dictFile);
    			ed.putString("dictTitle", dictTitle);
    			ed.commit();
    			updateTitleBar();
    		}
    		break;
    	}
    }

    public class DictionarySearcher extends AsyncTask<String, Long, DictionarySearcher.ResultSet> {
        
    	private class ResultSet { long count,anfang,ende; boolean success; String error; String[][] data; }
    	
    	ListView targetList;
    	
		@Override
		protected ResultSet doInBackground(String... params) {
			ResultSet result = new ResultSet();
			result.anfang = android.os.Process.getElapsedCpuTime();
			String dictFilePrefix = params[0], searchTerm = params[1].toLowerCase();
			
			ArrayList<String[]> lines = new ArrayList<String[]>();
			
			//String termLength = String.valueOf(searchTerm.length());
			try {
				char ch0 = searchTerm.charAt(0);
				char ch1 = searchTerm.length() > 1 ? searchTerm.charAt(1) : '$';
				FileInputStream fis;
				if (Character.isLetter(ch0)) ch0 = Character.toLowerCase(ch0); else ch0 = '$';
				if (Character.isLetter(ch1)) ch1 = Character.toLowerCase(ch1); else ch1 = '$';
				
				fis = SearchActivity.this.openFileInput(dictFilePrefix+"_"+ch0+ch1);
				
				BufferedReader read = new BufferedReader(new InputStreamReader(fis));
				
				Pattern tabPattern = Pattern.compile("\t");
				String lastTerm = "___", line;
				String[] cols;
				while ((line = read.readLine()) != null) {
					if (!line.startsWith(searchTerm)) continue;
					cols = tabPattern.split(line, -1);
					//Log.v("Search", "cols.length="+cols.length+"\t\t"+line);
					if (! lastTerm.equals(cols[Dict.WORD])) {
						lastTerm = cols[Dict.WORD];
						lines.add(new String[] {"H", lastTerm});
					}
					lines.add(new String[]{"T", "<b>" + cols[Dict.DEF] + "</b> <i>" + cols[Dict.DEF_GENDER] + " " + cols[Dict.DEF_EXTRA] + "</i>", cols[Dict.TYPE], cols[Dict.WORD_GENDER] + " " + cols[Dict.WORD_EXTRA]});
					
					result.count++;
				}


				read.close();
				result.success = true;
				result.data = lines.toArray(new String[][]{});
			} catch (Exception e) {
				result.success = false;
				result.error = e.toString();
				e.printStackTrace();
			} finally {
			}
			
			result.ende = android.os.Process.getElapsedCpuTime();
			return result;
		}
    	
		@Override
		protected void onPostExecute(ResultSet result) {
			//progdialog.dismiss();
			if (result.success) {
		        Toast.makeText(SearchActivity.this, String.format(getString(R.string.result_count_toast), result.count, (float)(result.ende - result.anfang)/1000), Toast.LENGTH_SHORT).show();
		        DictListAdapter dla = new DictListAdapter(SearchActivity.this, 0, result.data);
		        dla.listHeaderId = R.layout.listitem_groupheader;
		        dla.listContentId = R.layout.listitem_translation;
		        targetList.setAdapter(dla);
			} else {
				MessageBox.alert(SearchActivity.this, result.error, "Fehler!");
			}
			super.onPostExecute(result);
		}
		
    }
    

	public class DictListAdapter extends ArrayAdapter<String[]> {
		int listHeaderId, listContentId;
		
		public DictListAdapter(Context context, int textViewResourceId, String[][] objects) {
			super(context, textViewResourceId, objects);
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater=getLayoutInflater();
			String[] content = getItem(position);
			View row;
			if (content[0].equals("H")) {
				row = inflater.inflate(listHeaderId, parent, false);
				TextView headerText = (TextView) row.findViewById(R.id.text);
				headerText.setText(Html.fromHtml(content[1]));
			} else {
				row = inflater.inflate(listContentId, parent, false);
				TextView translation1 = (TextView) row.findViewById(R.id.translation_1);
				translation1.setText(Html.fromHtml(content[1]));
				TextView translation2 = (TextView) row.findViewById(R.id.translation_2);
				translation2.setText(Html.fromHtml(content[2]));
				TextView translation3 = (TextView) row.findViewById(R.id.translation_3);
				translation3.setText(Html.fromHtml(content[3]));
			}
			
			return row;
		}
	}
}