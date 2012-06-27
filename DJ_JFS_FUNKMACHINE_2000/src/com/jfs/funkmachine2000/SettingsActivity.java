package com.jfs.funkmachine2000;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Settings page Loads the settings from preferences.xml. Sets setCacheColorHint
 * to 0 to prevent a black background while scrolling.
 * 
 * @author Jan
 * 
 */
public class SettingsActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		getListView().setCacheColorHint(0);
	}

}
