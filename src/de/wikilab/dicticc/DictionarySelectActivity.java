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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
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
	String[] file_prefix_list;
	
    public String getDictDirectory() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DictionarySelectActivity.this);
		return prefs.getString("dict_directory", "/mnt/sdcard/dictionaries/");
    }
	
    public void fillList() {
    	
    	File[] subfiles = this.getFilesDir().listFiles();
        ArrayList<String> files = new ArrayList<String>();
        ArrayList<String> file_prefixes = new ArrayList<String>();
        for(int i = 0; i < subfiles.length; i++) {
        	String name = subfiles[i].getName();
        	if (name.endsWith("_info")) {
        		file_prefixes.add(name.substring(0, name.length()-5));
        		try {
	        		BufferedReader bfr = new BufferedReader(new InputStreamReader(openFileInput(name)));
	        		String firstLine = bfr.readLine();
	        		bfr.close();
	        		files.add(firstLine);
        		} catch (Exception ex) {
        			files.add(name);
        		}
        	}
        }
        

        if (files.size() == 0) {
        	emptyList = true;
        	file_list = null;
        	file_prefix_list = null;
        	setListAdapter(new GenericStringAdapter(DictionarySelectActivity.this, R.layout.listitem_dictionary, R.id.text, getLayoutInflater(), new String[]{getString(R.string.no_dictionaries_helptext)}, true));
        } else {
        	emptyList = false;
        	file_list = files.toArray(new String[0]);
        	file_prefix_list = file_prefixes.toArray(new String[0]);
	        
	        setListAdapter(new GenericStringAdapter(DictionarySelectActivity.this, R.layout.listitem_dictionary, R.id.text, getLayoutInflater(), file_list, false));

        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fillList();
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);

        lv.setOnItemClickListener(new OnItemClickListener() {
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          	if (emptyList) return;
          	
        	String data = file_prefix_list[position];
          	Intent resultIntent = new Intent();
          	resultIntent.putExtra(Intent.EXTRA_TEXT, data);
          	resultIntent.putExtra("title", file_list[position]);
          	//resultIntent.putExtra(Intent.EXTRA_UID, file_id_list[position]);
          	setResult(RESULT_OK, resultIntent);
          	finish();
          }
        });
        
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (emptyList) return false;

				final String file_prefix = file_prefix_list[position];
				final String file_title = file_list[position];
				new AlertDialog.Builder(DictionarySelectActivity.this)
				.setTitle("Wörterbuch")
				.setItems(new CharSequence[] {"Entfernen", "Umbenennen", "Details"}, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch(which) {
						case 0:
							
							break;
						case 1:
							renameDictionaryDialog(file_prefix, file_title);
							break;
						case 2:
							try {
								new AlertDialog.Builder(DictionarySelectActivity.this)
								.setMessage(Dict.readFile(new File(getFilesDir(), file_prefix + "_info").getAbsolutePath()))
								.setPositiveButton("Schließen", new DialogInterface.OnClickListener() {
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
								.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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


    public void renameDictionaryDialog(String _filePrefix, String oldTitle) {
    	final String filePrefix = _filePrefix;
    	final EditText txt = new EditText(DictionarySelectActivity.this);
    	txt.setText(oldTitle);
    	new AlertDialog.Builder(DictionarySelectActivity.this)
		.setTitle("Wörterbuch umbenennen")
		.setView(txt)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
	    	String fileContent = Dict.readFile(new File(getFilesDir(), filePrefix+"_info").getAbsolutePath());
	    	
	    	FileOutputStream fos1 = openFileOutput(filePrefix+"_info",MODE_WORLD_READABLE);
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
    		MessageBox.alert(DictionarySelectActivity.this, "Umbenennen nicht möglich:\n"+e.getMessage());
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
