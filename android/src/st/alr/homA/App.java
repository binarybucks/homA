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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
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

	private static MqttClient mqttClient;
	private static HashMap<String, Device> devices;
	private static Handler uiThreadHandler;
	private static short mqttConnectivity;
	// private static BroadcastReceiver serverAdressChanged;
	private static RoomsHashMapAdapter roomsAdapter;
	private static SharedPreferences sharedPreferences;

	@Override
	public void onCreate() {
		super.onCreate();

		instance = this;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		devices = new HashMap<String, Device>();
		roomsAdapter = new RoomsHashMapAdapter(this);
		uiThreadHandler = new Handler();
		establishObservers();
		bootstrapAndConnectMqtt();
	}

	public static App getInstance() {
		return instance;
	}

	public static void bootstrapAndConnectMqtt() {
		bootstrapAndConnectMqtt(false);
	}

	public static void bootstrapAndConnectMqtt(final boolean clear) {
		try {
			// Ensures that this method is not called on the main thread
			if (Thread.currentThread().getName().equals("main")) { 
				new Thread(new Runnable() {
					@Override
					public void run() {
						bootstrapAndConnectMqtt(clear);
					}
				}).start();
				return;
			}

			disconnect();

			if (clear) {
				Log.v("bootstrapAndConnectMqtt", "Clearing all rooms");
				removeAllRooms();
				Log.v("bootstrapAndConnectMqtt", "Clearing all devices");
				devices.clear();
			}

			disconnect();

			updateMqttConnectivity(App.MQTT_CONNECTIVITY_CONNECTING);

			mqttClient = new MqttClient("tcp://" + sharedPreferences.getString("serverAddress", "") + ":" + sharedPreferences.getString("serverPort", "1883"), MqttClient.generateClientId(), null);

			mqttClient.setCallback(getInstance());
			connectMqtt();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void connectMqtt() {
		try {
			Log.v("Application", "Connecting to MQTT broker");
			mqttClient.connect();
			updateMqttConnectivity(App.MQTT_CONNECTIVITY_CONNECTED);

			mqttClient.subscribe("/devices/+/controls/+/type", 0);
			mqttClient.subscribe("/devices/+/controls/+", 0);
			mqttClient.subscribe("/devices/+/meta/#", 0);

		} catch (MqttException e) {
			Log.e("Application", "MqttException during connect: " + e.getMessage());

			if (e.getReasonCode() == MqttException.REASON_CODE_SERVER_CONNECT_ERROR) {
				getInstance().connectionLost(e.getCause());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// When the connection is lost after a connection has been established
	// successfully, this will retry to reconnect
	@Override
	public void connectionLost(Throwable cause) {
		Log.e(toString(), "Mqtt connection lost. Cause: " + cause);
		updateMqttConnectivity(App.MQTT_CONNECTIVITY_DISCONNECTED);

		while (!mqttClient.isConnected()) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
			bootstrapAndConnectMqtt();
		}
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
			Log.v("disconnect", "Disconnecting");

			updateMqttConnectivity(App.MQTT_CONNECTIVITY_DISCONNECTING);
			mqttClient.disconnect();
			updateMqttConnectivity(App.MQTT_CONNECTIVITY_DISCONNECTED);
			Log.v("disconnect", "Disconnected");
		} else {
			Log.v("disconnect", "Not connected");

		}
		Log.v("disconnect", "Done");

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

	// private static void showRecordingNotification(){
	// NotificationCompat.Builder mBuilder =
	// new NotificationCompat.Builder(this)
	// .setSmallIcon(R.drawable.homamonochrome)
	// .setContentTitle("HomA")
	// .setContentText("Connected to broker!");
	// // Creates an explicit intent for an Activity in your app
	// Intent resultIntent = new Intent(this, RoomListActivity.class);
	//
	// // The stack builder object will contain an artificial back stack for the
	// // started Activity.
	// // This ensures that navigating backward from the Activity leads out of
	// // your application to the Home screen.
	// TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
	// // Adds the back stack for the Intent (but not the Intent itself)
	// stackBuilder.addParentStack(RoomListActivity.class);
	// // Adds the Intent that starts the Activity to the top of the stack
	// stackBuilder.addNextIntent(resultIntent);
	// PendingIntent resultPendingIntent =
	// stackBuilder.getPendingIntent(
	// 0,
	// PendingIntent.FLAG_UPDATE_CURRENT
	// );
	// mBuilder.setContentIntent(resultPendingIntent);
	// NotificationManager mNotificationManager =
	// (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	// // mId allows you to update the notification later on.
	// mNotificationManager.notify(1, mBuilder.build());
	//
	// }

	private void establishObservers() {
		// serverAdressChanged = new BroadcastReceiver() {
		// @Override
		// public void onReceive(Context context, Intent intent) {
		// bootstrapAndConnectMqtt(true);
		//
		// }
		// };
		//
		// IntentFilter filter = new IntentFilter();
		// filter.addAction(App.SERVER_SETTINGS_CHANGED);
		// registerReceiver(serverAdressChanged, filter);
	}
}
