
package st.alr.homA;

import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class NfcReadActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent mqttSrv = new Intent(this, MqttService.class);        
        startService(mqttSrv);

        
        
        if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Intent srv = new Intent(this, NfcReadService.class);        
            srv.putExtras(getIntent().getExtras());
            srv.setAction(getIntent().getAction());
            startService(srv);
            
            
        } else {
            Log.e(this.toString(), "Fail");
        }

        finish();

    }


}
