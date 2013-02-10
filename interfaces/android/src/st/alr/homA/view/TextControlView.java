package st.alr.homA.view;
import st.alr.homA.R;
import android.app.Activity;
import android.widget.TextView;

public class TextControlView extends ControlView {

    public TextControlView(Activity activity) {
        super(activity, R.layout.fragment_device_text);
    }

    @Override
    public void setContent(String name, String value) {
        _name.setText(name);
        ((TextView) _value).setText(value);
    }

    @Override
    protected void setInteractionListener() {
    }
}
