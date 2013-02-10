
package st.alr.homA.view;

import st.alr.homA.R;
import st.alr.homA.model.Control;
import st.alr.homA.support.ValueChangedObserver;
import android.app.Activity;
import android.view.View;
import android.widget.TextView;

public abstract class ControlView {
    protected TextView _name;
    protected View _value;
    protected View _layout;
    protected Control _control;
    protected Activity activity;



    public ControlView(Activity activity, int layoutRessource) {
        this.activity = activity;
        _layout = activity.getLayoutInflater().inflate(layoutRessource, null);
        _value = _layout.findViewById(R.id.controlValue);
        _name = (TextView) _layout.findViewById(R.id.controlName);
        
    }

    abstract public void setContent(String name, String value);

    abstract protected void setInteractionListener();

    public void setContent(Control c) {
        setContent(c.getName(), c.getValue());
    }

    public ControlView attachToControl(Control control) {
        _control = control;
        control.setValueChangedObserver(new ValueChangedObserver() {
            @Override
            public void onValueChange(final Object sender, Object value) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setContent((Control) sender);

                    };
                });

            }
        });
        setInteractionListener();
        setContent(control);
        return this; // for easy chaining
    }
    
    public View getLayout() {
        return _layout;
    }
}
