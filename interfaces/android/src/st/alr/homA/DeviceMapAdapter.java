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
		   View rowView = convertView;
		   ViewHolder holder;
		   
		    if (rowView == null) {
		      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		      rowView = inflater.inflate(R.layout.row_layout, null);
		      
		      holder = new ViewHolder();
		      holder.title = (TextView) rowView.findViewById(R.id.title);
		      rowView.setTag(holder);
		    } else {
		      holder = (ViewHolder) rowView.getTag();
		    }

		    holder.title.setText(((Device)getItem(position)).getName());

		    return rowView;
	  
	  }

	  
	  
	  

}
