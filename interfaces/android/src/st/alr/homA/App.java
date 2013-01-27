package st.alr.homA;

import java.util.HashMap;
import java.util.TreeMap;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import st.alr.homA.support.Events;
import de.greenrobot.event.EventBus;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class App extends Application implements MqttCallback {
	private static App instance;
	private static MqttClient mqttClient;
	private static Handler uiThreadHandler;
	private static SharedPreferences sharedPreferences;
	private static NotificationCompat.Builder notificationBuilder;

	private static HashMap<String, Device> devices;
	private static TreeMap<String, Room> rooms;

	public static final short MQTT_CONNECTIVITY_DISCONNECTED = 0x01;
	public static final short MQTT_CONNECTIVITY_CONNECTING = 0x02;
	public static final short MQTT_CONNECTIVITY_CONNECTED = 0x03;
	public static final short MQTT_CONNECTIVITY_DISCONNECTING = 0x04;

	private static boolean isAnyActivityRunning = true;
	private static int nofiticationID = 1337;
	private static short mqttConnectivity = MQTT_CONNECTIVITY_DISCONNECTED;


	@Override
	public void onCreate() {
		super.onCreate();

		instance = this;
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		devices = new HashMap<String, Device>();
		rooms = new TreeMap<String, Room>();
		notificationBuilder = new NotificationCompat.Builder(App.getInstance());
		uiThreadHandler = new Handler();
		EventBus.getDefault().register(this);
		createNotification();		
		bootstrapAndConnectMqtt();
	}


	public static void bootstrapAndConnectMqtt() {
		bootstrapAndConnectMqtt(false, false);
	}

	public static void bootstrapAndConnectMqtt(final boolean clear, final boolean throttle) 	{
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

			mqttClient = new MqttClient("tcp://" + sharedPreferences.getString("serverAddress", getInstance().getString(R.string.defaultsServerAddress)) + ":" + sharedPreferences.getString("serverPort", getInstance().getString(R.string.defaultsServerPort)), MqttClient.generateClientId(), null);
			mqttClient.setCallback(getInstance());

			if (throttle) {
				Thread.sleep(5000);
			}

			EventBus.getDefault().post(new Events.MqttConnectivityChanged(MQTT_CONNECTIVITY_CONNECTING));

			connectMqtt();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void connectMqtt() {
		try {
			Log.v(getInstance().toString(), "Connecting to MQTT broker");
			mqttClient.connect();
			EventBus.getDefault().post(new Events.MqttConnectivityChanged(MQTT_CONNECTIVITY_CONNECTED));

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
		Log.e(toString(), "Lost connection to the MQTT server. Sending MQTT_RECONNECT_MIGHT_BE_REQUIRED broadcast");

		EventBus.getDefault().post(new Events.MqttConnectivityChanged(MQTT_CONNECTIVITY_DISCONNECTED));
		EventBus.getDefault().post(new Events.MqttReconnectMightBeRequired());
	}

	@Override
	public void deliveryComplete(MqttDeliveryToken token) {
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
			
			devices.put(device.getId(), device);
			EventBus.getDefault().post(new Events.DeviceAdded(device));

			device.moveToRoom(getString(R.string.defaultsRoomName));

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

			EventBus.getDefault().post(new Events.MqttConnectivityChanged(MQTT_CONNECTIVITY_DISCONNECTING));

			mqttClient.disconnect();
			EventBus.getDefault().post(new Events.MqttConnectivityChanged(MQTT_CONNECTIVITY_DISCONNECTED));

			Log.v(getInstance().toString(), "Disconnected");
		} else {
			Log.v(getInstance().toString(), "Not connected");

		}
	}
	public static void publishMqtt(String topicStr, String value) {

		MqttMessage message = new MqttMessage(value.getBytes());
		message.setQos(0);
		if (!mqttClient.isConnected()) {
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
	
	
	

	public static void removeAllRooms() {
		// TODO: Send notification to clean in interface
		rooms.clear();
	}

	public static void removeRoom(Room room)  {
		rooms.remove(room.getId());
		EventBus.getDefault().post(new Events.RoomRemoved(room));
	}

	public static Room getRoom(String id) {
		return rooms.get(id);
	}
	
	public static void addRoom(Room room) {
		rooms.put(room.getId(), room);
		Log.v(getInstance().toString(), "Room added: " + rooms);
		EventBus.getDefault().post(new Events.RoomAdded(room));
	}

	public static void addDevice(Device device) {
		devices.put(device.getId(), device);
		EventBus.getDefault().post(new Events.DeviceAdded(device));
	}

	public static Room getRoomAtPosition(Integer position) {
//		Log.v(getInstance().toString(), "room at position requested" + position);

		// (rooms.size() > position)
			return getRoom((String)rooms.keySet().toArray()[position]);
		//else
			//return null;
	}
	


	public static Integer getRoomCount() {
		return rooms.size();
	}

	public static Device getDevice(String id) {
		return devices.get(id);
	}



	public static String getConnectionStateText() {
		switch (App.getState()) {
			case App.MQTT_CONNECTIVITY_CONNECTED:
				return  App.getInstance().getString(R.string.connectivityConnected);
			case App.MQTT_CONNECTIVITY_CONNECTING:
				return App.getInstance().getString(R.string.connectivityConnecting);
			case App.MQTT_CONNECTIVITY_DISCONNECTING:
				return App.getInstance().getString(R.string.connectivityDisconnected);
			default:
				return App.getInstance().getString(R.string.connectivityDisconnecting);
		}
	}
	
	public void onEvent(Events.MqttConnectivityChanged event) {
		mqttConnectivity = event.getConnectivity();
		updateNotification();
	}

	public void onEvent(Events.MqttReconnectMightBeRequired event) {
		if (App.getState() == App.MQTT_CONNECTIVITY_DISCONNECTED && App.isAnyActivityRunning()) {
			App.bootstrapAndConnectMqtt(false, true);
		}
	}

	private static void createNotification() {
		Intent resultIntent = new Intent(App.getInstance(), MainActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(App.getInstance());
		stackBuilder.addParentStack(MainActivity.class);
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		notificationBuilder.setContentIntent(resultPendingIntent);
		updateNotification();
	}

	private static void updateNotification() {
		final NotificationManager mNotificationManager = (NotificationManager) App.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationBuilder.setSmallIcon(R.drawable.homamonochrome).setContentTitle("HomA");
		notificationBuilder.setOngoing(true).setContentText(getConnectionStateText()).setPriority(Notification.PRIORITY_MIN);
		final Notification note = notificationBuilder.build();
		mNotificationManager.notify(nofiticationID, note);
	}
	
	/* Some helpers */
	
	public static void activityPaused() {
		Log.v(getInstance().toString(), "Activity paused");
		isAnyActivityRunning = false;
	}

	public static void activityActivated() {
		isAnyActivityRunning = true;
		Log.v(getInstance().toString(), "Activity resumed");
		EventBus.getDefault().post(new Events.MqttReconnectMightBeRequired());
	}
	public static Handler getUiThreadHandler() {
		return uiThreadHandler;
	}

	public static short getState() {
		return mqttConnectivity;
	}

	public static boolean isConnected() {
		return (mqttClient != null) && mqttClient.isConnected();
	}

	public static boolean isAnyActivityRunning() {
		return isAnyActivityRunning;
	}

	public static App getInstance() {
		return instance;
	}

}
