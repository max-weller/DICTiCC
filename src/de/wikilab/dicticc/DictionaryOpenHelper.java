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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DictionaryOpenHelper extends SQLiteOpenHelper {

	public static final String KEY_FILENAME = "sFilename";
	public static final String KEY_TITLE = "sTitle";
	
	public static final String KEY_FILEID = "iFileId";
	public static final String KEY_WORD_LENGTH = "sWordLen";
	public static final String KEY_WORD_LOWERCASE = "sWordLc";
	public static final String KEY_WORD = "sWord";
	public static final String KEY_WORD_EXTRA = "sWordEx";
	public static final String KEY_WORD_GENDER = "sWordGen";
	public static final String KEY_DEFINITION = "sDef";
	public static final String KEY_DEFINITION_EXTRA = "sDefEx";
	public static final String KEY_DEFINITION_GENDER = "sDefGen";
	public static final String KEY_TYPE = "sType";

	public static final String KEY_LANG1 = "sLang1";
	public static final String KEY_LANG2 = "sLang2";

	private static final String DATABASE_NAME = "dict";
    private static final int DATABASE_VERSION = 12;
    public static final String DICTIONARY_INDEX_NAME = "dictionary_idx";
    public static final String DICTIONARY_TABLE_NAME = "dictionary";
    private static final String DICTIONARY_TABLE_CREATE =
                "CREATE TABLE " + DICTIONARY_TABLE_NAME + " (" +
                "_id integer primary key autoincrement, " +
                KEY_FILEID + " integer, " +
                KEY_WORD_LENGTH + " integer, " +
                KEY_WORD_LOWERCASE + " TEXT, " +
                KEY_WORD + " TEXT, " +
                KEY_WORD_EXTRA + " TEXT, " +
                KEY_WORD_GENDER + " TEXT, " +
                KEY_DEFINITION + " TEXT, " +
                KEY_DEFINITION_EXTRA + " TEXT, " +
                KEY_DEFINITION_GENDER + " TEXT, " +
                KEY_TYPE + " TEXT);";

    public static final String FILELIST_TABLE_NAME = "filelist";
    private static final String FILELIST_TABLE_CREATE =
                "CREATE TABLE " + FILELIST_TABLE_NAME + " (" +
                KEY_FILEID + "  integer primary key autoincrement, " +
                KEY_FILENAME + " TEXT, " +
                KEY_TITLE + " TEXT, " +
                KEY_LANG1 + " TEXT, " +
                KEY_LANG2 + " TEXT);";

    DictionaryOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
        db.execSQL(FILELIST_TABLE_CREATE);
    }

    private void clearDatabase(SQLiteDatabase db) {
		db.execSQL("DROP INDEX IF EXISTS "+DICTIONARY_INDEX_NAME);
		db.execSQL("DROP TABLE IF EXISTS "+DICTIONARY_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS "+FILELIST_TABLE_NAME);
    }
    
	@Override
	public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
		clearDatabase(db);
        db.execSQL(DICTIONARY_TABLE_CREATE);
        db.execSQL(FILELIST_TABLE_CREATE);
        db.execSQL("CREATE INDEX "+DICTIONARY_INDEX_NAME+" ON "+DICTIONARY_TABLE_NAME+" ("+KEY_FILEID+", "+KEY_WORD_LENGTH+", "+KEY_WORD_LOWERCASE+")");
	}
}