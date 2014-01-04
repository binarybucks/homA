package st.alr.homA.services;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;

/* Android does not allow a service to receive NFC intents. 
 * This activity receives them and sends them to the proper service*/
public class ActivityNfc extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent mqttSrv = new Intent(this, ServiceMqtt.class);        
        startService(mqttSrv);

        if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Intent srv = new Intent(this, ServiceBackgroundPublish.class);        
            srv.putExtras(getIntent().getExtras());
            srv.setAction(getIntent().getAction());
            startService(srv);
        } else {
            Log.e(this.toString(), "Fail");
        }
        finish();
    }
}
