
package st.alr.homA.support;

import java.util.Collection;
import java.util.concurrent.Semaphore;

import st.alr.homA.ActivityMain;
import st.alr.homA.App;
import st.alr.homA.model.Device;
import st.alr.homA.model.Room;
import de.greenrobot.event.EventBus;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.BaseAdapter;

public abstract class MapAdapter<K, T> extends BaseAdapter {
    protected ValueSortedMap<K, T> map;
    protected Context context;

    public MapAdapter(Context c) {
        this.map = new ValueSortedMap<K, T>();
        this.context = c;
    }

    @SuppressWarnings("unchecked")
    public synchronized void addItem(T object) {
        this.map.put((K) object.toString(), object);
        reload();
    }
    

    public synchronized void setMap(ValueSortedMap<K, T> map) {
        this.map = map;
        reload();
    }

    public synchronized void removeItem(T object) {
        this.map.remove(object.toString());
        reload();
     }

    public synchronized void clearItems() {
        this.map.clear();        
        reload();
    }

    public synchronized void reload() {

        
        this.notifyDataSetChanged();

    }

    @Override
    public synchronized int getCount() {
        return this.map.size();
    }

    @Override
    public synchronized Object getItem(int position) {
        return this.map.get(position);
    }

    public synchronized Object getItem(String key) {
        return this.map.get(key);
    }

    @Override
    public synchronized long getItemId(int position) {
        return 0;
    }

    public synchronized void sortDataset() {
        map.sortDataset();
        reload();
    }
    
    public Collection<Object> getMap(){
        return (Collection<Object>) map.values();
    }
}
