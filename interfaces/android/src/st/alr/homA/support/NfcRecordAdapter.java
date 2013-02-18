
package st.alr.homA.support;

import java.util.ArrayList;
import java.util.HashMap;

import st.alr.homA.R;
import st.alr.homA.model.Device;
import st.alr.homA.support.DeviceMapAdapter.ViewHolder;

import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class NfcRecordAdapter extends BaseAdapter {
    private HashMap<String, String> map;
    Context context;

    public NfcRecordAdapter(Context context) {
        this.context = context;
        map = new HashMap<String, String>();
    }

    @Override
    public int getCount() {

        return map.size();
    }

    @Override
    public Object getItem(int position) {
        return getValue(position);
    }

    public String getValue(int position) {
        return (String) map.values().toArray()[position];
    }

    public String getKey(int position) {
        return (String) map.keySet().toArray()[position];
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    static class ViewHolder {
        public TextView topic;
        public TextView payload;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        ViewHolder holder;

        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.two_line_row_layout, null);

            holder = new ViewHolder();
            holder.topic = (TextView) rowView.findViewById(android.R.id.text1);
            holder.payload = (TextView) rowView.findViewById(android.R.id.text2);

            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        holder.topic.setText(getKey(position));
        holder.payload.setText(getValue(position));

        return rowView;
    }

    public void put(String topic, String payload) {
        synchronized (map) {

            map.put(topic, payload);
            this.notifyDataSetChanged();
        }
    }

    public void remove(SparseBooleanArray sba) {
        synchronized (map) {
            ArrayList<String> keysToDelete = new ArrayList<String>();

            for (int i = 0; i < getCount(); i++)
                if (sba.get(i))
                    keysToDelete.add((String) map.keySet().toArray()[i]);

            for (String key : keysToDelete)
                map.remove(key);

            this.notifyDataSetChanged();
        }
    }

    public void remove(String topic) {
        synchronized (map) {

            map.remove(topic);
            this.notifyDataSetChanged();
        }
    }

    public HashMap<String, String> getMap() {
        return new HashMap<String, String>(map);
    }

}
