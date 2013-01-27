package st.alr.homA;

import java.util.TreeMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import st.alr.homA.support.TreeMapAdapter;
import android.widget.TextView;

public class ControllsMapAdapter extends TreeMapAdapter<Control> {

	public ControllsMapAdapter(Context context, TreeMap<String, Control> map) {
		super(context, map);		
	}

	  @Override
	  public View getView(int position, View convertView, ViewGroup parent) {
	    View rowView = convertView;
	    if (rowView == null) {
	    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        rowView = inflater.inflate(R.layout.fragment_device_switch, parent, false);
	        TextView textView = (TextView) rowView.findViewById(R.id.controlName_switch);
	        textView.setText(getItem(position).toString());

	    }
	    return rowView;
	  }


}
