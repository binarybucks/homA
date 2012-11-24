package st.alr.homer;

import java.util.HashMap;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

public class RoomsHashMapAdapter extends BaseAdapter {
	private HashMap<String, Room> map;
	private Context context;
	private LayoutInflater inflater;

	public RoomsHashMapAdapter(Context context, HashMap<String, Room> map) {
		this.map = map;
		this.context = context;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return map.size();
	}

	@Override
	public Object getItem(int arg0) {
		return map.values().toArray()[arg0];
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return createViewFromResource(position, convertView, parent, R.layout.room_list_item);
	}

	private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
		View view;

		if (convertView == null) {
			view = inflater.inflate(resource, parent, false);
		} else {
			view = convertView;
		}

		try {
			TextView room_name = (TextView)view.findViewById(R.id.room_name);
			TextView room_device_count = (TextView)view.findViewById(R.id.room_device_count);
			room_name.setText(getItem(position).toString());
			room_device_count.setText(   ((Room)getItem(position)).getDevices().size() + " devices");
			
		} catch (ClassCastException e) {
	    	e.printStackTrace();
		}

		return view;

	}

}
