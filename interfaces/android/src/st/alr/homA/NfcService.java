package st.alr.homA;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;



public class NfcService extends Service 
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
            Log.v(this.toString(), "Doing some fancy publishing. Intent is: "+  intent.toString());

        } else {
            Log.v(this.toString(), "NFC Service started without intent, abandon ship");

        }
        

       stopSelf();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
