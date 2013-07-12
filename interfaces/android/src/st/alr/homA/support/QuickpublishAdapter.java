package st.alr.homA.support;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import st.alr.homA.R;
import st.alr.homA.model.Device;
import st.alr.homA.model.Quickpublish;
import st.alr.homA.support.DeviceMapAdapter.ViewHolder;

import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class QuickpublishAdapter extends BaseAdapter {
    private ArrayList<Quickpublish> values;
    Context context;
    
    @Override
    public int getCount() {
        return values.size();
    }

    public QuickpublishAdapter(Context context) {
        this(context, new ArrayList<Quickpublish>());
    }
    
    public QuickpublishAdapter(Context context, ArrayList<Quickpublish> values) {
        this.values = values != null? values : new ArrayList<Quickpublish>();
        this.context = context;
    }

    
    @Override
    public Quickpublish getItem(int arg0) {
        return values.get(arg0);
    }

    public ArrayList<Quickpublish> getValues(){
        return values;
    }
    
    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    
    static class ViewHolder {
        public TextView title;
        public TextView subtitle;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        ViewHolder holder;
        Quickpublish q = getItem(position);
        
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.two_line_row_layout, null);

            holder = new ViewHolder();
            holder.title = (TextView) rowView.findViewById(android.R.id.text1);
            holder.subtitle = (TextView) rowView.findViewById(android.R.id.text2);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        holder.title.setText(q.getTopic());
        holder.subtitle.setText("Payload: " + q.getPayload() + " Retained: " + q.isRetained());

        return rowView;

    }
    
    public void add(Quickpublish q) {
        values.add(q);
        notifyDataSetChanged();

    }

    public void remove(SparseBooleanArray sba) {
            for (int i = 0; i < getCount(); i++)
                if (sba.get(i))
                    values.remove(i);
            notifyDataSetChanged();
    }

}
