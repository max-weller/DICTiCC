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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Stack;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class DebugActivity extends ListActivity {

	private static final String TAG = "DebugActivity";
	
	String[] file_list;
	Stack<String> currentFolder = new Stack<String>();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState != null) {
        	currentFolder.addAll(Arrays.asList(savedInstanceState.getStringArray("path")));
        } else {
        	currentFolder.push("data");
        	currentFolder.push("data");
        	currentFolder.push("de.wikilab.dicticc");
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
        
        setListAdapter(new GenericStringAdapter(DebugActivity.this, R.layout.listitem_dictionary, R.id.text, getLayoutInflater(), file_list, false));
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
    		File f = new File(getFolder(), fileName);
    		
    		try {
				new AlertDialog.Builder(this)
				.setMessage(Dict.readFile(f.getAbsolutePath()))
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
				new AlertDialog.Builder(this)
				.setMessage("IO Exception:\n\n"+e.toString())
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.show();
			}
    		
    	}
    	
    }
    
    
    
}
