package st.alr.homer;

import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;


import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListAdapter;

public class App extends Application implements MqttCallback{
	private MqttClient mqttClient;
	private HashMap<String, Room> rooms;
	private HashMap<String, Device> devices;
	private Handler uiThreadHandler;

//	ControlsHashMapAdapter devicesAdapter;
	RoomsHashMapAdapter roomsAdapter;

	
    @Override
    public void onCreate() {
    	this.rooms = new HashMap<String, Room>();
    	this.devices = new HashMap<String, Device>();
    	roomsAdapter = new RoomsHashMapAdapter(this, rooms);

    	uiThreadHandler = new Handler();

    	Log.v(this.toString(), "Creating application wide instances");
    	super.onCreate();
    	
		new Thread(new Runnable() {
			@Override
			public void run() {
				connectMqtt();
			}
		}).start();
    }


	 
	private void connectMqtt() {
      try {
    	Log.v(this.toString(), "Starting Mqtt threat. Brace for impact");
		mqttClient = new MqttClient("tcp://192.168.8.2:1883", MqttClient.generateClientId(), null);
		mqttClient.setCallback(this);
		mqttClient.connect();
		
		mqttClient.subscribe("/devices/+/controls/+/type", 0);
		mqttClient.subscribe("/devices/+/controls/+", 0);
		mqttClient.subscribe("/devices/+/meta/#", 0);

	    
      } catch (MqttException e) {
    	//Log.v(this.toString(), "Exception: " + e);
    	Log.v(this.toString(), "Reason is: " + e.getMessage());
    	final String message = e.getMessage();
    	Log.e(this.toString(), message);
     	
      } catch (Exception e) {
    	  e.printStackTrace();
      }
	}
	
	@Override
	public void connectionLost(Throwable cause) {
    	Log.v(this.toString(), "Mqtt connectin lost. Cause: " + cause);
	}


	@Override
	public void deliveryComplete(MqttDeliveryToken token) {
    	Log.v(this.toString(), "Mqtt QOS delivery complete. Token: " + token);
	}


//	public ControlsHashMapAdapter getDevicesAdapter() {
//		return devicesAdapter;
//	}



	@Override
	public void messageArrived(MqttTopic topic, MqttMessage message) throws MqttException {
    
    	String payloadStr = new String(message.getPayload());
    	String topicStr = topic.getName();
    	
		final String text = topic.getName()+":"+ new String(message.getPayload()) + "\n";
    	Log.v(this.toString(), "Received: " + text);

    	String[] splitTopic = topicStr.split("/");
    	
        // Ensure the device for the message exists
    	String deviceId = splitTopic[2];
    	Device device = devices.get(deviceId);
        if (device == null) {
          device = new Device(deviceId, this);
          addDevice(device);
          device.moveToRoom(this.getString(R.string.defaultRoomName));

        }
        
        // Topic parsing
        if(splitTopic[3].equals("controls")) {
        	String controlName = splitTopic[4]; 
        	Control control = device.getControlWithId(controlName);

        	if (control == null) {
        		control = new Control(controlName, topicStr.replace("/type", ""), device);
        		device.addControl(control);

        	}

        	
          if(splitTopic.length < 6) {                                       // Control value
        	control.setValue(payloadStr);
          } else {                                                          // Control type 
          	control.setType(payloadStr);
          	Log.v(this.toString(), "type set to: " + payloadStr);
          } 
        } else if(splitTopic[3].equals("meta") ) { 
          if (splitTopic[4].equals("room")) {                                    // Device Room
            device.moveToRoom(payloadStr);

          } else if(splitTopic[4].equals("name")) {                              // Device name
        	  device.setName(payloadStr);
          }
        }
        


    }


	public void addDevice (Device device) {
		devices.put(device.getId(), device);
		Log.v(this.toString(), "Device '" + device.getId() +"' added, new count is: " + devices.size());
	}
	public void removeDevice (Device device) {
		devices.remove(device.getId());
		Log.v(this.toString(), "Device '" + device.getId() +"'  removed, new count is: " + devices.size());

	}
	public void addRoom (Room room) {
		rooms.put(room.getId(), room);
		roomAdapterDatasourceChanged();
		Log.v(this.toString(), "Room '" + room.getId() +"'  added, new count is: " + rooms.size());

	}
	public void removeRoom (Room room) {
		rooms.remove(room.getId());		
		roomAdapterDatasourceChanged();
		Log.v(this.toString(), "Room '" + room.getId() +"'  removed, new count is: " + rooms.size());
	}

	

	public void roomAdapterDatasourceChanged(){
	     uiThreadHandler.post(new Runnable() {
             @Override
             public void run() {
                 roomsAdapter.notifyDataSetChanged();
             }
           });
	}

	public HashMap<String, Room> getRooms() {
	return rooms;
}


	public HashMap<String, Device> getDevices() {
		return devices;
	}



	public RoomsHashMapAdapter getRoomsAdapter() {
		return roomsAdapter;
	}



	public Handler getUiThreadHandler() {
		return uiThreadHandler;
	}

	
}
