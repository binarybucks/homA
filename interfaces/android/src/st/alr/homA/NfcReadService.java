package st.alr.homA;

import android.app.Service;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;



public class NfcReadService extends Service 
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.v(this.toString(), "NFC Read service onCreate ");


    }

    @Override
    public void onStart(final Intent intent, final int startId)
    {
        Intent srv = new Intent(this, MqttService.class);        
        startService(srv);

        Log.v(this.toString(), "NFC Read service started: ");
        
        if (intent != null) {
            
            NdefMessage[] msgs = null;
            String action = intent.getAction();
            Log.v(this.toString(), "action: " + action);
            
            
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
                Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                
                
                
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                Ndef ndefTag = Ndef.get(tag);
                
                NdefMessage ndefMessage = ndefTag.getCachedNdefMessage();
                byte[] message;
                if(ndefMessage != null){
                    NdefRecord[] ndefRecords =  ndefMessage.getRecords();
                    if(ndefRecords.length > 0){
                        message =  ndefRecords[0].getPayload();
                        
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

    private void handleNfcMessage(String message) {
        String[] pairs = message.split(",");
        
        for (String pair : pairs) {
            String[] tokens = pair.split("=");
            if (tokens.length == 2) {
                Log.v(this.toString(), "Got topic: " + tokens[0]);
                Log.v(this.toString(), "Got topic: " + tokens[1]);           
                MqttService.getInstance().publish(tokens[0], tokens[1]);
            } else {
                Log.e(this.toString(), "Failed to parse message");
            }
        }
        
        

        
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
