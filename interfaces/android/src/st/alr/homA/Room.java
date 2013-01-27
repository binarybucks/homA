package st.alr.homA;

import java.util.TreeMap;

import st.alr.homA.support.Events;

import de.greenrobot.event.EventBus;

import android.content.Context;
import android.util.Log;



public class Room implements Comparable<Room>{
	private String id;
	private TreeMap<String, Device> devices;
	
	
	public Room(Context context, String id) {
		this.id = id;
		devices = new TreeMap<String, Device>();
	}

	public String getId() {
		return id;
	}

	public TreeMap<String, Device> getDevices() {
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
		devices.put(device.getId(), device);
		EventBus.getDefault().post(new Events.DeviceAddedToRoom(device, this));
		Log.v(toString(), "Device '" + device.getId() + "' added to room '" + id + "' , new count is: " + devices.size());
		
	}

	public void removeDevice(Device device) {
		devices.remove(device.getId());		
		EventBus.getDefault().post(new Events.DeviceRemovedFromRoom(device, this));
		Log.v(toString(), "Device '" + device.getId() + "'  removed from room '" + id + "', new count is: " + devices.size());
	}

	@Override
	public int compareTo(Room another) {	
		return this.toString().compareTo(another.toString());
	}
	

}
