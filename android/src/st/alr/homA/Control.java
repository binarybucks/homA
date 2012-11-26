package st.alr.homA;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

interface ValueChangedObserver
{
   void onValueChange(String value);
}


public class Control {
    ValueChangedObserver observer;
Context context;
    String value = "0";
    String type = "undefined";
    String topic = null;
    String id;
    Device device;
    
    public Control(Context context, String id, String topic, Device device) {
    	this.id = id;
    	this.topic = topic;
    	this.device = device;
    	this.context = context;
    }
    
  
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
		valueChanged();
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
		Intent i = new Intent("st.alr.homA.deviceTypeChanged").putExtra("deviceID", this.id);
		context.sendBroadcast(i);


	}
	public String getTopic() {
		return topic;
	}


	public String getId() {
		return id;
	}

    @Override
    public String toString() {
    	return id;
    }
    
    public void setValueChangedObserver(ValueChangedObserver observer){
    	this.observer = observer;
    }    
    public void removeValueChangedObserver(){
    	this.observer = null;
    }
    
    private void valueChanged(){
    	if (this.observer != null) {
			this.observer.onValueChange(value);
    	}
    }
    
    
}
