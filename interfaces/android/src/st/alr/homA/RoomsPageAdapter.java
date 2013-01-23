package st.alr.homA;

import java.util.HashMap;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class RoomsPageAdapter extends FragmentPagerAdapter {
	private HashMap<String, Room> map;
	final Handler uiThreadHandler = new Handler();

//	public RoomsPageAdapter(Context context) {
//		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		map = new HashMap<String, Room>();
//	}
	
	public RoomsPageAdapter(FragmentManager fm) {
		super(fm);
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

	
	public class RoomFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		public RoomFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			// Create a new TextView and set its text to the fragment's section
			// number argument value.
			
			//setContentView(R.layout.activity_main);
			Room room = App.getRoomAtPosition(getArguments().getInt(ARG_SECTION_NUMBER));
			
			
			TextView textView = new TextView(getActivity());
			textView.setGravity(Gravity.CENTER);
			textView.setText(room.getId());
			return textView;
		}
	}

	@Override
	public Fragment getItem(int p) {
		Fragment fragment = new RoomFragment();
		Bundle args = new Bundle();
		args.putInt(RoomFragment.ARG_SECTION_NUMBER, p);
		fragment.setArguments(args);
		return fragment;
	}


	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return map.size();
	}
	

	@Override
	public CharSequence getPageTitle(int position) {
		return App.getRoomAtPosition(position).getId().toUpperCase();
	}
	




}
