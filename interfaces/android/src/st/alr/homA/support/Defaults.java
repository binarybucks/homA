package st.alr.homA.support;

import st.alr.homA.R;
import android.net.Uri;

public class Defaults {
    public static final String BUGSNAG_API_KEY = "635a508c10fa87191e33662dd3c08512";
    public static final String MQTT_PING_ACTION = "st.alr.homA.ServiceMqtt.PING";
    public static final int NOTIFCATION_ID = 1337;
    public static final int GET_IMAGE_ID = 1338;

    public static final int NOTIFICATION_MAX_ACTIONS = 3; // Android just allows for 3 custom actions on a notification

    public static final String SETTINGS_KEY_BROKER_HOST = "serverAddress";
    public static final String SETTINGS_KEY_BROKER_PORT = "serverPort";
    public static final String SETTINGS_KEY_NOTIFICATION_ENABLED = "runInBackgroundPreference";
    public static final String SETTINGS_KEY_QUICKPUBLISH_NOTIFICATION = "quickpublishNotification";

    
    
    public static final String VALUE_BROKER_HOST = "192.168.8.2";
    public static final String VALUE_BROKER_PORT = "1883";
    public static final String VALUE_ROOM_NAME = "unassigned";
    public static final boolean VALUE_NOTIFICATION_ENABLED = true;
    public static final String VALUE_QUICKPUBLISH_JSON = "[]";
    public static final String VALUE_QUICKPUBLISH_NAME = "Quickpublish";
    public static final Uri VALUE_QUICKPUBLISH_ICON = Uri.parse("android.resource://st.alr.homA/drawable/ic_quickpublish");
    public static final Object INTENT_ACTION_PUBLISH_LASTKNOWN = "st.alr.mqttitude.PUBLISH_LASTKNOWN";


}
