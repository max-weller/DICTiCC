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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SearchActivity extends ActionBarActivity {
	
	final int REQ_DICTIONARY_SELECT = 1;
	
	Dict dict;
	
	ProgressDialog progdialog;
	
	ArrayList<String> lastSearches = new ArrayList<String>();
	
	boolean dictSearchInAction = false;
	
	//Settings:
	boolean hideKeyboardAfterSearch;
	boolean enableLivesearch;
	int maxResults = 400;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SearchActivity.this);

        if (pref.getBoolean("show_changelog", true) == true) {
        	MessageBox.showChangeLog(this);
        }
        
        /*
        if (pref.getBoolean("p_invert_colors", false) == true) {
        	setTheme(R.style.Light);
        } else {
        	setTheme(R.style.Dark);
        }*/
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		updateDictList();
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME);
		getSupportActionBar().setIcon(R.drawable.icon);

		dict = new Dict(this, pref.getString("dictFile", null));
        
        updateTitleBar();
        readMRU();
        
        final EditText searchbox = (EditText) findViewById(R.id.searchbox);
        final ListView listview = (ListView) findViewById(R.id.contentlist);
        
        listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView l, View v, int position, long id) {
				ListAdapter la = ((ListView) l).getAdapter();
				if (la instanceof GenericStringAdapter) {
					final String data = (String) la.getItem(position);
					if (data.startsWith("> ")) {
						searchbox.setText(data.substring(2));
						doSearch(true);
					}
				} else if (la instanceof DictListAdapter) {
					final String[] data = (String[]) la.getItem(position);

					final List<Dict> langs = Dict.searchForLanguage(SearchActivity.this, dict.toLang, null);

					String[] menu = new String[langs.size() + 1];
					menu[0] = getString(R.string.copy_translation_to_clipboard);
					for (int i = 1; i < menu.length; i++)
						menu[i] = String.format(getString(R.string.search_translation_in_language), langs.get(i - 1).toLang);

					Matcher m = Pattern.compile("<b>(.*)<\\/b>.*").matcher(data[1]);
					if (!m.matches()) return;
					final String translation = m.group(1).trim();

					new AlertDialog.Builder(SearchActivity.this)
							.setIcon(android.R.drawable.ic_dialog_info)
							.setTitle(translation)
							.setItems(menu, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0, int arg1) {
									switch (arg1) {
										case 0:
											ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
											cm.setText(translation);
											break;
										default:
											dict = langs.get(arg1 - 1);
											readMRU();
											updateTitleBar();
											searchbox.setText(translation);
											doSearch(true);
									}
								}
							})
							.show();
				}
			}
		});
        
        searchbox.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH ||
						actionId == EditorInfo.IME_ACTION_DONE ||
						event.getAction() == KeyEvent.ACTION_DOWN &&
								event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
					doSearch(true);
					return true;
				} else {
					return false;
				}
			}
		});
        
        final Drawable deleteIcon = getResources().getDrawable(R.drawable.textbox_clear);
        //deleteIcon.setBounds(0, 0, deleteIcon.getIntrinsicWidth(), deleteIcon.getIntrinsicHeight());
        deleteIcon.setBounds(0, 0, 51, 30); // wtf??? die Zahlen sind empirisch ermittelt... ab right>=52 springt die höhe des editText
        
        searchbox.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				searchbox.setCompoundDrawables(null, null, s.toString().equals("") ? null : deleteIcon, null);
				//searchbox.setCompoundDrawablesWithIntrinsicBounds(null, null, s.toString().equals("") ? null : deleteIcon, null);
				searchbox.setCompoundDrawablePadding(0);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (enableLivesearch && s.length() > 1 && dict.filePrefix != null && dictSearchInAction == false) {
					DictionarySearcher ds = new DictionarySearcher();
					ds.targetList = (ListView) findViewById(R.id.contentlist);
					ds.execute(dict.filePrefix, s.toString());
				}
				if (s.length() == 0) {
					displayMRU();
				}
			}
		});

        //searchbox.setCompoundDrawables(null, null, deleteIcon, null);
        searchbox.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (searchbox.getCompoundDrawables()[2] == null) {
					return false;
				}
				if (event.getAction() != MotionEvent.ACTION_UP) {
					return false;
				}
				if (event.getX() > searchbox.getWidth() - searchbox.getPaddingRight() - deleteIcon.getIntrinsicWidth()) {
					searchbox.setText("");
					searchbox.setCompoundDrawables(null, null, null, null);
				}
				return false;
			}
		});

        
    }

	private void swapLanguageDirection() {
		final List<Dict> langs = Dict.searchForLanguage(SearchActivity.this, dict.toLang, dict.fromLang);
		if(langs.size() > 0) {
			dict = langs.get(0);
			readMRU();
			updateTitleBar();
			EditText searchbox = (EditText) findViewById(R.id.searchbox);
			searchbox.setText("");
		} else {
			MessageBox.alert(SearchActivity.this, getString(R.string.no_reverse_dict_found));
		}
	}

	ActionBar.OnNavigationListener listNavListener = new ActionBar.OnNavigationListener() {
		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			DictfileManager.Dictfileitem item = listNavAdapter.getItem(itemPosition);
			dict = new Dict(SearchActivity.this, item.file_prefix);

			SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(SearchActivity.this).edit();
			ed.putString("dictFile", dict.filePrefix);
			ed.putString("dictTitle", dict.title);
			ed.commit();
			updateTitleBar();
			readMRU();
			displayMRU();

			return true;
		}
	};

	DictionarySpinnerAdapter listNavAdapter;
    
    
    private void addToMRU(String searchTerm) {
    	int p = lastSearches.indexOf(searchTerm);
    	if (p != -1) lastSearches.remove(p);
    	if (lastSearches.size() > 8) lastSearches.remove(lastSearches.size() - 1);
    	lastSearches.add(0, searchTerm);
    	
		try {
			FileOutputStream fos1 = Dict.openForWrite(SearchActivity.this, dict.filePrefix+"_mru", false);
			BufferedWriter mru = new BufferedWriter(new OutputStreamWriter(fos1));
			for(String s : lastSearches) {
				mru.write(s + "\n");
			}
			mru.close();
		} catch (Exception e) {
			Log.e("SearchActivity", "unable to write MRU list");
			e.printStackTrace();
		}
    }
    
    private void displayMRU() {
    	ArrayList<String> lines = new ArrayList<String>();
		lines.add(getString(R.string.enter_term_helptext));
    	
		for(String s : lastSearches) lines.add("> " + s);
		
    	((ListView)findViewById(R.id.contentlist)).setAdapter(new GenericStringAdapter(SearchActivity.this, R.layout.listitem_plaintext, R.id.text, getLayoutInflater(), lines.toArray(new String[]{}), true));
    }
    
    private void readMRU() {
		try {
			if (dict == null) return;
			lastSearches.clear();
			FileInputStream fos1 = Dict.openForRead(SearchActivity.this, dict.filePrefix+"_mru");
			if (fos1 == null) return;
			BufferedReader mru = new BufferedReader(new InputStreamReader(fos1));
			String s;
			s = mru.readLine();
			while(s != null && !s.equals("")) {
				lastSearches.add(s);
				s = mru.readLine();
			}
			mru.close();
		} catch (Exception e) {
			Toast.makeText(SearchActivity.this, R.string.unable_to_access_sdcard, Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SearchActivity.this);
        hideKeyboardAfterSearch = pref.getBoolean("behav_hide_keyboard_after_search", false);
        enableLivesearch = pref.getBoolean("behav_livesearch", false);
        try { maxResults = Integer.valueOf(pref.getString("behav_maxresults", "400")); } catch(Exception e) {}
        
        if (dict.filePrefix == null) {
        	((EditText)findViewById(R.id.searchbox)).setEnabled(false);
        	((ListView)findViewById(R.id.contentlist)).setAdapter(new GenericStringAdapter(SearchActivity.this, R.layout.listitem_plaintext, R.id.text, getLayoutInflater(), new String[]{getString(R.string.no_dictionary_selected_helptext)}, true));
        } else {
        	((EditText)findViewById(R.id.searchbox)).setEnabled(true);
        	//((ListView)findViewById(R.id.contentlist)).setAdapter(new GenericStringAdapter(SearchActivity.this, R.layout.listitem_plaintext, R.id.text, getLayoutInflater(), new String[]{getString(R.string.enter_term_helptext)}, true));
        	readMRU();
        	displayMRU();
        }
        
    }
    
    void selectDictionary() {
    	startActivityForResult(new Intent(SearchActivity.this, DictionarySelectActivity.class), REQ_DICTIONARY_SELECT);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	if (savedInstanceState != null) {
        	if (savedInstanceState.getBoolean("restartSearch", false) == true) {
        		doSearch(false);
        	}
        }
    }
    
    void doSearch(boolean storeInMRU) {
    	if (dictSearchInAction) return;
    	
        final EditText searchbox = (EditText) findViewById(R.id.searchbox);
    	
    	if (dict.filePrefix == null) {
    		Toast.makeText(SearchActivity.this, R.string.errmes_no_dictionary_selected, Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	String searchTerm = searchbox.getText().toString();
    	if (searchTerm.length() < 2) {
    		Toast.makeText(SearchActivity.this, R.string.errmes_no_term_typed, Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	if (storeInMRU) addToMRU(searchTerm);
    	
    	if (hideKeyboardAfterSearch) {
	    	searchbox.clearFocus();
	    	InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
	    	imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    	}
    	//progdialog = ProgressDialog.show(SearchActivity.this, null, "Wörterbuch wird durchsucht...", true);
    	
    	DictionarySearcher ds = new DictionarySearcher();
    	ds.isLiveSearch = false;
    	ds.targetList = (ListView) findViewById(R.id.contentlist);
    	ds.execute(dict.filePrefix, searchTerm);
        return;
    }

	void updateDictList() {
		listNavAdapter = new DictionarySpinnerAdapter(this, DictfileManager.getList(this));
		getSupportActionBar().setListNavigationCallbacks(listNavAdapter, listNavListener);
	}

    void updateTitleBar() {
		if (dict != null) {
			int pos = listNavAdapter.getPositionForTitle(dict.title);
			getSupportActionBar().setSelectedNavigationItem(pos);
		}

		invalidateOptionsMenu();
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
		final List<Dict> langs = Dict.searchForLanguage(SearchActivity.this, dict.toLang, dict.fromLang);
		menu.findItem(R.id.swaplanguage).setEnabled(dict != null && dict.title != null && langs.size()>0);
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
        case R.id.swaplanguage:
        	swapLanguageDirection();
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
    			dict = new Dict(this, data.getStringExtra(Intent.EXTRA_TEXT));
    	        
    			SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(SearchActivity.this).edit();
    			ed.putString("dictFile", dict.filePrefix);
    			ed.putString("dictTitle", dict.title);
    			ed.commit();
				updateDictList();
    			updateTitleBar();
    			readMRU();
    			displayMRU();
    		}
    		break;
    	}
    }

    public class DictionarySearcher extends AsyncTask<String, Long, DictionarySearcher.ResultSet> {
        
    	protected class ResultSet { long count,anfang,ende; boolean success; String error; String[][] data; }
    	
    	ListView targetList;
    	boolean isLiveSearch = true;
    	
		@Override
		protected ResultSet doInBackground(String... params) {
			dictSearchInAction = true;
			ResultSet result = new ResultSet();
			result.anfang = android.os.Process.getElapsedCpuTime();
			String dictFilePrefix = params[0], searchTerm =Dict.deAccent(params[1].toLowerCase());
			
			ArrayList<String[]> lines = new ArrayList<String[]>();
			
			//String termLength = String.valueOf(searchTerm.length());
			try {
				char ch0 = searchTerm.charAt(0);
				char ch1 = searchTerm.length() > 1 ? searchTerm.charAt(1) : '$';
				FileInputStream fis;
				if (Character.isLetter(ch0)) ch0 = Character.toLowerCase(ch0); else ch0 = '$';
				if (Character.isLetter(ch1)) ch1 = Character.toLowerCase(ch1); else ch1 = '$';
				
				fis = Dict.openForRead(SearchActivity.this, dictFilePrefix+"_"+ch0+ch1);
				if (fis == null) {  // File not found!
					throw new FileNotFoundException("Either no results were found or the import was interrupted!");
				}
				
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
					if (result.count >= maxResults) break;
				}
				
				read.close();
				result.success = true;
				result.data = lines.toArray(new String[][]{});
				
			} catch (FileNotFoundException e) {
				result.success = true;
				result.data = new String[][]{};
				
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
		        if (!isLiveSearch) Toast.makeText(SearchActivity.this, String.format(getString(R.string.result_count_toast), result.count, (float)(result.ende - result.anfang)/1000), Toast.LENGTH_SHORT).show();
		        DictListAdapter dla = new DictListAdapter(SearchActivity.this, 0, result.data);
		        dla.listHeaderId = R.layout.listitem_groupheader;
		        dla.listContentId = R.layout.listitem_translation;
		        targetList.setAdapter(dla);
			} else {
				MessageBox.alert(SearchActivity.this, result.error, getString(R.string.error));
			}
			dictSearchInAction = false;
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