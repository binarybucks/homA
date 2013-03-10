package st.alr.homA.view;

import st.alr.homA.MqttService;
import st.alr.homA.R;
import android.app.Activity;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class RangeControlView extends ControlView {
    public RangeControlView(Activity activity) {
        super(activity, R.layout.fragment_device_range, R.id.seekControlValue, R.id.seekControlName);
    }

    public void setContent(String name, String value) {
        _name.setText(name);

        ((SeekBar) _value).setSaveEnabled(false);
        ((SeekBar) _value).setSaveFromParentEnabled(false);

        ((SeekBar) _value).setMax(255);
        ((SeekBar) _value).setProgress(Integer.parseInt(value));
        Log.v(this.toString(), "Setting seekbar " + name  + " to " + value);
        Log.v(this.toString(), "Setting seekbar " + name  + " to " + value);

    }

    @Override
    protected void setInteractionListener() {

        ((SeekBar) _value).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (MqttService.getInstance() != null) {
                        String payload = Integer.toString(progress);
                        MqttService.getInstance().publish(_control.getTopic(), payload);
                    }
                }                
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }
        });
    }
}

