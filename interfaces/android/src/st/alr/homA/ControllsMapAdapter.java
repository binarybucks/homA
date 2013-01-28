package st.alr.homA;

import java.util.TreeMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import st.alr.homA.support.TreeMapAdapter;
import android.widget.Switch;
import android.widget.TextView;

public class ControllsMapAdapter extends TreeMapAdapter<Control> {
	LayoutInflater inflater;
	public ControllsMapAdapter(Context context, TreeMap<String, Control> map) {
		super(context, map);		
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	  @Override
	  public View getView(int position, View convertView, ViewGroup parent) {
	    View rowView = convertView;
	    if (rowView == null) {
	        rowView = getControlView((Control)getItem(position), parent);
	        TextView textView = (TextView) rowView.findViewById(R.id.controlName_switch);
	        textView.setText(getItem(position).toString());

	    }
	    return rowView;
	  }
	  
	  
	  public View getControlView(Control control, ViewGroup parent) {
			//if (control.getType().equals("switch")) {
				return switchView(control, parent);
		//	}
//			} else if (control.getType().equals("range")) {
//				return rangeView(control);
//			} else if (control.getType().equals("text")) {
//				return textView(control);
//			} else {
//				return unknownView(control);
//			}
		}
	  
	  public View switchView(final Control control, ViewGroup parent) {
		  
		  	View view = inflater.inflate(R.layout.fragment_device_switch, parent, false);
			TextView name = (TextView) view.findViewById(R.id.controlName_switch);
			final Switch value = (Switch) view.findViewById(R.id.controlValue_switch);

			name.setText(control.getName());
			value.setChecked(control.getValue().equals("1"));

			control.setValueChangedObserver(new ValueChangedObserver() {
				@Override
				public void onValueChange(String valueStr) {
					App.getUiThreadHandler().post(new Runnable() {
						@Override
						public void run() {
							value.setChecked(control.getValue().equals("1"));
						}
					});
				}
			});

			value.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String payload = control.getValue().equals("1") ? "0" : "1";
					App.publishMqtt(control.getTopic(), payload);
				}
			});

			return view;
		}



}
