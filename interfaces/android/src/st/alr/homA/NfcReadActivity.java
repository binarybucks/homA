
package st.alr.homA;

import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;

public class NfcReadActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(this.toString(), "NfcReadActivity onCreate");
        
        
        if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Intent srv = new Intent(this, NfcService.class);        
            srv.putExtras(getIntent().getExtras());
            startService(srv);
            
            
        } else {
            Log.e(this.toString(), "Fail");
        }

        finish();

    }


}
