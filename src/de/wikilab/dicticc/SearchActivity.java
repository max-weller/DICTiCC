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


import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
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
	
	SQLiteDatabase db;
	
	final int REQ_DICTIONARY_SELECT = 1;

	String dictFilename;
	String dictTitle;
	int dictId = -1;
	ProgressDialog progdialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SearchActivity.this);
        dictId = pref.getInt("dictId", -1);
        dictFilename = pref.getString("dictFilename", null);
        dictTitle = pref.getString("dictTitle", null);
        
        updateTitleBar();
        
        DictionaryOpenHelper dictoh = new DictionaryOpenHelper(SearchActivity.this);
        db = dictoh.getWritableDatabase();
        
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
				startActivityForResult(new Intent(SearchActivity.this, DictionarySelectActivity.class), REQ_DICTIONARY_SELECT);
			}
		});
        
        
        
        
    }
    
    void updateTitleBar() {
    	if (dictTitle != null) {
    		setTitle(getString(R.string.app_name) + " - " + dictTitle);
    	} else {
    		setTitle(getString(R.string.app_name) + " - kein Wörterbuch gewählt");
    	}
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
    	if (dictId < 0) {
    		Toast.makeText(SearchActivity.this, "Bitte ein Wörterbuch auswählen!", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	String searchTerm = searchbox.getText().toString();
    	if (searchTerm.length() < 2) {
    		Toast.makeText(SearchActivity.this, "Bitte einen Suchbegriff eingeben!", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	searchbox.clearFocus();
    	
    	progdialog = ProgressDialog.show(SearchActivity.this, null, "Wörterbuch wird durchsucht...", true);
    	
    	DictionarySearcher ds = new DictionarySearcher();
    	ds.targetList = (ListView) findViewById(R.id.contentlist);
    	ds.execute(String.valueOf(dictId), searchTerm);
        return;
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
            
            return true;
        case R.id.import_dictionary:
            
            return true;
        case R.id.settings:
            
            return true;
        case R.id.help:
            startActivity(new Intent(SearchActivity.this, AboutScreenActivity.class));
            return true;
        case R.id.item1:
        	Cursor cur = db.rawQuery("SELECT * FROM dictionary LIMIT 0,10;", new String[]{});
        	while (cur.moveToNext()) {
        		for (int i = 0; i<cur.getColumnCount(); i++) Log.i("Search", " --> "+cur.getColumnName(i)+"\t"+cur.getString(i));
        		Log.i("Search","");
        	}
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
    			dictFilename = data.getStringExtra(Intent.EXTRA_TEXT);
    			dictTitle = data.getStringExtra("dictTitle");
    			dictId = data.getIntExtra(Intent.EXTRA_UID, -1);
    			SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(SearchActivity.this).edit();
    			ed.putString("dictFilename", dictFilename);
    			ed.putString("dictTitle", dictTitle);
    			ed.putInt("dictId", dictId);
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
			String fileId = params[0], searchTerm = params[1].toLowerCase();
			
			ArrayList<String[]> lines = new ArrayList<String[]>();
			
			String termLength = String.valueOf(searchTerm.length());
			try {
				String sql = "SELECT "+DictionaryOpenHelper.KEY_WORD+", "+DictionaryOpenHelper.KEY_WORD_EXTRA+", "+DictionaryOpenHelper.KEY_WORD_GENDER+", "+
				DictionaryOpenHelper.KEY_DEFINITION+", "+DictionaryOpenHelper.KEY_DEFINITION_EXTRA+", "+DictionaryOpenHelper.KEY_DEFINITION_GENDER+", "+DictionaryOpenHelper.KEY_TYPE+
				" FROM "+DictionaryOpenHelper.DICTIONARY_TABLE_NAME+
				" WHERE "+DictionaryOpenHelper.KEY_FILEID+" = ? AND "+DictionaryOpenHelper.KEY_WORD_LENGTH+" >=  "+termLength+
				" AND substr("+DictionaryOpenHelper.KEY_WORD_LOWERCASE+",1,"+termLength+") = ? "+
				" ORDER BY "+DictionaryOpenHelper.KEY_WORD_LENGTH+" ";
				Log.v("DictionarySearcher", "sql="+sql);
				Log.i("DictionarySearcher", "fileId="+fileId);
				Log.i("DictionarySearcher", "searchTerm="+searchTerm);
				
				Cursor cur = db.rawQuery(sql, new String[] {fileId, searchTerm});
				
				
				String lastTerm = "___";
				while (cur.moveToNext()) {
					if (! lastTerm.equals(cur.getString(0))) {
						lastTerm = cur.getString(0);
						lines.add(new String[] {"H", lastTerm});
					}
					lines.add(new String[]{"T", "<b>" + cur.getString(3) + "</b> <i>" + cur.getString(4) + " " + cur.getString(5) + "</i>", cur.getString(6), cur.getString(1) + cur.getString(2)});
					
					result.count++;
				}


				cur.close();
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
			progdialog.dismiss();
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