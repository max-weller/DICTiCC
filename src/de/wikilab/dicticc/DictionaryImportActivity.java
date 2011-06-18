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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
	
	ProgressDialog progdialog;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(DictionaryImportActivity.this);
        setFolder(pref.getString("importDialogCurrentFolder", "/sdcard"));
        
        fillList();
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        
    }
    @Override
    protected void onPause() {
    	super.onPause();
    	SharedPreferences.Editor pref = PreferenceManager.getDefaultSharedPreferences(DictionaryImportActivity.this).edit();
        pref.putString("importDialogCurrentFolder", getFolder());
        pref.commit();
    }
    

    String getFolder() {
    	return "/" + TextUtils.join("/", currentFolder.toArray());
    }
    void setFolder(String folder) {
    	String[] s = folder.split("/");
    	currentFolder.clear();
    	for(String d : s) if (!d.equals("")) currentFolder.push(d);
    	updateTitleBar();
    }
    void updateTitleBar() {
    	setTitle(getString(R.string.import_dictionary) + " - " + getFolder());
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
    		updateTitleBar();
    	} else if (fileName.equals("..")) {
    		currentFolder.pop();
    		fillList();
    		updateTitleBar();
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
			
			long c = 0, ok = 0;
			BufferedWriter writeInfo = null, writeCurrent = null;
			
			try {

				BufferedReader read;
				File dictFileInfo = new File(dictFile);
				
				if (dictFile.substring(dictFile.length()-4).toLowerCase().equals(".zip")) {
					Log.i(TAG, "using ZipFile");
					ZipFile zip = new ZipFile(dictFileInfo);
					ZipEntry item = zip.entries().nextElement();
					read = new BufferedReader(new InputStreamReader(zip.getInputStream(item)));
				} else {
					FileInputStream fis = new FileInputStream(dictFileInfo);
					read = new BufferedReader(new InputStreamReader(fis));
				}
				
				String filePrefix = dictFileInfo.getName()+"_";
				
				Log.v(TAG, "Inserting new File: "+dictFile);
		    	
		    	
				FileOutputStream fos1 = openFileOutput(filePrefix+"info",MODE_WORLD_READABLE);
				writeInfo = new BufferedWriter(new OutputStreamWriter(fos1));
				
				//values = new ContentValues();

				ArrayList<String[]> result = new ArrayList<String[]>();
				String[] r;
				String line;
				char lastChar0 = 0, ch0, lastChar1 = 0, ch1;
				boolean firstLine = true;
				while ((line = read.readLine()) != null) {
					if (line.length() == 0) continue;
					ch0 = line.charAt(0);
					if (ch0 == '#') {
						if (firstLine && line.length() > 7) { writeInfo.write(line.substring(2, 8) + "\n"); firstLine = false; }
						writeInfo.write(line + "\n");
						continue;
					}
					int pos0, pos1 = line.indexOf('\t'), pos2 = line.lastIndexOf('\t');
					if (pos1==-1||pos2==pos1) { Log.v(TAG, "skipped invalid Line: >"+line+"<"); continue; }
					
					ch1 = line.charAt(1);
					
					if (Character.isLetter(ch0)) ch0 = Character.toLowerCase(ch0); else ch0 = '$';
					if (Character.isLetter(ch1)) ch1 = Character.toLowerCase(ch1); else ch1 = '$';
					if (ch0 != lastChar0 || ch1 != lastChar1) {
						Log.i(TAG, "new ch0/1: '"+ch0+ch1+"'");
						//if (writeCurrent != null)
						if (lastChar0 != 0) {
							writeToFile(lastChar0, lastChar1, filePrefix, result.toArray(new String[0][0]));
							result = new ArrayList<String[]>();
						}
						lastChar0 = ch0; lastChar1 = ch1;
					}
					
					r = new String[8];
					r[Dict.WORD] = line.substring(0,pos1);
					r[Dict.DEF] = line.substring(pos1+1,pos2);
					pos1 = r[Dict.WORD].indexOf('{');
					if (pos1 != -1) {
						pos0 = r[Dict.WORD].lastIndexOf('}'); if(pos0==-1)pos0=r[Dict.WORD].length()-1;
						r[Dict.WORD_GENDER]= r[Dict.WORD].substring(pos1,pos0+1); //WORD_GENDER
						r[Dict.WORD] = r[Dict.WORD].substring(0,pos1)+r[Dict.WORD].substring(pos0+1);
					} else {
						r[Dict.WORD_GENDER] = "";
					}
					pos1 = r[Dict.WORD].indexOf('[');
					if (pos1 != -1) {
						pos0 = r[Dict.WORD].lastIndexOf(']'); if(pos0==-1)pos0=r[Dict.WORD].length()-1;
						r[Dict.WORD_EXTRA]= r[Dict.WORD].substring(pos1,pos0+1); //WORD_EXTRA
						r[Dict.WORD] = r[Dict.WORD].substring(0,pos1)+r[Dict.WORD].substring(pos0+1);
					} else {
						r[Dict.WORD_EXTRA] = "";
					}
					
					pos1 = r[Dict.DEF].indexOf('{');
					if (pos1 != -1) {
						pos0 = r[Dict.DEF].lastIndexOf('}'); if(pos0==-1)pos0=r[Dict.DEF].length()-1; 
						r[Dict.DEF_GENDER]= r[Dict.DEF].substring(pos1,pos0+1); //DEFINITION_GENDER
						r[Dict.DEF] = r[Dict.DEF].substring(0,pos1)+r[Dict.DEF].substring(pos0+1);
					} else {
						r[Dict.DEF_GENDER] = "";
					}
					pos1 = r[Dict.DEF].indexOf('[');
					if (pos1 != -1) {
						pos0 = r[Dict.DEF].lastIndexOf(']'); if(pos0==-1)pos0=r[Dict.DEF].length()-1; 
						r[Dict.DEF_EXTRA]= r[Dict.DEF].substring(pos1,pos0+1); //DEFINITION_EXTRA
						r[Dict.DEF] = r[Dict.DEF].substring(0,pos1)+r[Dict.DEF].substring(pos0+1);
					} else {
						r[Dict.DEF_EXTRA] = "";
					}
					
					r[Dict.WORD] = r[Dict.WORD].trim();
					r[Dict.TYPE] = line.substring(pos2+1);
					r[Dict.LCWORD] = r[Dict.WORD].toLowerCase();
					//r[Dict.WORDLEN] = r[Dict.WORD].length();
					
					result.add(r);
					//for(int i=0; i<=Dict.TYPE; i++) { writeCurrent.write(r[i]); writeCurrent.write('\t'); }
					//writeCurrent.write('\n');
					
					/*DatabaseUtils.bindObjectToProgram(insertCommand, 4, w1); //WORD
					DatabaseUtils.bindObjectToProgram(insertCommand, 3, w1.toLowerCase()); //WORD_LOWERCASE
					DatabaseUtils.bindObjectToProgram(insertCommand, 2, w1.length()); //WORD_LENGTH
					DatabaseUtils.bindObjectToProgram(insertCommand, 7, w2); //DEFINITION
					DatabaseUtils.bindObjectToProgram(insertCommand, 10, line.substring(pos2+1)); //TYPE
					insertCommand.execute();*/
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
				
				if (lastChar0 != 0) {
					writeToFile(lastChar0, lastChar1, filePrefix, result.toArray(new String[0][0]));
				}
				
				read.close();
				ok = 1;
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, e.toString());
			} finally {
				try {
					if (writeInfo != null) writeInfo.close();
					if (writeCurrent != null) writeCurrent.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			long ende = android.os.Process.getElapsedCpuTime();
			return new Long[] {c,anfang,ende, ok};
		}
		
		void writeToFile(char ch0, char ch1, String filePrefix, String[][] ar) throws IOException {
			BufferedWriter writeCurrent = new BufferedWriter(new OutputStreamWriter(openFileOutput(filePrefix+ch0+ch1,MODE_APPEND | MODE_WORLD_READABLE)));
			Arrays.sort(ar, new StringArrayByLengthComparator());
			Log.v(TAG, "writeToFile "+ch0+ch1+" \t ar.length="+ar.length);
			for(int i = 0; i < ar.length; i++) {
				for (int j = 0; j <= Dict.TYPE; j++) {
					writeCurrent.write(ar[i][j]); writeCurrent.write('\t');
				}
				writeCurrent.write('\n');
			}
			writeCurrent.close();
		}
		
		class StringArrayByLengthComparator implements Comparator<String[]> {

			@Override
			public int compare(String[] object1, String[] object2) {
				Integer l1 = object1[Dict.LCWORD].length();
				return l1.compareTo(object2[Dict.LCWORD].length());
			}
			
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
