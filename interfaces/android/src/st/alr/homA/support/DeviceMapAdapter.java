package st.alr.homA.support;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import st.alr.homA.R;
import st.alr.homA.model.Device;
import android.widget.TextView;

public class DeviceMapAdapter extends MapAdapter<String, Device> {

	public DeviceMapAdapter(Context context, ValueSortedMap<String, Device> map) {
	    super(context, new ValueSortedMap<String, Device>(map));
	}

	static class ViewHolder {
		public TextView title;
	}

	public void setMap(ValueSortedMap<String, Device> map) {
	    this.map = map;
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

		holder.title.setText(((Device) getItem(position)).getName());

		return rowView;

	}



}
