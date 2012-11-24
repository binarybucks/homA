//package st.alr.homer;
//
//import java.util.HashMap;
//
//import android.content.Context;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseExpandableListAdapter;
//import android.widget.ExpandableListView;
//import android.widget.SeekBar;
//import android.widget.Switch;
//import android.widget.TextView;
//
//public class DevicesHashMapAdapter extends BaseExpandableListAdapter{
//	private HashMap<String, Device> map;
//	private Context context;
//	private LayoutInflater inflater;
//
//
//	public DevicesHashMapAdapter(Context context, HashMap<String, Device> map) {
//		this.map = map;
//		this.context = context;
//		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//	}
//	
//	@Override
//	public Object getChild(int groupPosition, int childPosition) {
//		return ((Device)getGroup(groupPosition)).getControls().values().toArray()[childPosition];
//	}
//
//	@Override
//	public long getChildId(int groupPosition, int childPosition) {
//		return 0;
//	}
//
//	@Override
//	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
//		View view;
//		Control control = (Control)getChild(groupPosition, childPosition);
//		
//		if (convertView == null) {
//			if (control.getType().equals("text")) {
//				view = inflater.inflate(R.layout.room_detail_child_item_text, parent, false);				
//			} else if (control.getType().equals("switch")) {
//				view = inflater.inflate(R.layout.room_detail_child_item_switch, parent, false);				
//				
//			} else if (control.getType().equals("range")) {
//				view = inflater.inflate(R.layout.room_detail_child_item_range, parent, false);				
//				
//			} else {
//				view = inflater.inflate(R.layout.room_detail_child_item_unknown, parent, false);				
//	//			android.R.layout.simple_list_item_1
//			}
//		
//		} else {
//			
//			view = convertView;
//		}
//
//		try {
//			TextView tv = (TextView)view.findViewById(R.id.childItemTextView);
//			Log.v(this.toString(), "type is" + control.getType());
//
//			
//			
//			tv.setText(control.getId());
//			if (control.getType().equals("text")) {
//				TextView value = (TextView)view.findViewById(R.id.childItemValue);
//				value.setText(control.getValue());
//			} else if (control.getType().equals("switch")) {
//				Log.v(this.toString(), "here");
//				Switch sw = (Switch)view.findViewById(R.id.childItemSwitch);
//				sw.setChecked(control.getValue().equals("1"));
//				
//			} else if (control.getType().equals("range")) {
//				SeekBar sb = (SeekBar)view.findViewById(R.id.childItemRange);
//				sb.setMax(255);
//				sb.setProgress( Math.round(Float.parseFloat(control.getValue())));
//			}
//			
//			
//		} catch (ClassCastException e) {
//			e.printStackTrace();
//		}
////		@android:drawable/divider_horizontal_dark
////		view.setBackground(android.drm);
//		return view;		
//		
//	}
//
//	@Override
//	public int getChildrenCount(int groupPosition) {
//		return ((Device)getGroup(groupPosition)).getControls().size();
//	}
//
//	@Override
//	public Object getGroup(int groupPosition) {
//		return map.values().toArray()[groupPosition];
//	}
//
//	@Override
//	public int getGroupCount() {
//		return map.size();
//	}
//
//	@Override
//	public long getGroupId(int groupPosition) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public View getGroupView(int groupPosition, boolean isLastChild, View convertView, ViewGroup parent) {
//		View view;
//
//		if (convertView == null) {
//			view = inflater.inflate(R.layout.room_detail_device_heading, parent, false);
//		} else {
//			view = convertView;
//		}
//
//		try {
//			TextView tv = (TextView)view.findViewById(R.id.device_heading_textview);
//			tv.setText(getGroup(groupPosition).toString());
//		} catch (ClassCastException e) {
//			e.printStackTrace();
//		}
//	    ExpandableListView eLV = (ExpandableListView) parent;
//	    eLV.expandGroup(groupPosition);
//
//
//
//		return view;	
//	}
//
//	@Override
//	public boolean hasStableIds() {
//		return false;
//	}
//
//	@Override
//	public boolean isChildSelectable(int groupPosition, int childPosition) {
//		return false;
//	}
//
//}
