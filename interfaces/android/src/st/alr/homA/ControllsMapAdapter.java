package st.alr.homA;

import java.util.ArrayList;
import java.util.TreeMap;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import st.alr.homA.support.TreeMapAdapter;
import android.widget.Switch;
import android.widget.TextView;

public class ControllsMapAdapter extends TreeMapAdapter<Control> {
	private final LayoutInflater inflater;
	
	
	
	
	public ControllsMapAdapter(Context context, TreeMap<String, Control> map) {
		super(context, map);		
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	}

	
	
	private abstract class ViewHolder {
		public TextView _name;
		public View _value;
		
		abstract public void setContent(String name, String value);
		
		public void setContent(Control c) {
			setContent(c.getName(), c.getValue());
		}
	}
	
	private class SwitchViewHolder extends ViewHolder{
		public void setContent(String name, String value) {
			_name.setText(name);
			((Switch)_value).setChecked(value.equals("1"));
		}
	}


//    public View getView(int position, View convertView, ViewGroup parent) {
//        return rows.get(position).getView(convertView);
//    }
//
//    private void updateView(Control control, String value){
//    	int position = control.device.getControls().values().toArray().
//    	
//        int visiblePosition = yourListView.getFirstVisiblePosition();
//        View v = yourListView.getChildAt(itemIndex - visiblePosition);
//        // Do something fancy with your listitem view
//        TextView someTextView = (TextView) v.findViewById(R.id.sometextview);
//        someTextView.setText(&quot;Hi! I updated you manually!&quot;);
//    }
//	
	
	
	
	  @Override
	  public View getView(int position, View convertView, ViewGroup parent) {

		  	Log.v(this.toString(), "getView");
			ViewHolder holder;

			Control control = (Control) getItem(position);
			if (convertView == null) {
				switch (control.getType()) {
					case App.APP_CONTROL_TYPE_SWITCH:
						holder = new SwitchViewHolder();						
						convertView = inflater.inflate(R.layout.fragment_device_switch, null);
						holder._value = convertView.findViewById(R.id.controlValue_switch);
						holder._name = (TextView) convertView.findViewById(R.id.controlName_switch);
						break;
					//TODO: Add other types here
					default:
						holder = new SwitchViewHolder();						
						convertView = inflater.inflate(R.layout.fragment_device_switch, null);
						holder._value = convertView.findViewById(R.id.controlValue_switch);
						holder._name = (TextView) convertView.findViewById(R.id.controlName_switch);
						
						break;
				}
				

				holder.setContent(control);
				convertView.setTag(holder);				
			} else {
				holder = (ViewHolder) convertView.getTag();
				holder.setContent(control);
			}


			return convertView;

	  }
//	  
//	  
		@Override
		public int getItemViewType(int position) {
			return ((Control)getItem(position)).getType();
		}

		@Override
		public int getViewTypeCount() {
			return 3;
		}
//		
//	  
//	  

		
//	  public View getControlView(Control control, ViewGroup parent) {
//			//if (control.getType().equals("switch")) {
//				return switchView(control, parent);
//		//	}
////			} else if (control.getType().equals("range")) {
////				return rangeView(control);
////			} else if (control.getType().equals("text")) {
////				return textView(control);
////			} else {
////				return unknownView(control);
////			}
//		}
//	  
//	  public View switchView(final Control control, ViewGroup parent) {
//		  
//		  
//		  	final View view = inflater.inflate(R.layout.fragment_device_switch, parent, false);
//			TextView name = (TextView) view.findViewById(R.id.controlName_switch);
//			final Switch value = (Switch) view.findViewById(R.id.controlValue_switch);
//
//			name.setText(control.getName());
//			value.setChecked(control.getValue().equals("1"));
//			Log.v("outside", ""+value);
//			Log.v("Items", ""+map);
//
//			control.removeValueChangedObserver();
//			control.setValueChangedObserver(new ValueChangedObserver() {
//				@Override
//				public void onValueChange(final String valueStr) {
//					App.getUiThreadHandler().post(new Runnable() {
//						@Override
//						public void run() {
//					Log.v("inseide", ""+value);
//
//							value.toggle();
//							
//							//							view.requestLayout();
//
//						}
//					});
//				}
//			});
//
//			value.setOnClickListener(new OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					String payload = control.getValue().equals("1") ? "0" : "1";
//					App.publishMqtt(control.getTopic(), payload);
//				}
//			});
//
//			return view;
//		}
//


}
