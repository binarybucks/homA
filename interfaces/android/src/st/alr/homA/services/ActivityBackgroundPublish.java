
package st.alr.homA.services;

import org.json.JSONException;
import org.json.JSONObject;

import st.alr.homA.model.Quickpublish;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;

public class ActivityBackgroundPublish extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
            Log.v(this.toString(), "NFC Quickpublish triggered");
            Intent mqttSrv = new Intent(this, ServiceMqtt.class);        
            startService(mqttSrv);

            Intent srv = new Intent(this, ServiceNfc.class);        
            srv.putExtras(getIntent().getExtras());
            srv.setAction(getIntent().getAction());
            startService(srv);

        } else if(getIntent().getAction().equals("st.alr.homA.action.QUICKPUBLISH")){
            Log.v(this.toString(), "Notification Quickpublish triggered");
            
            Intent mqttSrv = new Intent(this, ServiceMqtt.class);
            mqttSrv.putExtras(getIntent().getExtras());
            mqttSrv.setAction(getIntent().getAction());
            startService(mqttSrv);


        }
        finish();
    }

}
