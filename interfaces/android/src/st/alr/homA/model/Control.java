
package st.alr.homA.model;

import java.util.HashMap;

import st.alr.homA.support.ValueChangedObserver;
import android.content.Context;

public class Control implements Comparable<Control> {
    ValueChangedObserver observer;
    Context context;
    Device device;
    String value;
    String topic;
    String id;
    Integer order;

    HashMap<String, String> meta;


    public Control(Context context, String id, Device device) {
        this.id = id;
        this.value = "0";
        this.topic = "/devices/"+ device.toString() + "/controls/" + id + "/on";
        this.device = device;
        this.context = context;
        this.meta = new HashMap<String, String>();

    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
        if (observer != null) {
            observer.onValueChange(this, value);
        }
    }

    public String getMeta(String key, String def) {
        String s = meta.get(key);
        return (s != null && (!s.equals(""))) ? s : def;
    }

    public void setMeta(String key, String value) {
        if(!value.equals(""))              
            meta.put(key, value);
        else
            meta.remove(key);
        if(key.equals("order")) {
            this.device.sortControls();
        }
    }
    
    public String getTopic() {
        return topic;
    }

    // Returns a friendly name shown in the user interface. For now this is the  id
    public String getName() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    public void setValueChangedObserver(ValueChangedObserver observer) {
        this.observer = observer;
    }

    public void removeValueChangedObserver() {
        observer = null;
    }
    
    public Integer getOrder(){
        try {
            return Integer.parseInt(this.getMeta("order", "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int compareTo(Control other) {
        int result = 0;
        
        if (getOrder() > other.getOrder()) {
            result = 1;
        } else if (getOrder() < other.getOrder()) {
            result = -1;
        }
        return result;
    }
    

    
}
