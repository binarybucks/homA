package st.alr.homA.support;

import de.greenrobot.event.EventBus;
import st.alr.homA.App;
import st.alr.homA.R;
import st.alr.homA.model.Device;
import st.alr.homA.model.Room;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class RoomAdapter extends MapAdapter<String, Room> {
    
    public RoomAdapter(Context c) {
        super(c);
    }

    @Override
    public long getItemId(int position) {
        return 0;
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

        holder.title.setText(((Room) getItem(position)).getId());
        
        return rowView;

    }

    
    


}