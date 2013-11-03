
package st.alr.homA.support;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import st.alr.homA.R;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class NfcRecordAdapter extends BaseAdapter {
    private HashMap<String, MqttMessage> map;
    Context context;

    public NfcRecordAdapter(Context context) {
        this.context = context;
        map = new HashMap<String, MqttMessage>();
    }

    @Override
    public int getCount() {

        return map.size();
    }

    @Override
    public Object getItem(int position) {
        return getValue(position);
    }

    public MqttMessage getValue(int position) {
        return (MqttMessage) map.values().toArray()[position];
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

        MqttMessage m = getValue(position);
        String payload;
        payload = new String(m.getPayload());
        holder.topic.setText(getKey(position));
        holder.payload.setText("Payload: " + payload + " Retained: " + m.isRetained());

        return rowView;
    }

    public void put(String topic, MqttMessage message) {
        synchronized (map) {

            map.put(topic, message);
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

    public HashMap<String, MqttMessage> getMap() {
        return new HashMap<String, MqttMessage>(map);
    }

}
