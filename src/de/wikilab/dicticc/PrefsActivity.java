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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class PrefsActivity extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(PrefsActivity.this, R.xml.preferences, false);
        
        Preference p;

        p = findPreference("debug_fileman");
        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(PrefsActivity.this, DebugActivity.class));
				return true;
			}
		});

        p = findPreference("p_support_facebook");
        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/pages/Offline-Translator/105989296161901")));
				return true;
			}
		});
        
        p = findPreference("p_support_rate");
        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=de.wikilab.dicticc")));
				return true;
			}
		});
        
        p = findPreference("p_support_homepage");
        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://dicticc.wiki-lab.net")));
				return true;
			}
		});
        
        p = findPreference("p_invert_colors");
        ((CheckBoxPreference)p).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				Toast.makeText(PrefsActivity.this, R.string.restart_after_setting_change, Toast.LENGTH_LONG).show(); 
				return true;
			}
		});
        
        p = findPreference("p_show_changelog");
        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				MessageBox.showChangeLog(PrefsActivity.this);
				return true;
			}
		});
        
        
	}
	
}
