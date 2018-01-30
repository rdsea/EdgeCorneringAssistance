package com.k.matthias.corneringassistanceapplication;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by matthias on 28.01.18.
 */

public class SettingsActivity extends Activity {

    public static final String KEY_PREF_CALL_SERVER = "call_server";
    public static final String KEY_PREF_CLEAR_CACHE = "clear_cache";
    public static final String KEY_PREF_AUTO_MOVE_MAP = "auto_move_map";
    public static final String KEY_PREF_HIDE_PASSED_CURVES = "hide_passed_curves";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.pref_general);
        }
    }
}

