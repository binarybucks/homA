package st.alr.homA.support;

import java.util.TreeMap;

import android.content.Context;
import android.widget.BaseAdapter;

public abstract class TreeMapAdapter<T> extends BaseAdapter {
	protected TreeMap<String, T> map;
	protected Context context; 
	
	@SuppressWarnings("unchecked")
	public TreeMapAdapter(Context c, TreeMap<String, T> map) {
	    this.map = (TreeMap<String, T>) map.clone();
	    this.context = c;
	}

	public void addItem(T object) {
		this.map.put(object.toString(), object);
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
		return this.map.keySet().toArray()[position];
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}
}
