package st.alr.homA.services;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.json.JSONException;
import org.json.JSONObject;

import st.alr.homA.App;
import st.alr.homA.ActivityMain;
import st.alr.homA.R;
import st.alr.homA.R.drawable;
import st.alr.homA.R.string;
import st.alr.homA.model.Control;
import st.alr.homA.model.Device;
import st.alr.homA.model.Quickpublish;
import st.alr.homA.support.Defaults;
import st.alr.homA.support.Events;
import st.alr.homA.support.Events.MqttConnectivityChanged;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import de.greenrobot.event.EventBus;


public class ServiceMqtt extends Service implements MqttCallback
{

    public static enum MQTT_CONNECTIVITY {
        INITIAL, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED_WAITINGFORINTERNET, DISCONNECTED_USERDISCONNECT, DISCONNECTED_DATADISABLED, DISCONNECTED
    }
    

    private static MQTT_CONNECTIVITY mqttConnectivity = MQTT_CONNECTIVITY.DISCONNECTED;
    private short keepAliveSeconds;
    private String mqttClientId;
    private MqttClient mqttClient;
    private NetworkConnectionIntentReceiver netConnReceiver;
    private PingSender pingSender;
    private static SharedPreferences sharedPreferences;

    private static ServiceMqtt instance;
    private boolean notificationEnabled;
    private LocalBinder<ServiceMqtt> mBinder;
    private Thread workerThread;
    private Runnable deferredPublish;
    
    /**
     * @category SERVICE HANDLING
     */
    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
        workerThread = null;
        changeMqttConnectivity(MQTT_CONNECTIVITY.INITIAL);
        mBinder = new LocalBinder<ServiceMqtt>(this);
        keepAliveSeconds = 15 * 60;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(intent != null && intent.getAction() != null && intent.getAction().equals("st.alr.homA.action.QUICKPUBLISH")){
            Log.v(this.toString(), "Notification Quickpublish in Service");

            String json = intent.getStringExtra("qp");
            try {
                Quickpublish qp = new Quickpublish(new JSONObject(json));
                ServiceMqtt.getInstance().publishWithTimeout(qp.getTopic(), qp.getPayload(), qp.isRetained(), 30);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        doStart(intent, startId);
        return START_STICKY;
    }

    private void doStart(final Intent intent, final int startId) {
        init();
        

        Thread thread1 = new Thread(){
            @Override
            public void run() {
                handleStart(intent, startId);
                if (this == workerThread) // Clean up worker thread
                    workerThread = null;
            }
            
            @Override
            public void interrupt() {
                if (this == workerThread) // Clean up worker thread
                    workerThread = null;
                super.interrupt();
            }
        };
        thread1.start();
    }



     void handleStart(Intent intent, int startId) {
        Log.v(this.toString(), "handleStart");

        
        // If there is no mqttClient, something went horribly wrong
        if (mqttClient == null) {
            Log.e(this.toString(), "handleStart: !mqttClient");
            stopSelf();
            return;
        }        
        
        // Respect user's wish to stay disconnected
        if ((mqttConnectivity == MQTT_CONNECTIVITY.DISCONNECTED_USERDISCONNECT) && startId != -1) {
            return;
        }

        // No need to connect if we're already connecting
        if (isConnecting()) {
            return;
        }
        
        // Respect user's wish to not use data
        if (!isBackgroundDataEnabled()) {
            Log.e(this.toString(), "handleStart: !isBackgroundDataEnabled");
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED_DATADISABLED);
            return;
        }

        // Don't do anything when already connected
        if (!isConnected())
        {
            Log.v(this.toString(), "handleStart: !isConnected");
            // Check if there is a data connection
            if (isOnline(true))
            {
                if (connect())
                {
                    Log.v(this.toString(), "handleStart: connectToBroker() == true");
                    onConnect();
                }
            }
            else
            {
                Log.e(this.toString(), "handleStart: !isOnline");
                changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED_WAITINGFORINTERNET);
            }
        }
    }


    /**
     * @category CONNECTION HANDLING
     */
    private void init()
    {
        Log.v(this.toString(), "initMqttClient");
        if (mqttClient != null) {
            return;
        }

        try
        {
            String brokerAddress = sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_HOST, Defaults.VALUE_BROKER_HOST);
            String brokerPort = sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_PORT, Defaults.VALUE_BROKER_PORT);

            mqttClient = new MqttClient("tcp://" + brokerAddress + ":" + brokerPort, getClientId(),
                    null);
            mqttClient.setCallback(this);
        } catch (MqttException e)
        {
            // something went wrong!
            mqttClient = null;
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED);
        }


    }

    private boolean connect()
    {
        workerThread = Thread.currentThread(); // We connect, so we're the worker thread
        Log.v(this.toString(), "connectToBroker");

        try
        {
            changeMqttConnectivity(MQTT_CONNECTIVITY.CONNECTING);
            MqttConnectOptions options = new MqttConnectOptions();
            
            options.setKeepAliveInterval(keepAliveSeconds); 
            options.setConnectionTimeout(10);
            
            mqttClient.connect(options);

            changeMqttConnectivity(MQTT_CONNECTIVITY.CONNECTED);
            scheduleNextPing();

            return true;
        } catch (Exception e) // Paho tends to throw NPEs in some cases. 
        {
            Log.e(this.toString(), e.toString());
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED);
            return false;
        }

    }


   
    private void onConnect() {
        
        // Establish observer to monitor wifi and radio connectivity 
        if (netConnReceiver == null) {
            netConnReceiver = new NetworkConnectionIntentReceiver();
            registerReceiver(netConnReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }

        // Establish ping sender
        if (pingSender == null) {
            pingSender = new PingSender();
            registerReceiver(pingSender, new IntentFilter(Defaults.MQTT_PING_ACTION));
        }
        
        // Subscribe to topics
        if (!isConnected()) {
            Log.e(this.toString(), "onConnect: !isConnected");
        } else {
            try
            {
                mqttClient.subscribe("/devices/+/meta/#", 0);
                mqttClient.subscribe("/devices/+/controls/+/meta/#", 0);
                mqttClient.subscribe("/devices/+/controls/+", 0);

            } catch (IllegalArgumentException e)
            {
                Log.e(this.toString(), "subscribe failed - illegal argument", e);
            } catch (MqttException e)
            {
                Log.e(this.toString(), "subscribe failed - MQTT exception", e);
            }        }    
    }


    public void disconnect(boolean fromUser)
    {
        Log.v(this.toString(), "disconnect");
        if(fromUser)
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED_USERDISCONNECT);            
        

        try
        {
            if (netConnReceiver != null)
            {
                unregisterReceiver(netConnReceiver);
                netConnReceiver = null;
            }

            if (pingSender != null)
            {
                unregisterReceiver(pingSender);
                pingSender = null;
            }
        } catch (Exception eee)
        {
            Log.e(this.toString(), "Unregister failed", eee);
        }

        try
        {
            if (mqttClient != null && mqttClient.isConnected())
            {
                mqttClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mqttClient = null;

            if(workerThread != null) {
                workerThread.interrupt();
            }

        }
    }

    
    @SuppressLint("Wakelock") // Lint check derps with the wl.release() call. 
    @Override
    public void connectionLost(Throwable t)
    {
        // we protect against the phone switching off while we're doing this
        // by requesting a wake lock - we request the minimum possible wake
        // lock - just enough to keep the CPU running until we've finished
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();

        if (!isOnline(true))
        {
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED_WAITINGFORINTERNET);
        }
        else
        {
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED);
            scheduleNextPing(); // Try again in one ping intervall
        }
        wl.release();
    }
    

    public void reconnect() {
        disconnect(true);
        doStart(null, -1);
    }
    @Override
    public void messageArrived(MqttTopic topic, MqttMessage message) throws MqttException {
        // we protect against the phone switching off while we're doing this
        // by requesting a wake lock - we request the minimum possible wake
        // lock - just enough to keep the CPU running until we've finished
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();

        //
        // I'm assuming that all messages I receive are being sent as strings
        // this is not an MQTT thing - just me making as assumption about what
        // data I will be receiving - your app doesn't have to send/receive
        // strings - anything that can be sent as bytes is valid
        // String messageBody = new String(payloadbytes);

        // inform the app (for times when the Activity UI is running) of the
        // received message so the app UI can be updated with the new data
        try
        {
            Log.v(this.toString(), "messageArrived: topic=" + topic.getName() + ", message="
                    + new String(message.getPayload()));

            String payloadStr = new String(message.getPayload());
            String topicStr = topic.getName();

            final String text = topic.getName() + ":" + new String(message.getPayload()) + "\n";
            Log.v(toString(), "Received: " + text);

            String[] splitTopic = topicStr.split("/");

            // Ensure the device for the message exists
            String deviceId = splitTopic[2];
            Device device = App.getDevice(deviceId);
            if (device == null) {
                device = new Device(deviceId, this);

                App.addDevice(device);
                device.moveToRoom(Defaults.VALUE_ROOM_NAME);

            }
            
            // Topic parsing
            //  /devices/$uniqueDeviceId/controls/$deviceUniqueControlId/meta/type
            // 0/      1/              2/       3/                     4/   5/   6
            if (splitTopic[3].equals("controls")) {
                String controlName = splitTopic[4];
                Control control = device.getControlWithId(controlName);

                if (control == null) {
                    control = new Control(this, controlName, device);
                    device.addControl(control);
                }
                if (splitTopic.length == 5) { // Control value
                    control.setValue(payloadStr);
                } else if(splitTopic.length == 7){ // Control meta
                    control.setMeta(splitTopic[6], payloadStr);
                }
            } else if (splitTopic[3].equals("meta")) {
                device.setMeta(splitTopic[4], payloadStr); // Device Meta
            }

        } catch (MqttException e)
        {
            e.printStackTrace();
        }

        // receiving this message will have kept the connection alive for us, so
        // we take advantage of this to postpone the next scheduled ping
        scheduleNextPing();

        // we're finished - if the phone is switched off, it's okay for the CPU
        // to sleep now
        wl.release();

    }    
    private void scheduleNextPing()
    {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                Defaults.MQTT_PING_ACTION), PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar wakeUpTime = Calendar.getInstance();
        wakeUpTime.add(Calendar.SECOND, keepAliveSeconds);

        AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(), pendingIntent);
    }


    public void publish(String topicStr, String payload) {
        publish(topicStr, payload, true);
    }
    
    public void publish(String topicStr, String payload, boolean retained) {
        boolean isOnline = isOnline(false);
        boolean isConnected = isConnected();

        if (!isOnline || !isConnected) {
            return;
        }
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(0);
        message.setRetained(retained);
        
        try
        {
            mqttClient.getTopic(topicStr).publish(message);
            if(App.isRecording()) {
                App.addToNfcRecordMap(topicStr,message);
            }
            
        } catch (MqttException e)
        {
            Log.e(this.toString(), e.getMessage());
            e.printStackTrace();
        }
    }

    public void onEvent(Events.MqttConnectivityChanged event) {
        mqttConnectivity = event.getConnectivity();
        if(notificationEnabled)
           App.getInstance().updateNotification();
        
        if (deferredPublish != null && event.getConnectivity() == MQTT_CONNECTIVITY.CONNECTED)
            deferredPublish.run();
    }
    
    
  

    /**
     * @category CONNECTIVITY STATUS
     */
    private void changeMqttConnectivity(MQTT_CONNECTIVITY newConnectivity) {

        EventBus.getDefault().post(new Events.MqttConnectivityChanged(newConnectivity));
        mqttConnectivity = newConnectivity;
    }
    
    private boolean isOnline(boolean shouldCheckIfOnWifi)
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return     netInfo != null 
                && (!shouldCheckIfOnWifi || (netInfo.getType() == ConnectivityManager.TYPE_WIFI))
                && netInfo.isAvailable() 
                && netInfo.isConnected();
    }
    
    public boolean isConnected()
    {
        return ((mqttClient != null) && (mqttClient.isConnected() == true));
    }
    
    public boolean isConnecting() {
        return (mqttClient != null) && mqttConnectivity == MQTT_CONNECTIVITY.CONNECTING;
    }
    
    private boolean isBackgroundDataEnabled() {
        return isOnline(false);
    }
    
    public static MQTT_CONNECTIVITY getConnectivity() {
        return mqttConnectivity;
    }
    
    public static String getConnectivityText() {
        switch (ServiceMqtt.getConnectivity()) {
            case CONNECTED:
                return App.getInstance().getString(R.string.connectivityConnected) + " to " + sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_HOST, Defaults.VALUE_BROKER_HOST);
            case CONNECTING:
                return App.getInstance().getString(R.string.connectivityConnecting);
            case DISCONNECTING:
                return App.getInstance().getString(R.string.connectivityDisconnecting);
            // More verbose disconnect states could be added here. For now any flavor of disconnected is treated the same
            default:
                return App.getInstance().getString(R.string.connectivityDisconnected);
        }
    }


    
    /**
     * @category OBSERVERS
     */
    private class NetworkConnectionIntentReceiver extends BroadcastReceiver
    {

        @SuppressLint("Wakelock")
        @Override
        public void onReceive(Context ctx, Intent intent)
        {
            Log.v(this.toString(), "NetworkConnectionIntentReceiver: onReceive");
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();

            if (isOnline(true) && !isConnected() && !isConnecting()) {
                Log.v(this.toString(), "NetworkConnectionIntentReceiver: triggerting doStart(null, -1)");
                doStart(null, 1);
            
            }
            wl.release();
        }
    }

    public class PingSender extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            if (isOnline(true) && !isConnected() && !isConnecting()) {
                Log.v(this.toString(), "ping: isOnline()=" + isOnline(true)  + ", isConnected()=" + isConnected());
                doStart(null, -1);
            } else if (!isOnline(true)) {
                Log.d(this.toString(), "ping: Waiting for network to come online again");
            } else {            
                try
                {
                    ping();
                } catch (MqttException e)
                {
                    // if something goes wrong, it should result in
                    // connectionLost
                    // being called, so we will handle it there
                    Log.e(this.toString(), "ping failed - MQTT exception", e);

                    // assume the client connection is broken - trash it
                    try {
                        mqttClient.disconnect();
                    } catch (MqttPersistenceException e1) {
                        Log.e(this.toString(), "disconnect failed - persistence exception", e1);
                    } catch (MqttException e2)
                    {
                        Log.e(this.toString(), "disconnect failed - mqtt exception", e2);
                    }

                    // reconnect
                    Log.w(this.toString(), "onReceive: MqttException=" + e);
                    doStart(null, -1);
                }
            }
            scheduleNextPing();
        }
    }
        
    private void ping() throws MqttException {

        MqttTopic topic = mqttClient.getTopic("$SYS/keepalive");

        MqttMessage message = new MqttMessage();
        message.setRetained(false);
        message.setQos(1);
        message.setPayload(new byte[] {
            0
        });

        try
        {
            topic.publish(message);
        } catch (org.eclipse.paho.client.mqttv3.MqttPersistenceException e)
        {
            e.printStackTrace();
        } catch (org.eclipse.paho.client.mqttv3.MqttException e)
        {
            throw new MqttException(e);
        }
    }

    /**
     * @category MISC
     */
    public static ServiceMqtt getInstance() {
        return instance;
    }
    
    private String getClientId()
    {
        if (mqttClientId == null)
        {
            mqttClientId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

            // MQTT specification doesn't allow client IDs longer than 23 chars
            if (mqttClientId.length() > 22)
                mqttClientId = mqttClientId.substring(0, 22);
        }

        return mqttClientId;
    }
    
    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    public class LocalBinder<T> extends Binder
    {
        private WeakReference<ServiceMqtt> mService;
        public LocalBinder(ServiceMqtt service) {
            mService = new WeakReference<ServiceMqtt>(service);
        }

        public ServiceMqtt getService() {
            return mService.get();
        }

        public void close() {
            mService = null;
        }
    }    
    

    
    @Override
    public void onDestroy()
    {
        // disconnect immediately
        disconnect(false);

        changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED);

        if (mBinder != null) {
            mBinder.close();
            mBinder = null;
        }

        super.onDestroy();
    }

    @Override
    public void deliveryComplete(MqttDeliveryToken arg0) { }


    public void publishWithTimeout(final String topic, final String payload, final boolean retained, int timeout) {
        if (getConnectivity() == MQTT_CONNECTIVITY.CONNECTED) {
            publish(topic, payload, retained);
        } else {
            Log.d(this.toString(), "No broker connection established yet, deferring publish");
            deferredPublish = new Runnable() {
                @Override
                public void run() {
                    deferredPublish = null;
                    Log.d(this.toString(), "Broker connection established, publishing deferred message");
                    publish(topic, payload, retained);
                }
                
            };
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(this.toString(),  "Publish timed out");
                    deferredPublish = null;
                }
            }, timeout * 1000);        
        }
    }
}


