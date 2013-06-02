package st.alr.homA.model;

import st.alr.homA.support.Events;
import st.alr.homA.support.ValueSortedMap;
import android.content.Context;
import android.util.Log;
import de.greenrobot.event.EventBus;



public class Room implements Comparable<Room>{
	private String id;
	private ValueSortedMap<String, Device> devices;
	
	public Room(Context context, String id) {
		this.id = id;
		devices = new ValueSortedMap<String, Device>();
	}



    public String getId() {
		return id;
	}

	public ValueSortedMap<String, Device> getDevices() {
		return devices;
	}

	public Device getDeviceWithId(String id) {
		return devices.get(id);
	}

	@Override
	public String toString() {
		return getId();
	}

	public void addDevice(Device device) {
		devices.put(device.toString(), device);
		EventBus.getDefault().post(new Events.DeviceAddedToRoom(device, this));
		Log.v(toString(), "Device '" + device.toString() + "' added to room '" + id + "' , new count is: " + devices.size());
		
	}

	public void removeDevice(Device device) {
		devices.remove(device.toString());		
		EventBus.getDefault().post(new Events.DeviceRemovedFromRoom(device, this));
		Log.v(toString(), "Device '" + device.toString() + "'  removed from room '" + id + "', new count is: " + devices.size());
	}

	@Override
	public int compareTo(Room another) {	
	    return this.toString().compareToIgnoreCase(another.toString());
	}

//    public void deviceSortOrderChanged() {
//        TreeMap<String, Device> n = new TreeMap<String, Device>();
//        n.putAll(devices);
//        devices = n;
//        EventBus.getDefault().post(new Events.DeviceSortOrderChanged(devices, this));
//
//    }
//	

}
