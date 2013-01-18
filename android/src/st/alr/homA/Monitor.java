package st.alr.homA;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class Monitor {
	private static Monitor instance;
	private static int nofiticationID = 1337;
	private static NotificationCompat.Builder notificationBuilder;
	
	private static BroadcastReceiver reconnectMightBeRequiredReceiver;
	private static BroadcastReceiver mqttConnectivityChangedReceiver;
	
	private Monitor() {
		establishObserver();
		createNotification();
	}

	
	private static void establishObserver(){
		reconnectMightBeRequiredReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (App.getState() == App.MQTT_CONNECTIVITY_DISCONNECTED && App.isAnyActivityRunning()) {
					Log.v(getInstance().toString(), "Connectivity changed");
					
					new Thread(new Runnable() {
						@Override
						public void run() {
							App.bootstrapAndConnectMqtt(false, true);
						}
					}).start();

				}			

			}
		};
		IntentFilter f1 = new IntentFilter();
		f1.addAction(App.MQTT_RECONNECT_MIGHT_BE_REQUIRED);
		App.getInstance().registerReceiver(reconnectMightBeRequiredReceiver, f1);		

		mqttConnectivityChangedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateNotification();
			}
		};
		IntentFilter f2 = new IntentFilter();
		f2.addAction(App.MQTT_CONNECTIVITY_CHANGED);
		App.getInstance().registerReceiver(mqttConnectivityChangedReceiver, f2);		
	}
	
	
	
	private static void createNotification(){
		if (notificationBuilder != null) {
			updateNotification();
			return;
		}
		Log.v("Monitor", "Creating notification");
		 notificationBuilder = new NotificationCompat.Builder(App.getInstance());

		 
		 // Creates an explicit intent for an Activity that gets activated when notification is tabbed
		 Intent resultIntent = new Intent(App.getInstance(), RoomListActivity.class);
		 TaskStackBuilder stackBuilder = TaskStackBuilder.create(App.getInstance());		 
		 stackBuilder.addParentStack(RoomListActivity.class);
		 stackBuilder.addNextIntent(resultIntent);
		 PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);		 
		 notificationBuilder.setContentIntent(resultPendingIntent);
		 
		 updateNotification();
		 
	}
	
	public static void updateNotification(){
		if (notificationBuilder == null) {
			createNotification();
			return;
		}
		Log.v("Monitor", "Updating notification");
		
		 final NotificationManager mNotificationManager = (NotificationManager) App.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
		 

		 notificationBuilder.setSmallIcon(R.drawable.homamonochrome)
		 					.setContentTitle("HomA");
		 
		 notificationBuilder.setOngoing(true)
		 					.setContentText(getConnectionStateText())
		 					.setPriority(Notification.PRIORITY_MIN);
		 	
		 final Notification note = notificationBuilder.build();
					 mNotificationManager.notify(nofiticationID, note);
	}
	
	
	public static String getConnectionStateText() {
		switch (App.getState()) {
			case App.MQTT_CONNECTIVITY_CONNECTED:
				return "Connected";
				
			case App.MQTT_CONNECTIVITY_CONNECTING:
				return "Connecting";
			case App.MQTT_CONNECTIVITY_DISCONNECTING:
				return "Disconnecting";
			default:
				return "Disconnected";
		}
	}
	
	
	
	public static Monitor getInstance(){
		if (instance == null)
			instance = new Monitor();
		return instance;		
	}

}
