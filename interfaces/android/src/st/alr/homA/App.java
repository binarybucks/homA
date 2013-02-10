
package st.alr.homA;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import st.alr.homA.MqttService.MQTT_CONNECTIVITY;
import st.alr.homA.model.Device;
import st.alr.homA.model.Room;
import st.alr.homA.support.Events;
import st.alr.homA.support.Events.MqttConnectivityChanged;
import st.alr.homA.support.Events.ServerPreferencesSubmitted;
import de.greenrobot.event.EventBus;
import android.app.Application;

public class App extends Application {
    private static App instance;
    private static HashMap<String, Device> devices;
    private static TreeMap<String, Room> rooms;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        devices = new HashMap<String, Device>();
        rooms = new TreeMap<String, Room>();
        EventBus.getDefault().register(this);
    }

    public static Room getRoom(String id) {
        return rooms.get(id);
    }

    public static Room getRoomAtPosition(Integer position) {
        return (Room) rooms.values().toArray()[position];
    }

    public static Integer getRoomCount() {
        return rooms.size();
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
        for (Room room : rooms.values()) {
            EventBus.getDefault().post(new Events.RoomRemoved(room));
        }
        rooms.clear();
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
        
        if(event.getConnectivity() == MQTT_CONNECTIVITY.DISCONNECTED_USERDISCONNECT) {
            removeAllRooms();
            devices.clear();
        }
    }
}
