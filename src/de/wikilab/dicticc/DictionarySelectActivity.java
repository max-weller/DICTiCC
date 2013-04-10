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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.KeyStore.LoadStoreParameter;
import java.util.ArrayList;
import java.util.Collections;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;

public class DictionarySelectActivity extends ListActivity {

	final int RC_IMPORT_DONE = 1;
	
	boolean emptyList;
	ArrayList<Dictfileitem> itemlist;
	
	private CheckedListAdapter la;
	
	public static class Dictfileitem implements Comparable<Dictfileitem> {
		public String title,file_prefix; boolean selected;
		@Override
		public String toString() {
			return title;
		}
		@Override
		public int compareTo(Dictfileitem arg0) {
			return title.compareTo(arg0.title);
		}
	}
	
    public void fillList() {
    	SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(DictionarySelectActivity.this);
        String currentDictFile = pref.getString("dictFile", null);
        boolean sortList = pref.getBoolean("behav_sort_dict_list", true);
        
    	File[] subfiles = new File(Dict.getPath(DictionarySelectActivity.this)).listFiles();
    	itemlist = new ArrayList<Dictfileitem>();
        for(int i = 0; i < subfiles.length; i++) {
        	String name = subfiles[i].getName();
        	if (name.endsWith("_info")) {
        		Dictfileitem item = new Dictfileitem();
        		item.file_prefix = name.substring(0, name.length()-5);
        		
        		if (item.file_prefix.equals(currentDictFile)) item.selected = true;
        		
        		try {
	        		BufferedReader bfr = new BufferedReader(new InputStreamReader(Dict.openForRead(DictionarySelectActivity.this, name)));
	        		item.title = bfr.readLine();
	        		bfr.close();
        		} catch (Exception ex) {}
        		if (item.title == null) item.title = name;
        		itemlist.add(item);
        	}
        }
        
        
        if (itemlist.size() == 0) {
        	emptyList = true;
        	setListAdapter(new GenericStringAdapter(DictionarySelectActivity.this, R.layout.listitem_plaintext, R.id.text, getLayoutInflater(), new String[]{getString(R.string.no_dictionaries_helptext)}, true));
        } else {
        	emptyList = false;
        	
        	if (sortList) Collections.sort(itemlist);
	        
        	la = new CheckedListAdapter(DictionarySelectActivity.this, R.layout.listitem_dictionary, android.R.id.content, R.id.selector, getLayoutInflater(), itemlist, false);
        	setListAdapter(la);
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fillList();
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        //lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        lv.setOnItemClickListener(new OnItemClickListener() {
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          	if (emptyList) return;
          	
          	Dictfileitem item = (Dictfileitem)itemlist.get(position);
        	String data = item.file_prefix;
          	Intent resultIntent = new Intent();
          	resultIntent.putExtra(Intent.EXTRA_TEXT, data);
          	resultIntent.putExtra("title", item.title);
          	//resultIntent.putExtra(Intent.EXTRA_UID, file_id_list[position]);
          	setResult(RESULT_OK, resultIntent);
          	finish();
          }
        });
        
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (emptyList) return false;

	          	final Dictfileitem item = (Dictfileitem)itemlist.get(position);
				new AlertDialog.Builder(DictionarySelectActivity.this)
				.setTitle(R.string.dictionary)
				.setItems(R.array.dict_context_menu, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch(which) {
						case 0:
							removeDictionary(item.file_prefix);
							break;
						case 1:
							renameDictionaryDialog(item.file_prefix, item.title);
							break;
						case 2:
							try {
								new AlertDialog.Builder(DictionarySelectActivity.this)
								.setMessage(Dict.readFile(new File(Dict.getPath(DictionarySelectActivity.this), item.file_prefix + "_info").getAbsolutePath()))
								.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								})
								.show();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								new AlertDialog.Builder(DictionarySelectActivity.this)
								.setMessage("IO Exception:\n\n"+e.toString())
								.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								})
								.show();
							}
							break;
						}
					}
				})
				.show();
				return true;
			}
		});

    }
    
    public void removeDictionary(String filePrefix) {
    	ProgressDialog pd = new ProgressDialog(DictionarySelectActivity.this);
    	pd.setMessage(getString(R.string.dictionary_deleting));
    	pd.show();
    	
    	int counter1=0,counter2=0;
    	File[] subfiles = new File(Dict.getPath(DictionarySelectActivity.this)).listFiles();
        for(int i = 0, j = 0; i < subfiles.length; i++) {
        	String name = subfiles[i].getName();
        	if (name.startsWith(filePrefix)) {
        		counter2++;
        		if (subfiles[i].delete()) counter1++;
        	}
        }
        
        pd.dismiss();
        new AlertDialog.Builder(DictionarySelectActivity.this)
        .setTitle(R.string.message_dictionary_deleted_title)
        .setMessage(String.format(getString(R.string.message_dictionary_deleted_title), counter1, counter2))
        .setPositiveButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				arg0.dismiss();
				fillList();
			}
		})
        .show();
    }


    public void renameDictionaryDialog(String _filePrefix, String oldTitle) {
    	final String filePrefix = _filePrefix;
    	final EditText txt = new EditText(DictionarySelectActivity.this);
    	txt.setText(oldTitle);
    	new AlertDialog.Builder(DictionarySelectActivity.this)
		.setTitle(R.string.dictionary_rename)
		.setView(txt)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				renameDictionary(filePrefix, txt.getText().toString());
				dialog.dismiss();
				fillList();
			}
		})
		.show();
    }
    
    void renameDictionary(String filePrefix, String newTitle) {
    	try {
	    	String fileContent = Dict.readFile(new File(Dict.getPath(DictionarySelectActivity.this), filePrefix+"_info").getAbsolutePath());
	    	
	    	FileOutputStream fos1 = Dict.openForWrite(DictionarySelectActivity.this, filePrefix+"_info", false);
			BufferedWriter writeInfo = new BufferedWriter(new OutputStreamWriter(fos1));
			
			int pos = fileContent.indexOf('\n');
			if (pos == -1) {
				writeInfo.write(newTitle + '\n');
			} else {
				writeInfo.write(newTitle);
				writeInfo.write(fileContent.substring(pos));
			}
			writeInfo.close();
    	} catch (Exception e) {
    		MessageBox.alert(DictionarySelectActivity.this, getString(R.string.errmes_dictionary_rename_failed) + "\n" + e.getMessage());
    	}
    }
    
    
/*
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
    */
    
    
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
        case R.id.download_dictionaries:
        	startActivity(new Intent(DictionarySelectActivity.this, DownloadFrameActivity.class));
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
}
