package st.alr.homA;

import java.util.List;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;

public class SettingsActivity extends PreferenceActivity {
	private static Preference serverPreference;
	private static SharedPreferences sharedPreferences;
	private BroadcastReceiver mqttConnectivityChangedReceiver;
	private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;

	@Override
	protected void onDestroy() {
		unregisterReceiver(mqttConnectivityChangedReceiver);
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferencesChangedListener);
		super.onDestroy();
	}

	@Override
	protected void onPause() {
	    App.activityPaused();
		super.onPause();
	}

	@Override
	protected void onResume() {
	    App.activityActivated();
		super.onResume();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		getFragmentManager().beginTransaction().replace(android.R.id.content, new UserPreferencesFragment()).commit();

		preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
				String stringValue = sharedPreference.getString(key, "");

				if (key.equals("serverAddress")) {
					setServerPreferenceSummary(stringValue);
				}

			}
		};

		sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangedListener);

		mqttConnectivityChangedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				setServerPreferenceSummaryManually();
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(App.MQTT_CONNECTIVITY_CHANGED);
		registerReceiver(mqttConnectivityChangedReceiver, filter);

	}

	public static class UserPreferencesFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {

			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
			serverPreference = findPreference("serverPreference");
			setServerPreferenceSummaryManually();

		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onIsMultiPane() {
		return false;
	}

	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preferences_headers, target);
	}

	private static void setServerPreferenceSummaryManually() {
		setServerPreferenceSummary(sharedPreferences.getString("serverAddress", ""));
	}

	private static void setServerPreferenceSummary(String stringValue) {
		if (stringValue == null || stringValue.equals("") ) {
			serverPreference.setSummary("No server set");
		} else {
			serverPreference.setSummary(Monitor.getConnectionStateText());			
		}
	}
}
