package st.alr.homer;

import java.util.ArrayList;

import android.util.Log;

interface ValueChangedObserver
{
   void onValueChange(String value);
}


public class Control {
    ArrayList<ValueChangedObserver> observers = new ArrayList<ValueChangedObserver> ();

    String value = "0";
    String type = "undefined";
    String topic = null;
    String id;
    Device device;
    
    public Control(String id, String topic, Device device) {
    	this.id = id;
    	this.topic = topic;
    	this.device = device;
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
		this.device.getRoom().deviceAdapterDatasourceChanged();

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
    
    public void addValueChangedObserver(ValueChangedObserver observer){
    	this.observers.add(observer);
    }    
    public void removeValueChangedObserver(ValueChangedObserver observer){
    	this.observers.remove(observer);
    }
    
    private void valueChanged(){
    	for (ValueChangedObserver o : observers) {
			o.onValueChange(value);
		}
    }
    
    
}
