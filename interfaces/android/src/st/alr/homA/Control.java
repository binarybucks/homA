package st.alr.homA;

import android.content.Context;

interface ValueChangedObserver {
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
	}

	public String getTopic() {
		return topic;
	}

	// Returns a friendly name shown in the user interface. For now this is the id
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

	private void valueChanged() {
		if (observer != null) {
			observer.onValueChange(value);
		}
	}

}
