package st.alr.homA;

import java.util.HashMap;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class RoomsHashMapAdapter extends BaseAdapter {
	private HashMap<String, Room> map;
	private LayoutInflater inflater;
	final Handler uiThreadHandler = new Handler();

	public RoomsHashMapAdapter(Context context) {
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		map = new HashMap<String, Room>();
	}

	public void addOnMainThread(Room room) {

		class AddRunnable implements Runnable {
			Room room;

			AddRunnable(Room r) {
				room = r;
			}

			@Override
			public void run() {
				synchronized (this) {
					map.put(room.getId(), room);
					notifyDataSetChanged();
					notifyAll();
				}
			}
		}

		AddRunnable r = new AddRunnable(room);
		synchronized (r) {
			uiThreadHandler.post(r);
			try {
				r.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void notifyDataSetChangedOnMainThread() {
		uiThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
			}
		});

	}

	public void removeOnMainThread(Room room) {
		class RemoveRunnable implements Runnable {
			Room room;

			RemoveRunnable(Room r) {
				room = r;
			}

			@Override
			public void run() {
				synchronized (this) {
					map.remove(room.getId());
					notifyDataSetChanged();
					notifyAll();
				}
			}
		}
		RemoveRunnable r = new RemoveRunnable(room);
		synchronized (r) {
			uiThreadHandler.post(r);
			try {
				r.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public void clearOnMainThread() {
		class ClearRunnable implements Runnable {
			@Override
			public void run() {
				synchronized (this) {
					map.clear();
					notifyDataSetChanged();
					notifyAll();
				}
			}
		}
		ClearRunnable r = new ClearRunnable();
		synchronized (r) {
			uiThreadHandler.post(r);
			try {
				r.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public int getCount() {
		return map.size();
	}

	public Object getRoom(String id) {
		return map.get(id);
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
