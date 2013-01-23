package st.alr.homA;

import java.util.HashMap;

import de.greenrobot.event.EventBus;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Room implements Comparable<Room>{
	private String id;
	private HashMap<String, Device> devices;
	private Context context;

	public Room(Context context, String id) {
		this.id = id;
		this.context = context;
		devices = new HashMap<String, Device>();
	}

	public String getId() {
		return id;
	}

	public HashMap<String, Device> getDevices() {
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
