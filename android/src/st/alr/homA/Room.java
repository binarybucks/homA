package st.alr.homA;

import java.util.HashMap;

import com.commonsware.cwac.merge.MergeAdapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;


public class Room {
	private String id;
	private HashMap<String, Device> devices;
//	private MergeAdapter devicesAdapter;
//	private DevicesHashMapAdapter devicesAdapter;
	private Context context;

	public Room (Context context, String id) {
		this.id = id;
		this.context = context;
		devices = new HashMap<String, Device>();
//    	devicesAdapter = new DevicesHashMapAdapter(context, devices);
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
	
	public String toString(){
		return id;
	}
	
	public void addDevice (Device device) {
		devices.put(device.getId(), device);
		
//		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		view = inflater.inflate(resource, parent, false);

		//devicesAdapter.addAdapter(device.getControlsAdapter());
		
		deviceAdapterDatasourceChanged();
		Log.v(this.toString(), "Device '" + device.getId() +"' added to room '"+ this.id +"' , new count is: " + devices.size());
	}
	
	public void removeDevice (Device device) {
		devices.remove(device.getId());

		deviceAdapterDatasourceChanged();
		Log.v(this.toString(), "Device '" + device.getId() +"'  removed from room '"+ this.id +"', new count is: " + devices.size());

	}	
	
	public void deviceAdapterDatasourceChanged() {
	    ((App)context.getApplicationContext()).getUiThreadHandler().post(new Runnable() {
            @Override
            public void run() {
//                devicesAdapter.notifyDataSetChanged();
            }
          });
	}


//	public DevicesHashMapAdapter getDevicesAdapter() {
//		return devicesAdapter;
//	}
	
}
