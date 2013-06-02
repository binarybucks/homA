    
package st.alr.homA;
import java.util.HashMap;
import java.util.Set;
import st.alr.homA.MqttService.MQTT_CONNECTIVITY;
import st.alr.homA.model.Device;
import st.alr.homA.model.Room;
import st.alr.homA.support.Events;
import st.alr.homA.support.Events.MqttConnectivityChanged;
import st.alr.homA.support.NfcRecordAdapter;
import st.alr.homA.support.ValueSortedMap;
import android.app.Application;
import de.greenrobot.event.EventBus;
import com.bugsnag.android.*;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class App extends Application {
    private static App instance;
    private static HashMap<String, Device> devices;
    private static ValueSortedMap<String, Room> rooms;
    
    
    private static NfcRecordAdapter nfcRecordListAdapter;
    private static boolean recording = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Bugsnag.register(this, "635a508c10fa87191e33662dd3c08512");
        instance = this;
        devices = new HashMap<String, Device>();
        rooms = new ValueSortedMap<String, Room>();
        nfcRecordListAdapter = new NfcRecordAdapter(this);

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
        


        
        if(event.getConnectivity() == MQTT_CONNECTIVITY.DISCONNECTED_WAITINGFORINTERNET 
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
    
    public static NfcRecordAdapter getRecordMapListAdapter(){
        return nfcRecordListAdapter;
    }
    
    
    public static boolean isRecording(){
        return recording;
    }
    
    public static void startRecording(){
        recording = true;        
    }

    public static void stopRecording(){
        recording = false;
    }
}

