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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class DictionarySelectActivity extends ListActivity {

	final int RC_IMPORT_DONE = 1;
	
	boolean emptyList;
	String[] file_list;
	String[] file_title_list;
	Integer[] file_id_list;
	SQLiteDatabase db;
	
    public String getDictDirectory() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DictionarySelectActivity.this);
		return prefs.getString("dict_directory", "/mnt/sdcard/dictionaries/");
    }
	
    public void fillList() {

        //Cursor result = db.rawQuery("SELECT " + DictionaryOpenHelper.KEY_FILE + " FROM " + DictionaryOpenHelper.DICTIONARY_TABLE_NAME + " GROUP BY " + DictionaryOpenHelper.KEY_FILE, null);
        Cursor result = db.rawQuery("SELECT " + DictionaryOpenHelper.KEY_FILEID + ", " + DictionaryOpenHelper.KEY_FILENAME + ", " + DictionaryOpenHelper.KEY_TITLE + " FROM " + DictionaryOpenHelper.FILELIST_TABLE_NAME + " ORDER BY " + DictionaryOpenHelper.KEY_FILENAME, null);

        ArrayList<String> files = new ArrayList<String>();
        ArrayList<String> file_titles = new ArrayList<String>();
        ArrayList<Integer> file_ids = new ArrayList<Integer>();
        while(result.moveToNext()) {
        	files.add(result.getString(1));
        	file_titles.add(result.getString(2));
        	file_ids.add(result.getInt(0));
        }
        if (files.size() == 0) {
        	emptyList = true;
        	file_list = null;
        	file_title_list = null;
        	file_id_list = null;
        	setListAdapter(new GenericStringAdapter(DictionarySelectActivity.this, R.layout.listitem_dictionary, R.id.text, getLayoutInflater(), new String[]{getString(R.string.no_dictionaries_helptext)}, true));
        } else {
        	emptyList = false;
	        file_list = files.toArray(new String[0]);
	        file_id_list = file_ids.toArray(new Integer[0]);
	        file_title_list = file_titles.toArray(new String[0]);
	        result.close();
	        
	        //File dir = new File(getDictDirectory());
	        //File[] subfiles = dir.listFiles();
	        
	        //file_list = new String[subfiles.length];
	        //for(int i = 0; i < subfiles.length; i++) file_list[i] = subfiles[i].getName();
	        
	        setListAdapter(new GenericStringAdapter(DictionarySelectActivity.this, R.layout.listitem_dictionary, R.id.text, getLayoutInflater(), file_title_list, false));
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DictionaryOpenHelper dictoh = new DictionaryOpenHelper(DictionarySelectActivity.this);
        db = dictoh.getWritableDatabase();
        
        fillList();
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);

        lv.setOnItemClickListener(new OnItemClickListener() {
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          	if (emptyList) return;
          	
        	String data = file_list[position];
          	Intent resultIntent = new Intent();
          	resultIntent.putExtra(Intent.EXTRA_TEXT, data);
          	resultIntent.putExtra("dictTitle", file_title_list[position]);
          	resultIntent.putExtra(Intent.EXTRA_UID, file_id_list[position]);
          	setResult(RESULT_OK, resultIntent);
          	finish();
          }
        });
        
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (emptyList) return false;
	          	
				final int file_id = file_id_list[position];
				new AlertDialog.Builder(DictionarySelectActivity.this)
				.setTitle("Wörterbuch")
				.setItems(new CharSequence[] {"Entfernen", "Umbenennen"}, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch(which) {
						case 0:
							new DictionaryRemover().removeDictionary(file_id);
							break;
						case 1:
							renameDictionary(file_id);
							break;
						}
					}
				})
				.show();
				return true;
			}
		});

    }

    public void renameDictionary(int fileId) {
    	final EditText txt = new EditText(DictionarySelectActivity.this);
    	final String dictionary_id = String.valueOf(fileId);
    	Cursor cur = db.rawQuery("SELECT "+DictionaryOpenHelper.KEY_TITLE+" FROM "+DictionaryOpenHelper.FILELIST_TABLE_NAME+" WHERE "+DictionaryOpenHelper.KEY_FILEID+" = ?", new String[]{dictionary_id});
    	if (!cur.moveToFirst()) return;
    	txt.setText(cur.getString(0));
    	cur.close();
    	new AlertDialog.Builder(DictionarySelectActivity.this)
		.setTitle("Wörterbuch umbenennen")
		.setView(txt)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ContentValues values = new ContentValues();
				values.put(DictionaryOpenHelper.KEY_TITLE, txt.getText().toString());
				db.update(DictionaryOpenHelper.FILELIST_TABLE_NAME, values, DictionaryOpenHelper.KEY_FILEID+" = ?", new String[]{dictionary_id});
				dialog.dismiss();
				fillList();
			}
		})
		.show();
    }
    

    public class DictionaryRemover extends AsyncTask<Integer, Long, Boolean> {
        
    	private ProgressDialog progdialog;
    	
    	public void removeDictionary(int fileId) {
    		progdialog = ProgressDialog.show(DictionarySelectActivity.this, null, "Wörterbuch wird gelöscht...", true);
    		this.execute(fileId);
    	}
    	
		@Override
		protected Boolean doInBackground(Integer... params) {
			int fileId = params[0];
			try {
				db.delete(DictionaryOpenHelper.FILELIST_TABLE_NAME, DictionaryOpenHelper.KEY_FILEID + " = ?", new String[] {Integer.toString(fileId)});
		    	db.delete(DictionaryOpenHelper.DICTIONARY_TABLE_NAME, DictionaryOpenHelper.KEY_FILEID + " = ?", new String[] {Integer.toString(fileId)});
				return true;
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			progdialog.dismiss();
			fillList();
			super.onPostExecute(result);
		}
		
    }
    
    
    
    public void importDictionary() {
    	startActivityForResult(new Intent(DictionarySelectActivity.this, DictionaryImportActivity.class), RC_IMPORT_DONE);
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if (requestCode == RC_IMPORT_DONE) {
    		fillList();
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dictselect_menu, menu);
        return true;
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.import_dictionary:
            importDictionary();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
}
