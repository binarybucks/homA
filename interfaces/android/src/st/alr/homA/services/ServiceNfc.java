
package st.alr.homA.services;

import st.alr.homA.services.ServiceMqtt.MQTT_CONNECTIVITY;
import st.alr.homA.support.Events.MqttConnectivityChanged;
import android.app.Service;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.IBinder;
import android.util.Log;
import de.greenrobot.event.EventBus;

public class ServiceNfc extends Service
{
    private boolean waitingForConnection = false;
    Runnable deferred;

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.v(this.toString(), "NFC Read service onCreate ");

    }

    @Override
    public void onStart(final Intent intent, final int startId)
    {

        Log.v(this.toString(), "NFC Read service started: ");
        EventBus.getDefault().register(this, MqttConnectivityChanged.class);

        if (intent != null) {

            String action = intent.getAction();

            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                    || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                Ndef ndefTag = Ndef.get(tag);

                NdefMessage ndefMessage = ndefTag.getCachedNdefMessage();
                byte[] message;
                if (ndefMessage != null) {
                    NdefRecord[] ndefRecords = ndefMessage.getRecords();
                    if (ndefRecords.length > 0) {
                        message = ndefRecords[0].getPayload();

                        if (message != null) {
                            handleNfcMessage(new String(message));
                        }
                    }
                }

            } else {
                Log.d(this.toString(), "Unknown intent.");
            }
        } else {
            Log.v(this.toString(), "NFC Service started without intent, abandon ship");
        }

        stopSelf();
    }

    private void handleNfcMessage(String nfcMessage) {
        String[] mqttMessages = nfcMessage.split(";");

        for (String mqttMessage : mqttMessages) {
            String[] tokens = mqttMessage.split(",");
            if (tokens.length == 3) {
                Log.v(this.toString(), "Got topic: " + tokens[0]);
                Log.v(this.toString(), "Got payload: " + tokens[1]);
                Log.v(this.toString(), "Got retained: " + tokens[2]);

                ServiceMqtt.getInstance().publishWithTimeout(tokens[0], tokens[1], tokens[2] == "t", 10);
            } else {
                Log.e(this.toString(), "Failed to parse mqttMessages");
            }
        }

    }

    public void onEvent(MqttConnectivityChanged event) {
        if (waitingForConnection && event.getConnectivity() == MQTT_CONNECTIVITY.CONNECTED) {
            if (deferred != null) {
                waitingForConnection = false;
                deferred.run();
            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
