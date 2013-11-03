
package st.alr.homA;

import st.alr.homA.services.ServiceMqtt;
import st.alr.homA.support.Defaults;
import st.alr.homA.support.Events;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import de.greenrobot.event.EventBus;

public class ActivityPreferences extends PreferenceActivity {
    private static Preference serverPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start service if it is not already started
        Intent service = new Intent(this, ServiceMqtt.class);
        startService(service);

        // Register for connection changed events
        EventBus.getDefault().register(this);

        // Replace content with fragment for custom preferences
        getFragmentManager().beginTransaction().replace(android.R.id.content, new CustomPreferencesFragment()).commit();

    }

   public static String getAndroidId() {
        
        String id = App.getAndroidId();

        // MQTT specification doesn't allow client IDs longer than 23 chars
        if (id.length() > 22)
            id = id.substring(0, 22);
        
        return id;
    }
    
    public static int getBrokerAuthType(){
        return PreferenceManager.getDefaultSharedPreferences(App.getContext()).getInt(Defaults.SETTINGS_KEY_BROKER_AUTH, Defaults.VALUE_BROKER_AUTH_ANONYMOUS);

    }

    public static String getBrokerUsername()
    {
        return PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString(st.alr.homA.support.Defaults.SETTINGS_KEY_BROKER_USERNAME, "");
    }
    
    
    public static String getDeviceName()
    {
        return getAndroidId();        
    }
    
    public static class CustomPreferencesFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
           
            PackageManager pm = this.getActivity().getPackageManager();
            Preference version = findPreference("versionReadOnly");

            try {
                version.setSummary(pm.getPackageInfo(this.getActivity().getPackageName(), 0).versionName);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }

            serverPreference = findPreference("serverPreference");
            setServerPreferenceSummary();
            
            if (NfcAdapter.getDefaultAdapter(getActivity()) == null
                    || !NfcAdapter.getDefaultAdapter(getActivity()).isEnabled()) {
                PreferenceScreen nfcPreferenceItem = (PreferenceScreen) findPreference("preferencesNFC");
                nfcPreferenceItem.setEnabled(false);
             }


        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onEventMainThread(Events.StateChanged.ServiceMqtt event) {
        setServerPreferenceSummary();
    }

    private static void setServerPreferenceSummary() {
        serverPreference.setSummary(ServiceMqtt.getStateAsString());
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

}
