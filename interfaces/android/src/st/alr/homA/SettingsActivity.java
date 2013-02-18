package st.alr.homA;

import st.alr.homA.support.Events;
import de.greenrobot.event.EventBus;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.Menu;

public class SettingsActivity extends PreferenceActivity {
	private static Preference serverPreference;

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public void onEventMainThread(Events.MqttConnectivityChanged event) {
		setServerPreferenceSummary();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Start service if it is not already started
        Intent service = new Intent(this, MqttService.class);
        startService(service);

		EventBus.getDefault().register(this);

		getFragmentManager().beginTransaction().replace(android.R.id.content, new UserPreferencesFragment()).commit();


	}

	public static class UserPreferencesFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {

			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
			serverPreference = findPreference("serverPreference");
			setServerPreferenceSummary();

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

	private static void setServerPreferenceSummary() {
	    serverPreference.setSummary(MqttService.getConnectivityText());
	}
}
