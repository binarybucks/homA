
package st.alr.homA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import st.alr.homA.model.Device;
import st.alr.homA.model.Room;
import st.alr.homA.model.Quickpublish;

import st.alr.homA.services.ActivityBackgroundPublish;
import st.alr.homA.services.ServiceMqtt;
import st.alr.homA.services.ServiceMqtt.MQTT_CONNECTIVITY;
import st.alr.homA.support.Events;
import st.alr.homA.support.Events.MqttConnectivityChanged;
import st.alr.homA.support.Defaults;
import st.alr.homA.support.NfcRecordAdapter;
import st.alr.homA.support.ValueSortedMap;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import de.greenrobot.event.EventBus;
import com.bugsnag.android.*;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class App extends Application {
    private static App instance;
    private static HashMap<String, Device> devices;
    private static ValueSortedMap<String, Room> rooms;
    private static NfcRecordAdapter nfcRecordListAdapter;
    private static boolean recording = false;
    private static NotificationCompat.Builder notificationBuilder;
    private static SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;

    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Bugsnag.register(this, Defaults.BUGSNAG_API_KEY);
        Bugsnag.setNotifyReleaseStages("production", "testing");
        instance = this;
        devices = new HashMap<String, Device>();
        rooms = new ValueSortedMap<String, Room>();
        nfcRecordListAdapter = new NfcRecordAdapter(this);
        notificationManager = (NotificationManager) App.getInstance().getSystemService(
                Context.NOTIFICATION_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
                if (key.equals(Defaults.SETTINGS_KEY_NOTIFICATION_ENABLED) || key.equals(Defaults.SETTINGS_KEY_QUICKPUBLISH_NOTIFICATION))
                    handleNotification();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangedListener);
        handleNotification();
        EventBus.getDefault().register(this);
    }

    public static Room getRoom(String id) {
        synchronized (rooms) {

            return rooms.get(id);
        }
    }

    public static Room getRoom(int index) {
        synchronized (rooms) {
            return rooms.get(index);
        }
    }

    public static Integer getRoomCount() {
        return rooms.size();
    }

    public static Set<String> getRoomIds() {
        return rooms.keySet();
    }

    public static void addRoom(Room room) {
        rooms.put(room.getId(), room);
        EventBus.getDefault().post(new Events.RoomAdded(room));
    }

    public static void removeRoom(Room room) {
        rooms.remove(room.getId());
        EventBus.getDefault().post(new Events.RoomRemoved(room));

    }

    public static void removeAllRooms() {
        rooms.clear();
        EventBus.getDefault().post(new Events.RoomsCleared());
    }

    public static Device getDevice(String id) {
        return devices.get(id);
    }

    public static void addDevice(Device device) {
        devices.put(device.toString(), device);
        EventBus.getDefault().post(new Events.DeviceAdded(device));
    }

    public static App getInstance() {
        return instance;
    }

    public void onEvent(MqttConnectivityChanged event) {

        if (event.getConnectivity() == MQTT_CONNECTIVITY.DISCONNECTED_WAITINGFORINTERNET
                || event.getConnectivity() == MQTT_CONNECTIVITY.DISCONNECTED_USERDISCONNECT
                || event.getConnectivity() == MQTT_CONNECTIVITY.DISCONNECTED_DATADISABLED
                || event.getConnectivity() == MQTT_CONNECTIVITY.DISCONNECTED) {
            removeAllRooms();
            devices.clear();

        }
    }

    public static void addToNfcRecordMap(String topic, MqttMessage message) {
        nfcRecordListAdapter.put(topic, message);
    }

    public static void removeFromNfcRecordMap(String topic) {
        nfcRecordListAdapter.remove(topic);
    }

    public static HashMap<String, MqttMessage> getNfcRecordMap() {
        return nfcRecordListAdapter.getMap();
    }

    public static NfcRecordAdapter getRecordMapListAdapter() {
        return nfcRecordListAdapter;
    }

    public static boolean isRecording() {
        return recording;
    }

    public static void startRecording() {
        recording = true;
    }

    public static void stopRecording() {
        recording = false;
    }

    /**
     * @category NOTIFICATION HANDLING
     */
    private void handleNotification() {
        Log.v(this.toString(), "handleNotification()");
        notificationManager.cancel(Defaults.NOTIFCATION_ID);

        if (sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_NOTIFICATION_ENABLED, Defaults.VALUE_NOTIFICATION_ENABLED))
            createNotification();
    }

    private void createNotification() {
        notificationBuilder = new NotificationCompat.Builder(App.getInstance());

        Intent resultIntent = new Intent(App.getInstance(), ActivityMain.class);
        android.support.v4.app.TaskStackBuilder stackBuilder = android.support.v4.app.TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ActivityMain.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultPendingIntent);
        setNotificationQuickpublishes();
        updateNotification();
    }

    public void updateNotification() {
        notificationBuilder.setContentTitle(getResources().getString(R.string.appName));
        notificationBuilder
                .setSmallIcon(R.drawable.homamonochrome)
                .setOngoing(true)
                .setContentText(ServiceMqtt.getConnectivityText())
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setWhen(0);
        notificationManager.notify(Defaults.NOTIFCATION_ID, notificationBuilder.build());
    }
    
    private void setNotificationQuickpublishes(){
        ArrayList<Quickpublish> qps = Quickpublish.fromPreferences(this, Defaults.SETTINGS_KEY_QUICKPUBLISH_NOTIFICATION);
        for (int i = 0; i < qps.size() && i < Defaults.NOTIFICATION_MAX_ACTIONS; i++) {
            Log.v(this.toString(),                     qps.get(i).getName());
            notificationBuilder.addAction(
                    R.drawable.ic_quickpublish,
                    qps.get(i).getName(),
                    PendingIntent.getActivity(this, 0, new Intent(this, ActivityBackgroundPublish.class), 0));

        }
    }
}
