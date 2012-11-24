//package st.alr.homer;
//
//import java.util.HashMap;
//
//
//import android.content.Context;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseAdapter;
//import android.widget.BaseExpandableListAdapter;
//import android.widget.TextView;
//
//public class ControlsHashMapAdapter extends BaseAdapter {
//	private HashMap<String, Control> map;
//	private Context context;
//	private LayoutInflater inflater;
//
//
//	public ControlsHashMapAdapter(Context context, HashMap<String, Control> map) {
//		this.map = map;
//		this.context = context;
//		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//	}
//
//	@Override
//	public int getCount() {
//		return map.size();
//	}
//
//	@Override
//	public Object getItem(int arg0) {
//		return map.values().toArray()[arg0];
//	}
//
//	@Override
//	public long getItemId(int arg0) {
//		return 0;
//	}
//
//	@Override
//	public View getView(int position, View convertView, ViewGroup parent) {
//		return createViewFromResource(position, convertView, parent, R.layout.room_detail_control_item);
//	}
//
//	private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
//		View view;
//
//		if (convertView == null) {
//			view = inflater.inflate(resource, parent, false);
//			
//		} else {
//			view = convertView;
//		}
//
//		try {
//			TextView controlNameTv = (TextView)view.findViewById(R.id.ControlNameTextView);
//			
//			controlNameTv.setText(getItem(position).toString());
//
//		} catch (ClassCastException e) {
//			Log.e(toString(), "You must supply a resource ID for a TextView");
//			throw new IllegalStateException("ArrayAdapter requires the resource ID to be a TextView", e);
//		}
//
//		return convertView;
//	}
//
//	
//	
//	
//	
//	
//	
//}
