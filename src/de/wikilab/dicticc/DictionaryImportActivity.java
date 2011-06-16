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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class DictionaryImportActivity extends ListActivity {

	private static final String TAG = "DictionaryImportActivity";
	
	String[] file_list;
	Stack<String> currentFolder = new Stack<String>();
	SQLiteDatabase db;
	
	ProgressDialog progdialog;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        DictionaryOpenHelper dictoh = new DictionaryOpenHelper(DictionaryImportActivity.this);
        db = dictoh.getWritableDatabase();
        
        if (savedInstanceState != null) {
        	currentFolder.addAll(Arrays.asList(savedInstanceState.getStringArray("path")));
        } else {
        	currentFolder.push("sdcard");
        }
        
        fillList();
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        
    }
    
    protected void onSaveInstanceState(Bundle outState) {
    	outState.putStringArray("path", currentFolder.toArray(new String[0]));
    }
    
    String getFolder() {
    	return "/" + TextUtils.join("/", currentFolder.toArray());
    }
    
    void fillList() {
        File[] subfiles = new File(getFolder()).listFiles();
        file_list = new String[subfiles.length+1];
        file_list[0] = "..";
        for(int i = 0; i < subfiles.length; i++) file_list[i+1] = (subfiles[i].isDirectory() ? "/" : "") + subfiles[i].getName();
        Arrays.sort(file_list, String.CASE_INSENSITIVE_ORDER);
        
        setListAdapter(new GenericStringAdapter(DictionaryImportActivity.this, R.layout.listitem_dictionary, R.id.text, getLayoutInflater(), file_list, false));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dictimport_menu, menu);
        return true;
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.import_all_dictionaries:
            
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	//super.onListItemClick(l, v, position, id);
    	Log.i(TAG, "onListItemClick: " + position);
    	final String fileName = ((TextView)v.findViewById(R.id.text)).getText().toString();
    	Log.i(TAG, "fileName: " + fileName);
    	if (fileName.charAt(0) == '/') {
    		currentFolder.push(fileName.substring(1));
    		fillList();
    	} else if (fileName.equals("..")) {
    		currentFolder.pop();
    		fillList();
    	} else {
    		new AlertDialog.Builder(this)
    		.setMessage("Diese Datei als Wörterbuch importieren?")
    		.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					importDictionary(getFolder() + "/" + fileName);
				}
			})
			.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.show();
    		
    	}
    	
    }
    
    public void importDictionary(String fileSpec) {
    	progdialog = ProgressDialog.show(DictionaryImportActivity.this, null, getString(R.string.importing_dictionary_long) +"Initialisieren");
    	
    	Log.i(TAG, "going to call DictionaryImporter for fileSpec=" + fileSpec);
    	DictionaryImporter imp = new DictionaryImporter();
    	imp.execute(fileSpec);
    }
    

    public class DictionaryImporter extends AsyncTask<String, Long, Long[]> {
        
    	private static final String TAG = "DictionaryImportActivity/DictionaryImporter";
    	
    	ListView targetList;
    	String dictFile;
    	
		@Override
		protected Long[] doInBackground(String... params) {
			long anfang = android.os.Process.getElapsedCpuTime();
			dictFile = params[0];
			
			File dictFileInfo = new File(dictFile);
			ArrayList<String[]> result = new ArrayList<String[]>();
			
			long c = 0, ok = 0;
			
			try {
				// Transaction: schneller ca. faktor 100
				db.beginTransaction();

				ContentValues fileValues;
				fileValues = new ContentValues();
				fileValues.put(DictionaryOpenHelper.KEY_FILENAME, dictFile);
				fileValues.put(DictionaryOpenHelper.KEY_TITLE, dictFile);
				fileValues.put(DictionaryOpenHelper.KEY_LANG1, "Deutsch");
				fileValues.put(DictionaryOpenHelper.KEY_LANG2, "Englisch");
		    	
		    	String newID = String.valueOf(db.insert(DictionaryOpenHelper.FILELIST_TABLE_NAME, null, fileValues));
		    	Log.v(TAG, "Inserted new File: "+newID+" - "+dictFile);
		    	
		    	FileInputStream fis = new FileInputStream(dictFileInfo);
				BufferedReader read = new BufferedReader(new InputStreamReader(fis));
				
				//values = new ContentValues();
				String w1,w2;
				String line, lastTerm = null;
				String[] cols;
				SQLiteStatement insertCommand = db.compileStatement("INSERT INTO "+DictionaryOpenHelper.DICTIONARY_TABLE_NAME+" VALUES (null,?,?,?,?,?,?,?,?,?,?)");
				DatabaseUtils.bindObjectToProgram(insertCommand, 1, newID);
				while ((line = read.readLine()) != null) {
					if (line.length() == 0 || line.charAt(0) == '#') { Log.v(TAG, "skipped empty Line: >"+line+"<"); continue; }
					int pos0, pos1 = line.indexOf('\t'), pos2 = line.lastIndexOf('\t');
					if (pos1==-1||pos2==pos1) { Log.v(TAG, "skipped invalid Line: >"+line+"<"); continue; }
					
					w1 = line.substring(0,pos1);
					w2 = line.substring(pos1+1,pos2);
					pos1 = w1.indexOf('{');
					if (pos1 != -1) {
						pos0 = w1.lastIndexOf('}'); if(pos0==-1)pos0=w1.length()-1;
						DatabaseUtils.bindObjectToProgram(insertCommand, 6, w1.substring(pos1,pos0+1)); //WORD_GENDER
						w1 = w1.substring(0,pos1)+w1.substring(pos0+1);
					} else {
						DatabaseUtils.bindObjectToProgram(insertCommand, 6, "");
					}
					pos1 = w1.indexOf('[');
					if (pos1 != -1) {
						pos0 = w1.lastIndexOf(']'); if(pos0==-1)pos0=w1.length()-1;
						DatabaseUtils.bindObjectToProgram(insertCommand, 5, w1.substring(pos1,pos0+1)); //WORD_EXTRA
						w1 = w1.substring(0,pos1)+w1.substring(pos0+1);
					} else {
						DatabaseUtils.bindObjectToProgram(insertCommand, 5, "");
					}
					
					pos1 = w2.indexOf('{');
					if (pos1 != -1) {
						pos0 = w2.lastIndexOf('}'); if(pos0==-1)pos0=w2.length()-1; 
						DatabaseUtils.bindObjectToProgram(insertCommand, 9, w2.substring(pos1,pos0+1)); //DEFINITION_GENDER
						w2 = w2.substring(0,pos1)+w2.substring(pos0+1);
					} else {
						DatabaseUtils.bindObjectToProgram(insertCommand, 9, "");
					}
					pos1 = w2.indexOf('[');
					if (pos1 != -1) {
						pos0 = w2.lastIndexOf(']'); if(pos0==-1)pos0=w2.length()-1; 
						DatabaseUtils.bindObjectToProgram(insertCommand, 8, w2.substring(pos1,pos0+1)); //DEFINITION_EXTRA
						w2 = w2.substring(0,pos1)+w2.substring(pos0+1);
					} else {
						DatabaseUtils.bindObjectToProgram(insertCommand, 8, "");
					}

					DatabaseUtils.bindObjectToProgram(insertCommand, 4, w1); //WORD
					w1 = w1.trim();
					DatabaseUtils.bindObjectToProgram(insertCommand, 3, w1.toLowerCase()); //WORD_LOWERCASE
					DatabaseUtils.bindObjectToProgram(insertCommand, 2, w1.length()); //WORD_LENGTH
					DatabaseUtils.bindObjectToProgram(insertCommand, 7, w2); //DEFINITION
					DatabaseUtils.bindObjectToProgram(insertCommand, 10, line.substring(pos2+1)); //TYPE
					insertCommand.execute();
					//db.execSQL("INSERT INTO "+DictionaryOpenHelper.DICTIONARY_TABLE_NAME+" VALUES (null,?,?,?,?,?,?,?,?)",
					//		new String[] { newID, w1, ex1, g1, w2, ex2, g2, line.substring(pos2+1) });
					
					
					
					//values.put(DictionaryOpenHelper.KEY_FILEID, newID);   //1
					//values.put(DictionaryOpenHelper.KEY_WORD_LENGTH, w1); //2
					//values.put(DictionaryOpenHelper.KEY_WORD_LOWERCASE, w1);//3
					//values.put(DictionaryOpenHelper.KEY_WORD, w1);        //4
					//values.put(DictionaryOpenHelper.KEY_WORD_EXTRA, ex1); //5
					//values.put(DictionaryOpenHelper.KEY_WORD_GENDER, g1); //6
					//values.put(DictionaryOpenHelper.KEY_DEFINITION, w2);  //7
					//values.put(DictionaryOpenHelper.KEY_DEFINITION_EXTRA, ex2);//8
					//values.put(DictionaryOpenHelper.KEY_DEFINITION_GENDER, g2);//9
					//values.put(DictionaryOpenHelper.KEY_TYPE, line.substring(pos2+1));//10
					
					//long newID2 = db.insert(DictionaryOpenHelper.DICTIONARY_TABLE_NAME, null, values);
					//Log.v("DictionaryImporter", "importing word: ["+newID2+"] "+parts[0]);
					
					c++;
					
					if ((c % 4713) == 0) publishProgress(c);
				}


				db.setTransactionSuccessful();
				ok = 1;
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				db.endTransaction();
			}

			long ende = android.os.Process.getElapsedCpuTime();
			return new Long[] {c,anfang,ende, ok};
		}
		
		@Override
		protected void onProgressUpdate(Long... values) {
			progdialog.setMessage(getString(R.string.importing_dictionary_long) + dictFile + "\n" + values[0] + " Wörter importiert");
			super.onProgressUpdate(values);
		}
		
		@Override
		protected void onPostExecute(Long[] result) {
			progdialog.dismiss();
			if (result[3] == 1) {
		        new AlertDialog.Builder(DictionaryImportActivity.this)
		        .setMessage("Wörterbuch erfolgreich importiert. Es enthält " + result[0] + " Wörter. Dies dauerte " + (result[2] - result[1]) + " ms.")
		        .setPositiveButton("OK", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						finish();
					}
				})
				.setNeutralButton("Weitere importieren", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.show();
			} else  {
				new AlertDialog.Builder(DictionaryImportActivity.this)
		        .setMessage("Aufgrund eines Fehlers wurde der Import nach " + result[0] + " Wörtern abgebrochen. Eventuell ist die Datei fehlerhaft. Dies dauerte " + (result[2] - result[1]) + " ms.")
		        .setIcon(android.R.drawable.ic_dialog_alert)
		        .setNeutralButton("Schließen", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.show();
			}
			super.onPostExecute(result);
		}
		
    }
    
    
}
