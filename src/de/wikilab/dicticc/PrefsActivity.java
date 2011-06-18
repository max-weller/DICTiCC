package de.wikilab.dicticc;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;

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
	}
	
}
