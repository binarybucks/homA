
package st.alr.homA.model;

import st.alr.homA.App;
import st.alr.homA.R;
import st.alr.homA.support.Defaults;
import st.alr.homA.support.ValueChangedObserver;
import st.alr.homA.support.ValueSortedMap;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class Device implements Comparable<Device> {
    private String id;
    private String name;
    private Room room;
    private ValueSortedMap<String, Control> controls;
    private ValueChangedObserver controlAddedObserver;
    
    public Room getRoom() {
        return room;
    }

    private Context context;



    public Device(String id, Context context) {
        this.id = id;
        this.name = null;
        controls = new ValueSortedMap<String, Control>();
        this.context = context;
    }

    public void removeFromCurrentRoom() {

        if (room != null) {
            room.removeDevice(this);
            if (room.getDeviceCount() == 0) {
                Log.v(toString(), "Room " + room.getId() + " is empty, removing it");
                App.removeRoom(room);
            }
        }

    }

    public void moveToRoom(final String roomname) {
        final Device device = this;
        Runnable r  = new Runnable() {
            
            @Override
            public void run() {
                if (room != null && room.getId().equals(roomname)) // Don't move if the device is already in the target room. Also prevents https://github.com/binarybucks/homA/issues/47
                    return;

                String cleanedName = (roomname != null) && !roomname.equals("") ? roomname : Defaults.VALUE_ROOM_NAME;

                Room newRoom = App.getRoom(cleanedName);

                if (newRoom == null) {
                    newRoom = new Room(context, cleanedName);
                    App.addRoom(newRoom);
                }

                removeFromCurrentRoom();
                newRoom.addDevice(device);

                room = newRoom;
            }
        };
        
        if(Looper.myLooper() == Looper.getMainLooper())
               r.run();
        else
            new Handler(context.getMainLooper()).post(r);
        
        

    }

    public String getName() {
        return (name != null) && !name.equals("") ? name : id;
    }

    public void setName(String name) {
        //this.room.removeDevice(this);
        this.name = name;  
        //this.room.addDevice(this);
    }

    public Control getControlWithId(String id) {
        return controls.get(id);
    }

    public void addControl(Control control) {
        controls.put(control.toString(), control);
        if (controlAddedObserver != null) {
            controlAddedObserver.onValueChange(this, control);
        }
    }

    public void setControlAddedObserver(ValueChangedObserver observer) {
        this.controlAddedObserver = observer;
    }

    public void removeControlAddedObserver() {
        controlAddedObserver = null;
    }

    public ValueSortedMap<String, Control> getControls() {
        return controls;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int compareTo(Device another) {
        return this.getName().compareToIgnoreCase(another.getName());
    }

    public void setMeta(String key, String value) {
        if (key.equals("room"))
            this.moveToRoom(value);
        else if (key.equals("name"))
            this.setName(value);
    }

    public void sortControls() {
        this.controls.sortDataset();
    }

}
