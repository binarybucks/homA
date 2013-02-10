
package st.alr.homA;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
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
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import de.greenrobot.event.EventBus;

/*
 * An example of how to implement an MQTT client in Android, able to receive
 *  push notifications from an MQTT message broker server.
 *  
 *  Dale Lane (dale.lane@gmail.com)
 *    28 Jan 2011
 *    
 *  Modified into generic library and improved; Dirk Moors
 *    6 Nov 2012  
 */

public class MqttService extends Service implements MqttCallback
{

    public static enum MQTT_CONNECTIVITY
    {
        INITIAL,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED_WAITINGFORINTERNET,
        DISCONNECTED_USERDISCONNECT,
        DISCONNECTED_DATADISABLED,
        DISCONNECTED
    }

    private static MQTT_CONNECTIVITY mqttConnectivity = MQTT_CONNECTIVITY.INITIAL;
    private short keepAliveSeconds = 20 * 60;
    private static int nofiticationID = 1337;
    private String mqttClientId = null;
    private MqttClient mqttClient = null;
    private NetworkConnectionIntentReceiver netConnReceiver;
    private PingSender pingSender;
    private ExecutorService executor;
    private static SharedPreferences sharedPreferences;
    private static NotificationCompat.Builder notificationBuilder;
    private static MqttService instance;
    public static final String MQTT_PING_ACTION = "st.alr.homA.MqttService.PING";

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.v(this.toString(), "onCreate");
        instance = this;
        changeMqttConnectivity(MQTT_CONNECTIVITY.INITIAL);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mBinder = new LocalBinder<MqttService>(this);
        executor = Executors.newFixedThreadPool(2);
        notificationBuilder = new NotificationCompat.Builder(App.getInstance());
        EventBus.getDefault().register(this);

        createNotification();

    }

    @Override
    public void onStart(final Intent intent, final int startId)
    {
        Log.v(this.toString(), "onStart");

        // This is the old onStart method that will be called on the pre-2.0
        // platform. On 2.0 or later we override onStartCommand() so this
        // method will not be called.

        doStart(intent, startId);
    }

    public static MqttService getInstance() {
        return instance;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId)
    {
        Log.v(this.toString(), "onStartCommand");

        doStart(intent, startId);

        // return START_NOT_STICKY - we want this Service to be left running
        // unless explicitly stopped, and it's process is killed, we want it to
        // be restarted
        return START_STICKY;
    }

    private void doStart(final Intent intent, final int startId) {
        doStart(intent, startId, false);
    }

    private void doStart(final Intent intent, final int startId,
            final boolean reconnectAfterUserDisconnect) {
        Log.v(this.toString(), "doStart");

        initMqttClient();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                handleStart(intent, startId, reconnectAfterUserDisconnect);
            }
        });
    }

    protected void onConnect() {
        subscribeToTopics();
    }

    synchronized void handleStart(Intent intent, int startId, boolean reconnectAfterUserDisconnect)
    {
        Log.v(this.toString(), "handleStart");

        if (mqttClient == null)
        {
            // we were unable to define the MQTT client connection, so we stop
            // immediately - there is nothing that we can do
            Log.e(this.toString(), "handleStart: mqttClient == null");
            stopSelf();
            return;
        }

        if (!reconnectAfterUserDisconnect
                && (mqttConnectivity == MQTT_CONNECTIVITY.DISCONNECTED_USERDISCONNECT)) {
            return;
        }

        if (!isBackgroundDataEnabled()) // respect the user's request not to use
                                        // data!
        {
            // user has disabled background data
            Log.e(this.toString(), "handleStart: !isBackgroundDataEnabled");
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED_DATADISABLED);
            return;
        }

        // if the Service was already running and we're already connected - we
        // don't need to do anything
        if (!isConnected())
        {
            Log.v(this.toString(), "handleStart: !isConnected");

            // set the status to show we're trying to connect

            // before we attempt to connect - we check if the phone has a
            // working data connection
            if (isOnline(true))
            {
                // we think we have an Internet connection, so try to connect
                // to the message broker
                if (connectToBroker())
                {
                    onConnect();
                }
            }
            else
            {
                Log.v(this.toString(), "No data connection, abandon ship");
                // we can't do anything now because we don't have a working
                // data connection
                changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED_WAITINGFORINTERNET);
            }
        }

        /*
         * Changes to the phone's network - such as bouncing between WiFi and
         * mobile data networks - can break the MQTT connection the MQTT
         * connectionLost can be a bit slow to notice, so we use Android's
         * inbuilt notification system to be informed of network changes - so we
         * can reconnect immediately, without hang to wait for the MQTT timeout
         */
        if (netConnReceiver == null)
        {
            netConnReceiver = new NetworkConnectionIntentReceiver();
            registerReceiver(netConnReceiver, new IntentFilter(
                    ConnectivityManager.CONNECTIVITY_ACTION));
        }

        // creates the intents that are used to wake up the phone when it is
        // time to ping the server
        if (pingSender == null)
        {
            pingSender = new PingSender();
            registerReceiver(pingSender, new IntentFilter(MQTT_PING_ACTION));
        }
    }

    @Override
    public void onDestroy()
    {
        // disconnect immediately
        disconnectFromBroker();

        changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED);

        if (mBinder != null) {
            mBinder.close();
            mBinder = null;
        }

        super.onDestroy();
    }

    private LocalBinder<MqttService> mBinder;

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    public class LocalBinder<T> extends Binder
    {
        private WeakReference<MqttService> mService;

        public LocalBinder(MqttService service)
        {
            mService = new WeakReference<MqttService>(service);
        }

        public MqttService getService()
        {
            return mService.get();
        }

        public void close()
        {
            mService = null;
        }
    }

    public void disconnect()
    {
        disconnectFromBroker();

        // set status
        changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED_USERDISCONNECT);
    }

    /************************************************************************/
    /* METHODS - MQTT methods inherited from MQTT classes */
    /************************************************************************/

    /*
     * callback - method called when we no longer have a connection to the
     * message broker server
     */
    @Override
    public void connectionLost(Throwable t)
    {
        // we protect against the phone switching off while we're doing this
        // by requesting a wake lock - we request the minimum possible wake
        // lock - just enough to keep the CPU running until we've finished
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();

        if (isOnline(true) == false)
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
            if (splitTopic[3].equals("controls")) {
                String controlName = splitTopic[4];
                Control control = device.getControlWithId(controlName);

                if (control == null) {
                    control = new Control(this, controlName, topicStr.replace("/type", ""), device);
                    device.addControl(control);
                }
                if (splitTopic.length < 6) { // Control value
                    control.setValue(payloadStr);
                } else { // Control type
                    control.setType(payloadStr);
                }
            } else if (splitTopic[3].equals("meta")) {
                if (splitTopic[4].equals("room")) { // Device Room
                    device.moveToRoom(payloadStr);
                } else if (splitTopic[4].equals("name")) { // Device name
                    device.setName(payloadStr);
                }
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

    private void initMqttClient()
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

    private boolean connectToBroker()
    {

        Log.v(this.toString(), "connectToBroker");

        try
        {
            changeMqttConnectivity(MQTT_CONNECTIVITY.CONNECTING);
            mqttClient.connect();

            changeMqttConnectivity(MQTT_CONNECTIVITY.CONNECTED);
            scheduleNextPing();

            return true;
        } catch (MqttException e)
        {
            Log.e(this.toString(), e.toString());
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED);
            return false;
        }
    }

    private void subscribeToTopics()
    {
        Log.v(this.toString(), "subscribeToTopics");

        if (!isConnected())
        {
            Log.e(this.toString(), "Unable to subscribe as we are not connected");
        }
        else
        {
            try
            {
                mqttClient.subscribe("/devices/+/controls/+/type", 0);
                mqttClient.subscribe("/devices/+/controls/+", 0);
                mqttClient.subscribe("/devices/+/meta/#", 0);

            } catch (IllegalArgumentException e)
            {
                Log.e(this.toString(), "subscribe failed - illegal argument", e);
            } catch (MqttException e)
            {
                Log.e(this.toString(), "subscribe failed - MQTT exception", e);
            }
        }
    }

    private void disconnectFromBroker()
    {
        Log.v(this.toString(), "disconnectFromBroker");

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
        } catch (MqttPersistenceException e)
        {
            Log.e(this.toString(), "disconnect failed - persistence exception", e);
        } catch (MqttException e)
        {
            Log.e(this.toString(), "disconnect failed - mqtt exception", e);
        } finally
        {
            mqttClient = null;
        }
    }

    private void scheduleNextPing()
    {
        Log.v(this.toString(), "scheduleNextPing in " + keepAliveSeconds + " seconds");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                MQTT_PING_ACTION), PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar wakeUpTime = Calendar.getInstance();
        wakeUpTime.add(Calendar.SECOND, keepAliveSeconds);

        AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(), pendingIntent);
    }

    private boolean isConnected()
    {
        return ((mqttClient != null) && (mqttClient.isConnected() == true));
    }

    @SuppressWarnings("deprecation")
    private boolean isBackgroundDataEnabled() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        // Only on pre-ICS platforms, backgroundDataSettings API exists
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return cm.getBackgroundDataSetting();
        }

        // On ICS platform and higher, define BackgroundDataSetting by checking
        // if phone is online
        return isOnline(false);
    }

    private boolean isOnline(boolean shouldCheckIfOnWifi)
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null
                && (!shouldCheckIfOnWifi || (netInfo.getType() == ConnectivityManager.TYPE_WIFI))
                && netInfo.isAvailable() && netInfo.isConnected();
    }

    private String getClientId()
    {
        // generate a unique client id if we haven't done so before, otherwise
        // re-use the one we already have

        if (mqttClientId == null)
        {
            mqttClientId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

            // truncate - MQTT spec doesn't allow client ids longer than 23
            // chars
            if (mqttClientId.length() > 22) {
                mqttClientId = mqttClientId.substring(0, 22);
            }
        }

        return mqttClientId;
    }

    private void changeMqttConnectivity(MQTT_CONNECTIVITY newConnectivity) {

        EventBus.getDefault().post(new Events.MqttConnectivityChanged(newConnectivity));
        mqttConnectivity = newConnectivity;
    }

    public void publish(String topicStr, String value) {
        boolean isOnline = isOnline(false);
        boolean isConnected = isConnected();

        if (!isOnline || !isConnected) {
            return;
        }
        MqttMessage message = new MqttMessage(value.getBytes());
        message.setQos(0);

        try
        {
            mqttClient.getTopic(topicStr + "/on").publish(message);
        } catch (MqttException e)
        {
            Log.e(this.toString(), e.getMessage());
            e.printStackTrace();
        }
    }

    public void onEvent(Events.MqttConnectivityChanged event) {
        mqttConnectivity = event.getConnectivity();
        updateNotification();
    }

    /*
     * Called in response to a change in network connection - after losing a
     * connection to the server, this allows us to wait until we have a usable
     * data connection again
     */
    private class NetworkConnectionIntentReceiver extends BroadcastReceiver
    {

        @SuppressLint("Wakelock")
        @Override
        public void onReceive(Context ctx, Intent intent)
        {
            // Log.v("context", ctx.toString());
            // Log.v("intent", intent.toString());
            // we protect against the phone switching off while we're doing this
            // by requesting a wake lock - we request the minimum possible wake
            // lock - just enough to keep the CPU running until we've finished
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();

            if (isOnline(true) && !isConnected()
                    && getConnectivity() != MQTT_CONNECTIVITY.CONNECTING)
            {
                Log.v(this.toString(), "Reconnecting");
                doStart(null, -1);
            }

            // we're finished - if the phone is switched off, it's okay for the
            // CPU to sleep now
            wl.release();
        }
    }

    private void ping() throws MqttException {

        MqttTopic topic = mqttClient.getTopic("$SYS/KEEPALIVEPING");

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

    public class PingSender extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // Note that we don't need a wake lock for this method (even though
            // it's important that the phone doesn't switch off while we're
            // doing this).
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            // long as the alarm receiver's onReceive() method is executing.
            // This guarantees that the phone will not sleep until you have
            // finished handling the broadcast."
            // This is good enough for our needs.

            if (isOnline(true) && !isConnected())
            {
                Log.v(this.toString(), "onReceive: isOnline()=" + isOnline(true)
                        + ", isConnected()="
                        + isConnected());
                doStart(null, -1);
            }
            else if (!isOnline(true)) {
                Log.d(this.toString(), "Waiting for network to come online again");
            }
            else
            {
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

            // start the next keep alive period
            scheduleNextPing();
        }
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
            // More verbose disconnect states could be added here. For now any flavour of disconnected is treated the same
            default:
                return App.getInstance().getString(R.string.connectivityDisconnected);
        }
    }

    @Override
    public void deliveryComplete(MqttDeliveryToken token) {
    }

    private static void createNotification() {
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

    private static void updateNotification() {
        final NotificationManager mNotificationManager = (NotificationManager) App.getInstance()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder.setSmallIcon(R.drawable.homamonochrome).setContentTitle("HomA");
        notificationBuilder.setOngoing(true).setContentText(getConnectivityText())
                .setPriority(Notification.PRIORITY_MIN);
        final Notification note = notificationBuilder.build();
        mNotificationManager.notify(nofiticationID, note);
    }

    public void reconnect() {
        disconnect();
        doStart(null, -1, true);
    }
}
