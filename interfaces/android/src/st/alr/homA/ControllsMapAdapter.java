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
	private final ArrayList<Row>rows = new ArrayList<Row>();

	
	public ControllsMapAdapter(Context context, TreeMap<String, Control> map) {
		super(context, map);		
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	
	interface Row {
	    public View getView(View convertView);
	    public int getViewType();
	}

    public View getView(int position, View convertView, ViewGroup parent) {
        return rows.get(position).getView(convertView);
    }

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
//	  @Override
//	  public View getView(int position, View convertView, ViewGroup parent) {
//
//		  
//			ViewHolder holder;
//
//			int type = getItemViewType(position);
//			Control control = (Control) getItem(position);
//			Log.d("Adapter test","setting type:"+type);
//			if (convertView == null) {
//				Log.d("Adapter test", "new holder ");
//
//				holder = new ViewHolder();
//				if (type == 0) {
//					convertView = inflater.inflate(R.layout.fragment_device_switch, null);
//					holder._switch = (Switch) convertView.findViewById(R.id.controlValue_switch);
//					holder._name = (TextView) convertView.findViewById(R.id.controlName_switch);
//
//				}else if(type ==1){
//				}
//				else {
//				}
//				holder.type = type;
//				convertView.setTag(holder);
//			} else {
//				holder = (ViewHolder) convertView.getTag();
//				Log.d("Adapter test", " holder ::" + holder);
//			}
//            
//				if (type == 0) {
//					holder._name.setText(control.getName());
//					holder._switch.setChecked(control.getValue().equals("1")?true:false);
//
//				} else if(type ==1 ) {
//				}else{
//				}
//			return convertView;
//
//	  }
//	  
//	  
//		@Override
//		public int getItemViewType(int position) {
//			String type = ((Control)getItem(position)).getType();
//            if(type.equals("switch")) 
//            	return 0;
//			else if(type.equals("text")) 
//				return 1;
//			else 
//				return 2;
//		}
//
//		@Override
//		public int getViewTypeCount() {
//			return 3;
//		}
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
