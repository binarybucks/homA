
package st.alr.homA.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ValueSortedMap<K, V> implements Map<K, V> {
    ArrayList<V> dataSorted;
    HashMap<K, V> dataMap;

    public ValueSortedMap() {
        this.dataMap = new HashMap<K, V>();
        this.dataSorted = new ArrayList<V>();
    }

    public ValueSortedMap(Map<K, V> dataMap) {
        this.dataMap = new HashMap<K, V>(dataMap);
        this.dataSorted = new ArrayList<V>(this.dataMap.values());
        sortDataset();
    }

    public ValueSortedMap(ValueSortedMap<K, V> map) {
        this.dataMap = new HashMap<K, V>(map.getDataMap());
        this.dataSorted = new ArrayList<V>(map.getDataSorted());
    }

    public V get(String key) {
        synchronized (dataSorted) {

            return dataMap.get(key);
        }
    }

    public V get(int index) {
        synchronized (dataSorted) {

            return dataSorted.get(index);
        }
    }

    public void sortDataset() {
        synchronized (dataSorted) {

            Collections.sort(dataSorted, new GenericObjectComparator());
        }
    }

    @Override
    public V put(K key, V value) {
        synchronized (dataSorted) {

            // TODO: Optimize by avoiding recreating of dataSorted list on every insert
            dataMap.put(key, value);
            dataSorted = new ArrayList<V>(dataMap.values());
            
            sortDataset();
            return value;
        }
    }

    public int size() {
        synchronized (dataSorted) {
            return dataMap.size();
        }
    }

    public Collection<V> values() {
        synchronized (dataSorted) {

            return this.dataSorted;
        }
    }

    public void clear() {
        synchronized (dataSorted) {

            this.dataMap.clear();
            this.dataSorted.clear();
        }
    }

    public V remove(Object key) {
        synchronized (dataSorted) {
            V v = dataMap.get(key);
            dataSorted.remove(v);
            dataMap.remove(key);
            return v;
        }
    }

    @Override
    public V get(Object key) {
        synchronized (dataSorted) {
            return dataMap.get(key);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return dataMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return dataMap.containsValue(value);
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return dataMap.entrySet();
    }

    @Override
    public boolean isEmpty() {
        return size() <= 0;
    }

    @Override
    public Set<K> keySet() {
        synchronized (dataSorted) {

            return dataMap.keySet();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> arg0) {

    }

    public class GenericObjectComparator implements Comparator<V>
    {
        @SuppressWarnings("unchecked")
        public int compare(V left, V right) {
            return ((Comparable<V>) left).compareTo(right);
        }
    }

    private ArrayList<V> getDataSorted() {
        synchronized (dataSorted) {

            return dataSorted;
        }
    }

    private HashMap<K, V> getDataMap() {
        synchronized (dataSorted) {

            return dataMap;
        }
    }

}
