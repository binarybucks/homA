package st.alr.homA;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import java.util.List;

import st.alr.homA.R;

public class SettingsActivity extends PreferenceActivity {
	private static Preference serverPreference;

	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new UserPreferencesFragment())
				.commit();
	}

	public class UserPreferencesFragment extends PreferenceFragment {

		public void onCreate(Bundle savedInstanceState) {

			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
			serverPreference = findPreference("serverPreference");
			
			
			bindPreferenceSummaryToValue(serverPreference);
			setServerPreferenceSummaryManually();

			BroadcastReceiver mqttConnectivityChangedReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (intent.getAction().equals("st.alr.homA.mqttConnectivityChanged")) {
						setServerPreferenceSummaryManually();
					}
				}
			};
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

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private Preference.OnPreferenceChangeListener onPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference.getKey().equals("serverAddress")) {
				setServerPreferenceSummary(stringValue);
			} else {
				Log.v(this.toString(),"OnPreferenceChangeListener not implemented for key "+ preference.getKey());
			}

			return true;
		}
	};

	private void bindPreferenceSummaryToValue(Preference preference) {
		preference.setOnPreferenceChangeListener(onPreferenceChangeListener);
		onPreferenceChangeListener.onPreferenceChange(preference,PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(),""));
	}

	private void setServerPreferenceSummaryManually() {
		setServerPreferenceSummary(PreferenceManager.getDefaultSharedPreferences(serverPreference.getContext()).getString("serverAdress", ""));
	}

	private void setServerPreferenceSummary(String stringValue) {

		Log.v(this.toString(), "setServerPreferenceSummary");

		if (((App) getApplicationContext()).isConnected()) {
			serverPreference.setSummary("Connected to " + stringValue);
		} else {
			serverPreference.setSummary("Not connected to " + stringValue);
		}
	}
}
