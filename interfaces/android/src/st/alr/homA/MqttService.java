package st.alr.homA;

import java.lang.ref.WeakReference;
import java.util.Calendar;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import st.alr.homA.model.Control;
import st.alr.homA.model.Device;
import st.alr.homA.support.Events;
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
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import de.greenrobot.event.EventBus; 


public class MqttService extends Service implements MqttCallback
{

    public static enum MQTT_CONNECTIVITY {
        INITIAL, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED_WAITINGFORINTERNET, DISCONNECTED_USERDISCONNECT, DISCONNECTED_DATADISABLED, DISCONNECTED
    }
    
    public static final String MQTT_PING_ACTION = "st.alr.homA.MqttService.PING";
    private static final int NOTIFCATION_ID = 1337;

    private static MQTT_CONNECTIVITY mqttConnectivity;
    private short keepAliveSeconds;
    private String mqttClientId;
    private MqttClient mqttClient;
    private NetworkConnectionIntentReceiver netConnReceiver;
    private PingSender pingSender;
    private static SharedPreferences sharedPreferences;
    private static NotificationCompat.Builder notificationBuilder;
    private static MqttService instance;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
    private NotificationManager notificationManager;
    private boolean notificationEnabled;
    private LocalBinder<MqttService> mBinder;
    private Thread workerThread;
    
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
        mBinder = new LocalBinder<MqttService>(this);
        notificationManager = (NotificationManager) App.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(App.getInstance());
        keepAliveSeconds = 15 * 60;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
          @Override
          public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
              if (key.equals("runInBackgroundPreference"))
                  handleNotification();
          }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangedListener);

        EventBus.getDefault().register(this);
        handleNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
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
            String brokerAddress = sharedPreferences.getString("serverAddress",
                    getString(R.string.defaultsServerAddress));
            String brokerPort = sharedPreferences.getString("serverPort",
                    getString(R.string.defaultsServerPort));

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
            registerReceiver(pingSender, new IntentFilter(MQTT_PING_ACTION));
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
                device.moveToRoom(getString(R.string.defaultsRoomName));

            }
            
            // Topic parsing
            //  /devices/$uniqueDeviceId/controls/$deviceUniqueControlId/meta/type
            // 0/      1/              2/       3/                     4/   5/   6
            if (splitTopic[3].equals("controls")) {
                String controlName = splitTopic[4];
                Control control = device.getControlWithId(controlName);

                if (control == null) {
                    control = new Control(this, controlName, topicStr.replace("/type", ""), device);
                    device.addControl(control);
                }
                if (splitTopic.length == 5) { // Control value
                    control.setValue(payloadStr);
                } else if(splitTopic.length == 7){ // Control meta
                    control.setMeta(splitTopic[6], payloadStr);
                }
            } else if (splitTopic[3].equals("meta")) {
                device.setMeta(splitTopic[4], splitTopic[4]); // Device Meta
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
                MQTT_PING_ACTION), PendingIntent.FLAG_UPDATE_CURRENT);

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
            updateNotification();
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
        switch (MqttService.getConnectivity()) {
            case CONNECTED:
                return App.getInstance().getString(R.string.connectivityConnected);
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
     * @category NOTIFICATION HANDLING
     */
    private void handleNotification(){
        if(notificationEnabled = sharedPreferences.getBoolean("runInBackgroundPreference", true)) {        
            createNotification();
        } else {
            notificationManager.cancel(NOTIFCATION_ID);
        }
    }
    
    private void createNotification() {
        Intent resultIntent = new Intent(App.getInstance(), MainActivity.class);
        android.support.v4.app.TaskStackBuilder stackBuilder = TaskStackBuilder.create(App
                .getInstance());
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultPendingIntent);
        updateNotification();
    }

    private void updateNotification() {
        notificationBuilder.setSmallIcon(R.drawable.homamonochrome).setContentTitle("HomA");
        notificationBuilder.setOngoing(true).setContentText(getConnectivityText())
                .setPriority(Notification.PRIORITY_MIN).setWhen(0);
        final Notification note = notificationBuilder.build();
        notificationManager.notify(NOTIFCATION_ID, note);
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
    public static MqttService getInstance() {
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
        private WeakReference<MqttService> mService;
        public LocalBinder(MqttService service) {
            mService = new WeakReference<MqttService>(service);
        }

        public MqttService getService() {
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
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferencesChangedListener);

        super.onDestroy();
    }

    @Override
    public void deliveryComplete(MqttDeliveryToken arg0) { }
}


