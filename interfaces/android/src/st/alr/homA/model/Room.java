
package st.alr.homA.model;

import st.alr.homA.App;
import st.alr.homA.support.DeviceAdapter;
import st.alr.homA.support.Events;
import st.alr.homA.support.ValueSortedMap;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import de.greenrobot.event.EventBus;

public class Room implements Comparable<Room> {
    private String id;
    private DeviceAdapter devices;
    private Handler uiThreadHandler;

    public Room(Context context, String id) {
        this.id = id;
        uiThreadHandler = new Handler(context.getMainLooper());

        devices = new DeviceAdapter(context);
        devices.setMap(new ValueSortedMap<String, Device>());
    }

    public String getId() {
        return id;
    }

    public Device getDevice(String id) {
        return (Device) devices.getItem(id);
    }
    public Device getDevice(int position) {
        return (Device) devices.getItem(position);
    }

    @Override
    public String toString() {
        return getId();
    }

    public void addDevice(final Device device) {
        final Room room = this;
        Runnable r  = new Runnable() {
            
            @Override
            public void run() {
                Log.v(this.toString(), "Adding " + device.getName() + " to " + room.getId());
                devices.addItem(device);
            }
        };
        
        if(Looper.myLooper() == Looper.getMainLooper())
               r.run();
        else
            uiThreadHandler.post(r);
    }

    public void removeDevice(final Device device) {
        
        Runnable r  = new Runnable() {
            
            @Override
            public void run() {
                devices.removeItem(device);
            }
        };
        
        if(Looper.myLooper() == Looper.getMainLooper())
               r.run();
        else
            uiThreadHandler.post(r);
        
    }

    @Override
    public int compareTo(Room another) {
        return this.toString().compareToIgnoreCase(another.toString());
    }
    
    public int getDeviceCount(){
        return devices.getCount();
    }
    
    public DeviceAdapter getAdapter(){
        return devices;
    }
}
