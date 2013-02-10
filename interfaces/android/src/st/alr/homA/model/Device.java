
package st.alr.homA.model;

import java.util.TreeMap;

import st.alr.homA.App;
import st.alr.homA.R;
import st.alr.homA.R.string;
import st.alr.homA.support.ValueChangedObserver;
import android.content.Context;
import android.util.Log;

public class Device implements Comparable<Device> {
    private String id;
    private String name;
    private Room room;
    private TreeMap<String, Control> controls;
    private ValueChangedObserver controlAddedObserver;

    public Room getRoom() {
        return room;
    }

    private Context context;

    public Device(String id, Context context) {
        this(id, null, context);
    }

    public Device(String id, String name, Context context) {
        this.id = id;
        this.name = name;
        controls = new TreeMap<String, Control>();
        this.context = context;
    }

    public void removeFromCurrentRoom() {

        if (room != null) {
            room.removeDevice(this);
            if (room.getDevices().size() == 0) {
                Log.v(toString(), "Room " + room.getId() + " is empty, removing it");
                App.removeRoom(room);
            }
        }

    }

    public void moveToRoom(String roomname) {
        if (room != null && room.getId().equals(roomname))
            return;

        String cleanedName = (roomname != null) && !roomname.equals("") ? roomname : context.getString(R.string.defaultsRoomName);

        Room newRoom = App.getRoom(cleanedName);

        if (newRoom == null) {
            newRoom = new Room(context, cleanedName);
            App.addRoom(newRoom);
        }

        removeFromCurrentRoom();
        newRoom.addDevice(this);

        room = newRoom;
    }

    public String getName() {
        return (name != null) && !name.equals("") ? name : id;
    }

    public void setName(String name) {
        this.name = name;
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

    public TreeMap<String, Control> getControls() {
        return controls;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int compareTo(Device another) {
        return this.getName().compareTo(another.getName());
    }

}
