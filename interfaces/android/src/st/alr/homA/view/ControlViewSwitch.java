
package st.alr.homA.view;

import st.alr.homA.R;
import st.alr.homA.services.ServiceMqtt;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Switch;

public class ControlViewSwitch extends ControlView {
    public ControlViewSwitch(Activity activity) {
        super(activity, R.layout.fragment_device_switch, R.id.switchControlValue, R.id.switchControlName);
    }

    public void setContent(String name, String value) {
        _name.setText(name);
        ((Switch) _value).setChecked(value.equals("1"));
    }

    @Override
    protected void setInteractionListener() {

        ((Switch) _value).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String payload = _control.getValue().equals("1") ? "0" : "1";
                if (ServiceMqtt.getInstance() != null) {
                    ServiceMqtt.getInstance().publish(_control.getTopic(), payload);
                }
            }
        });
    }
}
