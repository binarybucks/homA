package st.alr.homA;

import android.content.Context;
import android.util.Log;
import android.widget.Switch;

interface ValueChangedObserver {
	void onValueChange(Object sender, Object value);
}

public class Control {
	ValueChangedObserver observer;
	Context context;
	String value = "0";
	short type = App.APP_CONTROL_TYPE_UNDEFINED;
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
		if (observer != null) {
			observer.onValueChange(this, value);
		}
	}

	public short getType() {
		return type;
	}

	public void setType(String typeStr) {
		if(typeStr.equals("switch")) {
			this.type = App.APP_CONTROL_TYPE_SWITCH;
		} else if (typeStr.equals("range")) {
			this.type = App.APP_CONTROL_TYPE_RANGE;
		} else if (typeStr.equals("text")) {
			this.type = App.APP_CONTROL_TYPE_TEXT;			
		} else {
			this.type = App.APP_CONTROL_TYPE_UNDEFINED;
		}
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

	
}
