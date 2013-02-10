
package st.alr.homA.model;

import st.alr.homA.support.ValueChangedObserver;
import android.content.Context;

public class Control {
    ValueChangedObserver observer;
    Context context;
    Device device;
    String value;
    String topic;
    String id;

    public static enum TYPE {
        UNDEFINED, SWITCH, RANGE, TEXT
    };

    private TYPE type = TYPE.UNDEFINED;

    public Control(Context context, String id, String topic, Device device) {
        this.id = id;
        this.value = "0";
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

    public TYPE getType() {
        return type;
    }

    public void setType(String typeStr) {
        if (typeStr.equals("switch")) {
            this.type = TYPE.SWITCH;
        } else if (typeStr.equals("range")) {
            this.type = TYPE.RANGE;
        } else if (typeStr.equals("text")) {
            this.type = TYPE.TEXT;
        } else {
            this.type = TYPE.UNDEFINED;
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

}
