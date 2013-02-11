package st.alr.homA.support;

import java.util.Map;

import android.content.Context;
import android.widget.BaseAdapter;

public abstract class MapAdapter<K, T> extends BaseAdapter {
	protected ValueSortedMap<K, T> map;
	protected Context context; 
	
	public MapAdapter(Context c, ValueSortedMap<K, T> map) {
	    this.map = map;
	    this.context = c;
	}

	@SuppressWarnings("unchecked")
    public void addItem(T object) {
		this.map.put((K) object.toString(), object);
		this.notifyDataSetChanged();
	}
	
	public void removeItem(T object) { 
		this.map.remove(object.toString());
		this.notifyDataSetChanged();
	}
	
	public void clearItems() {
		this.map.clear();
		this.notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return this.map.size();
	}

	@Override
	public Object getItem(int position) {
		return this.map.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}
    public void sortDataset() {
        map.sortDataset();
        this.notifyDataSetChanged();

        
    }
}
