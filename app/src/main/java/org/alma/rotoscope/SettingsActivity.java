package org.alma.rotoscope;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Create the preference activity of Rotoscope program.
 *
 * @author dralagen
 */
public class SettingsActivity extends PreferenceActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);

  }
}
