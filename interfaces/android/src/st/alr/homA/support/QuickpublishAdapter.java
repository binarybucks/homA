package st.alr.homA.support;

import java.util.ArrayList;
import java.util.HashMap;

import st.alr.homA.R;
import st.alr.homA.model.Quickpublish;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class QuickpublishAdapter extends BaseAdapter {
    private HashMap<String, Quickpublish> map;
    Context context;
    
    @Override
    public int getCount() {
        return map.size();
    }

    public QuickpublishAdapter(Context context) {
        this.map = map != null? map : new HashMap<String, Quickpublish>();
        this.context = context;
    }
    
    public QuickpublishAdapter(Context context, ArrayList<Quickpublish> array) {
        this(context);

        for (Quickpublish quickpublish : array) 
            add(quickpublish);

    }

    
    @Override
    public Quickpublish getItem(int arg0) {
        return (Quickpublish) map.values().toArray()[arg0];
    }

    public ArrayList<Quickpublish> getValues(){        
        return new ArrayList<Quickpublish>(map.values());
    }
    
    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    
    static class ViewHolder {
        public TextView name;
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
            holder.name = (TextView) rowView.findViewById(android.R.id.text1);
            holder.subtitle = (TextView) rowView.findViewById(android.R.id.text2);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        holder.name.setText(q.getName());
        holder.subtitle.setText("Payload: " + q.getPayload() + " Retained: " + q.isRetained());

        return rowView;

    }
    
    public void add(Quickpublish q) {
        map.put(q.getTopic(), q);
        notifyDataSetChanged();
    }
    
    public void remove(Quickpublish q) {
        map.remove(q.getTopic());
        notifyDataSetChanged();
    }

    public void remove(int position) {
        map.remove(getItem(position));
        notifyDataSetChanged();
    }

    
    public void remove(SparseBooleanArray sba) {
            for (int i = 0; i < getCount(); i++)
                if (sba.get(i))
                    remove(i);
            notifyDataSetChanged();
    }

}
