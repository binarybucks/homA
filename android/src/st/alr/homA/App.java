package st.alr.homA;

import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class App extends Application implements MqttCallback {
	private static App instance;

	public static final String MQTT_CONNECTIVITY_CHANGED = "st.alr.homA.mqttConnectivityChanged";
	public static final short MQTT_CONNECTIVITY_DISCONNECTED = 0x01;
	public static final short MQTT_CONNECTIVITY_CONNECTING = 0x02;
	public static final short MQTT_CONNECTIVITY_CONNECTED = 0x03;
	public static final short MQTT_CONNECTIVITY_DISCONNECTING = 0x04;
	public static final String DEVICE_ATTRIBUTE_TYPE_CHANGED = "st.alr.homA.deviceAttributeTypeChanged";
	public static final String DEVICE_ADDED_TO_ROOM = "st.alr.homA.deviceAddedToRoom";
	public static final String DEVICE_REMOVED_FROM_ROOM = "st.alr.homA.deviceRemovedFromRoom";
	public static final String SERVER_SETTINGS_CHANGED = "st.alr.homA.serverSettingsChanged";
	public static final String MQTT_RECONNECT_MIGHT_BE_REQUIRED = "st.alr.homA.reconnectMightBeRequired";

	
	private static MqttClient mqttClient;
	private static HashMap<String, Device> devices;
	private static Handler uiThreadHandler;
	private static short mqttConnectivity;
	private static BroadcastReceiver mqttReconnectReceiver;
	private static RoomsHashMapAdapter roomsAdapter;
	private static SharedPreferences sharedPreferences;
	private static boolean isAnyActivityRunning = true;
	private static Monitor monitor;
	
	// When activities pause or get destroyed, they call this method, to prevent mqtt auto reconnects during inactivity
	public static void activityPaused() {
		Log.v(getInstance().toString(), "Activity paused");
		isAnyActivityRunning = false;
	}
	
	// When activities resume or start mqtt connectivity is checked and auto reconnects are enabled
	public static void activityActivated() {
		isAnyActivityRunning = true;
		// this just a state changed notification with the same state.
		// The observer then checks if there is a connection and if not, triggers a reconnect loop until a connection is established or isInForeground is false
		Log.v(getInstance().toString(), "Activity resumed");
		Intent i = new Intent(App.MQTT_RECONNECT_MIGHT_BE_REQUIRED); // Recalling bootstrapAndConnect has to be decoupled from the connectionLost in order to prevent out of stack exceptions
		getInstance().sendBroadcast(i);
	}
	
	
	
	
	
	public void onCreate() {
		super.onCreate();

		instance = this;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		monitor = Monitor.getInstance();
		devices = new HashMap<String, Device>();
		roomsAdapter = new RoomsHashMapAdapter(this);
		uiThreadHandler = new Handler();
		bootstrapAndConnectMqtt();
	}

	public static App getInstance() {
		return instance;
	}

	
	public static void bootstrapAndConnectMqtt() {
		bootstrapAndConnectMqtt(false, false);
	}

	
	public static void bootstrapAndConnectMqtt(final boolean clear, final boolean throttle) {
		try {
			// Ensures that this method is not called on the main thread
			if (Thread.currentThread().getName().equals("main")) { 
				
				new Thread(new Runnable() {
					@Override
					public void run() {
						bootstrapAndConnectMqtt(clear, throttle);
					}
				}).start();
				return; 	
			}

			disconnect();
	
			if (clear) {
				Log.v(getInstance().toString(), "Clearing all rooms");
				removeAllRooms();
				Log.v(getInstance().toString(), "Clearing all devices");
				devices.clear();
			}

			mqttClient = new MqttClient("tcp://" + sharedPreferences.getString("serverAddress", "") + ":" + sharedPreferences.getString("serverPort", "1883"), MqttClient.generateClientId(), null);
			mqttClient.setCallback(getInstance());
			
			

				
			
			if (throttle) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
			}
		
			updateMqttConnectivity(App.MQTT_CONNECTIVITY_CONNECTING);

			connectMqtt();

			

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void connectMqtt() {
		try {
			Log.v(getInstance().toString(), "Connecting to MQTT broker");
			mqttClient.connect();
			updateMqttConnectivity(App.MQTT_CONNECTIVITY_CONNECTED);			
			mqttClient.subscribe("/devices/+/controls/+/type", 0);
			mqttClient.subscribe("/devices/+/controls/+", 0);
			mqttClient.subscribe("/devices/+/meta/#", 0);

		} catch (MqttException e) {
			Log.e(getInstance().toString(), "MqttException: " + e.getMessage() + ". Reason code: " + e.getReasonCode());

			if (e.getReasonCode() == MqttException.REASON_CODE_SERVER_CONNECT_ERROR) {
				getInstance().connectionLost(e.getCause()); 			
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void connectionLost(Throwable cause) {
		Log.e(toString(), "Lost conection to the mqtt server. Sending MQTT_RECONNECT_MIGHT_BE_REQUIRED broadcast");
		updateMqttConnectivity(App.MQTT_CONNECTIVITY_DISCONNECTED);
		Intent i = new Intent(App.MQTT_RECONNECT_MIGHT_BE_REQUIRED); // Recalling bootstrapAndConnect has to be decoupled from the connectionLost in order to prevent out of stack exceptions
		getInstance().sendBroadcast(i);
	}

	@Override
	public void deliveryComplete(MqttDeliveryToken token) {
		// Log.v(toString(), "Mqtt QOS delivery complete. Token: " + token);
	}

	@Override
	public void messageArrived(MqttTopic topic, MqttMessage message) throws MqttException {

		String payloadStr = new String(message.getPayload());
		String topicStr = topic.getName();

		final String text = topic.getName() + ":" + new String(message.getPayload()) + "\n";
		Log.v(toString(), "Received: " + text);

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
		if (splitTopic[3].equals("controls")) {
			String controlName = splitTopic[4];
			Control control = device.getControlWithId(controlName);

			if (control == null) {
				control = new Control(this, controlName, topicStr.replace("/type", ""), device);
				device.addControl(control);
			}
			if (splitTopic.length < 6) { // Control value
				control.setValue(payloadStr);
			} else { // Control type
				control.setType(payloadStr);
			}
		} else if (splitTopic[3].equals("meta")) {
			if (splitTopic[4].equals("room")) { // Device Room
				device.moveToRoom(payloadStr);
			} else if (splitTopic[4].equals("name")) { // Device name
				device.setName(payloadStr);
			}
		}

	}

	public static void disconnect() throws MqttException {
		if ((mqttClient != null) && mqttClient.isConnected()) {
			Log.v(getInstance().toString(), "Disconnecting");

			updateMqttConnectivity(App.MQTT_CONNECTIVITY_DISCONNECTING);
			mqttClient.disconnect();
			updateMqttConnectivity(App.MQTT_CONNECTIVITY_DISCONNECTED);
			Log.v(getInstance().toString(), "Disconnected");
		} else {
			Log.v(getInstance().toString(), "Not connected");

		}
	}

	public static void addDevice(Device device) {
		devices.put(device.getId(), device);
	}

	public static void removeDevice(Device device) {
		devices.remove(device.getId());
	}

	public static void addRoom(Room room) {
		roomsAdapter.addOnMainThread(room);
	}

	public static void removeAllRooms() {
		roomsAdapter.clearOnMainThread();
	}

	public static void removeRoom(Room room) {
		roomsAdapter.removeOnMainThread(room);
	}

	public static Room getRoom(String id) {
		return (Room) roomsAdapter.getRoom(id);
	}

	public static Device getDevice(String id) {
		return devices.get(id);
	}

	public static RoomsHashMapAdapter getRoomsAdapter() {
		return roomsAdapter;
	}

	public static Handler getUiThreadHandler() {
		return uiThreadHandler;
	}

	public static void publishMqtt(String topicStr, String value) {

		MqttMessage message = new MqttMessage(value.getBytes());
		message.setQos(0);
		if(!mqttClient.isConnected()) {
			Log.e(getInstance().toString(), "Unable to publish while not connected to a server");
			return;
		}
		
		try {
			mqttClient.getTopic(topicStr + "/on").publish(message);
		} catch (MqttPersistenceException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	private static void updateMqttConnectivity(short state) {
		mqttConnectivity = state;
		Intent i = new Intent(App.MQTT_CONNECTIVITY_CHANGED);
		getInstance().sendBroadcast(i);
	}

	
	
	
	public static short getState() {
		return mqttConnectivity;
	}

	public static boolean isConnected() {
		return (mqttClient != null) && mqttClient.isConnected();
	}
	public static boolean isAnyActivityRunning(){
		return isAnyActivityRunning;
	}
}
