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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import java.text.Normalizer;

public class Dict {
	public static final int LCWORD = 0;
	public static final int WORD = 1;
	public static final int WORD_GENDER = 2;
	public static final int WORD_EXTRA = 3;
	public static final int DEF = 4;
	public static final int DEF_GENDER = 5;
	public static final int DEF_EXTRA = 6;
	public static final int TYPE = 7;
	
	public static final String dictFolder = "Android/data/de.wikilab.dicticc/imported";
	//public static final String dictFolder = "dictionaries/imported";
	
	
	public String filePrefix,fromLang="",toLang="",title="";

    private static final Pattern patternDiacriticalMarks = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	public static String deAccent(String str) {
	    String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
	    return patternDiacriticalMarks.matcher(nfdNormalizedString).replaceAll("");
	}

	
    public Dict(Context ctx, String pFilePrefix) {
    	try {
    		filePrefix = pFilePrefix;
    		BufferedReader bfr = new BufferedReader(new InputStreamReader(openForRead(ctx, filePrefix+"_info")));
    		title = bfr.readLine().trim();
    		String secondLine = bfr.readLine().trim();
    		Log.i("Dict", "secondLine >>>"+secondLine+"<<<");
    		bfr.close();
    		fromLang = secondLine.substring(2,4); toLang = secondLine.substring(5, 7);
    		Log.i("Dict", "filePrefix="+filePrefix+", fromLang="+fromLang+", toLang="+toLang);
		} catch (Exception ex) {
			ex.printStackTrace();
			title="INVALID!"; fromLang = "ERR"; toLang = "ERR";
		}
    }
    
    public static String readFile(String path) throws IOException {
    	FileInputStream stream = new FileInputStream(new File(path));
    	try {
    		FileChannel fc = stream.getChannel();
    		MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
    		/* Instead of using default, pass in a decoder. */
    		return Charset.defaultCharset().decode(bb).toString();
    	}
    	finally {
    		stream.close();
    	}
    }
    
    public static String getPath(Context ctx) {
    	SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        
    	switch(pref.getString("location_path", "sdcard").charAt(0)) {
    	case 'a':
    		return ctx.getFilesDir().getAbsolutePath();
    	case 's':
    		File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), dictFolder);
    		f.mkdirs();
    		return f.getAbsolutePath();
    	//case 'e':
    		//return Environment.
    	default:
    		return ctx.getFilesDir().getAbsolutePath();
    	}
    }
    
    public static FileOutputStream openForWrite(Context ctx, String fileName, boolean append) throws FileNotFoundException {
    	SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        
    	switch(pref.getString("location_path", "sdcard").charAt(0)) {
    	case 's':
    		File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), dictFolder+"/");
    		f.mkdirs();
    		return new FileOutputStream(new File(f, fileName), append);
    	//case 'e':
    		//return Environment.
    	default:
    	case 'a':
    		return ctx.openFileOutput(fileName, (append ? Context.MODE_APPEND : 0));
    	}
    }
    
    public static FileInputStream openForRead(Context ctx, String fileName) throws FileNotFoundException {
    	SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        
    	switch(pref.getString("location_path", "sdcard").charAt(0)) {
    	case 's':
    		File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), dictFolder);
    		f.mkdirs();
    		File ff = new File(f, fileName);
    		if (ff.exists() == false) return null;
    		return new FileInputStream(ff);
    	//case 'e':
    		//return Environment.
    	default:
    	case 'a':
    		return ctx.openFileInput(fileName);
    	}
    }
    
    public static List<Dict> searchForLanguage(Context ctx, String fromLang, String toLang) {
    	File[] subfiles = new File(getPath(ctx)).listFiles();
    	String file_prefix;
        ArrayList<Dict> language = new ArrayList<Dict>();
        for(int i = 0; i < subfiles.length; i++) {
        	String name = subfiles[i].getName();
        	if (name.endsWith("_info")) {
        		file_prefix = name.substring(0, name.length()-5);
        		
        		Dict dict = new Dict(ctx, file_prefix);
        		Log.v("Dict", "fromLang="+dict.fromLang+"; toLang="+dict.toLang+";");
        		if (fromLang != null && !fromLang.equalsIgnoreCase(dict.fromLang)) continue;
        		if (toLang != null && !toLang.equalsIgnoreCase(dict.toLang)) continue;
        		
        		language.add(dict);
        	}
        }
        return language;
    }
}
