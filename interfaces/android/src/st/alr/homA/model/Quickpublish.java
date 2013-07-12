package st.alr.homA.model;

import java.util.ArrayList;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import st.alr.homA.support.Defaults;
import android.content.Context;
import android.preference.PreferenceManager;

public class Quickpublish {
    String topic = ""; 
    String payload = "";
    boolean retained = false; 
    String imagePath = "";
    
    public Quickpublish(String topic, String payload, boolean retained) {
        this( topic, payload, null, retained);
    }
    public Quickpublish(String topic, String payload, String imagePath, boolean retained) {
        this.topic = topic;
        this.payload = payload; 
        this.imagePath = imagePath;
        this.retained = retained;
    }
    public Quickpublish(JSONObject jsonObject) {
        try {
            this.topic = jsonObject.getString("t");
            this.payload = jsonObject.getString("p");
            this.retained = jsonObject.getString("r").equals("1") ? true : false;
            this.imagePath = jsonObject.has("img") ? jsonObject.getString("img") : "";
 
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    
    public String getTopic() {
        return topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }
    public String getPayload() {
        return payload;
    }
    public void setPayload(String payload) {
        this.payload = payload;
    }
    public boolean isRetained() {
        return retained;
    }
    public void setRetained(boolean retained) {
        this.retained = retained;
    }
    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    } 

    
    
    
    public String toJsonString()
    {
        JSONObject object = new JSONObject();
        try {
          object.put("t", this.topic);
          object.put("p", this.payload);
          object.put("r", this.retained == true ? "1" : "0" );
          object.put("img", this.imagePath);
        } catch (JSONException e) {
          e.printStackTrace();
        }
        return object.toString();
    }
    public String toString() {
        return toJsonString();
    }

    
    public static String toJsonString(ArrayList<Quickpublish> list) {
        StringBuffer b = new StringBuffer();
        b.append("[");
        
        if(list.size() > 0) {
            b.append(list.get(0).toJsonString());
        }
        
        for (int i=1; i<list.size(); i++) {
            b.append(",");
            b.append(list.get(i).toJsonString());
        }
        b.append("]");

        return b.toString();
    }

    public static ArrayList<Quickpublish> fromJsonString(String json) {
        ArrayList<Quickpublish> list = new ArrayList<Quickpublish>();
        
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++)
                list.add(i, new Quickpublish(a.getJSONObject(i)));
            
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return list;
    }
    
    public MqttMessage toMqttMessage(){
        MqttMessage m = new MqttMessage();
        return m;
        // TODO
    }
    
    public static ArrayList<Quickpublish> fromPreferences(Context context, String key) {
        return Quickpublish.fromJsonString(PreferenceManager.getDefaultSharedPreferences(context).getString(key, Defaults.VALUE_QUICKPUBLISH_JSON));
    }

    public static void toPreferences(Context context, String key, ArrayList<Quickpublish> values) {
       PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, Quickpublish.toJsonString(values)).apply();
    }
}
