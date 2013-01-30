package st.alr.homA;

import java.util.TreeMap;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import st.alr.homA.support.TreeMapAdapter;
import android.widget.TextView;

public class DeviceMapAdapter extends TreeMapAdapter<Device> {

	public DeviceMapAdapter(Context context, TreeMap<String, Device> map) {
		super(context, map);		
	}
	
	  static class ViewHolder {
		    public TextView title;
		  }
	  
	  
	  @Override
	  public View getView(int position, View convertView, ViewGroup parent) {
//	    View rowView = convertView;
//	    if (rowView == null) {
//	    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//	        rowView = inflater.inflate(R.layout.row_layout, parent, false);
//	        TextView textView = (TextView) rowView.findViewById(R.id.title);
//	        Device d =(Device)getItem(position);
//	        textView.setText(d.getName());
//
//	    }
//	    return rowView;
		  
		  Log.v("DeviceMapAdapter", "Map:  " + map);
		   View rowView = convertView;
		    if (rowView == null) {
		      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		      rowView = inflater.inflate(R.layout.row_layout, null);
		      ViewHolder viewHolder = new ViewHolder();
		      viewHolder.title = (TextView) rowView.findViewById(R.id.title);
		      rowView.setTag(viewHolder);
		    } // TODO: Fix holder pattern here

		    ViewHolder holder = (ViewHolder) rowView.getTag();
		    holder.title.setText(((Device)getItem(position)).getName());

		    return rowView;
	  
	  }

	  
	  
	  

}
