package st.alr.homA;

import java.util.HashMap;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class RoomsHashMapAdapter extends BaseAdapter {
	private HashMap<String, Room> map;
	private Context context;
	private LayoutInflater inflater;
	private final Object mLock = new Object();
	final Handler uiThreadHandler = new Handler();

	public RoomsHashMapAdapter(Context context) {
		this.context = context;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		map = new HashMap<String, Room>();
	}

	public void add(Room room) {
		synchronized (mLock) {
			Log.v(toString(), "adding room with id: " + room.getId());

			map.put(room.getId(), room);
			Log.v(toString(), "checking: " + map.get(room.getId().toString()));

		}

		notifyDataSetChangedOnMainThread();
	}

	public void notifyDataSetChangedOnMainThread() {
		uiThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
			}
		});

	}

	public void remove(Room room) {

		synchronized (mLock) {

			map.remove(room.getId());
			Log.v(toString(), "roomRemoved. New content is: " + map);
		}
		notifyDataSetChangedOnMainThread();
	}

	public void clear() {
		uiThreadHandler.post(new Runnable() {
			@Override
			public void run() {

				synchronized (mLock) {

					map.clear();
				}

				notifyDataSetChanged();
			}
		});
	}

	@Override
	public int getCount() {
		return map.size();
	}

	public Object getRoom(String id) {
		Log.v(toString(), "getting room with id: " + id);
		Room r = map.get(id);
		Log.v(toString(), "Got:" + r);

		return r;
	}

	@Override
	public Object getItem(int arg0) {
		return map.values().toArray()[arg0];
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return createViewFromResource(position, convertView, parent, R.layout.room_list_item);
	}

	private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
		View view;

		if (convertView == null) {
			view = inflater.inflate(resource, parent, false);
		} else {
			view = convertView;
		}

		try {
			Room room = (Room) getItem(position);
			TextView room_name = (TextView) view.findViewById(R.id.room_name);
			TextView room_device_count = (TextView) view.findViewById(R.id.room_device_count);
			room_name.setText(room.toString());
			room_device_count.setText(room.getDevices().size() + " devices");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return view;

	}

}
