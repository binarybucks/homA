package st.alr.homA;

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Room {
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
		return id;
	}

	public void addDevice(Device device) {
		devices.put(device.getId(), device);
		Intent i = new Intent(App.DEVICE_ADDED_TO_ROOM);
		i.putExtra("roomID", id);
		i.putExtra("deviceID", device.getId());
		context.sendBroadcast(i);
		Log.v(toString(), "Device '" + device.getId() + "' added to room '" + id + "' , new count is: " + devices.size());
	}

	public void removeDevice(Device device) {
		devices.remove(device.getId());
		Intent i = new Intent(App.DEVICE_REMOVED_FROM_ROOM).putExtra("roomID", id).putExtra("deviceID", device.getId());
		context.sendBroadcast(i);
		Log.v(toString(), "Device '" + device.getId() + "'  removed from room '" + id + "', new count is: " + devices.size());
	}

}
