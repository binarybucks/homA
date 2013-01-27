package st.alr.homA;

import java.util.HashMap;

import st.alr.homA.support.Events;
import de.greenrobot.event.EventBus;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class MainActivity extends FragmentActivity {
	private RoomsFragmentPagerAdapter roomsFragmentPagerAdapter;
	private static ViewPager mViewPager;
	private static Room currentRoom;
	private static HashMap<String, DeviceMapAdapter> deviceMapAdapter = new HashMap<String, DeviceMapAdapter>();

    
	// Handle click events
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_settings:
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}




	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

		setContentView(R.layout.activity_main);
		roomsFragmentPagerAdapter = new RoomsFragmentPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);

		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
		    @Override
		    public void onPageSelected(int index) {
				currentRoom = App.getRoomAtPosition(index);
		    }

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}
		});
		mViewPager.setAdapter(roomsFragmentPagerAdapter);
	}

    @Override
    public void onSaveInstanceState (Bundle outState) {
    	super.onSaveInstanceState(outState);
    	//outState.putString("id", room.getId());

    }


    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {		
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	
	public void onEventMainThread(Events.RoomAdded event) {
		Log.v(this.toString(), "Room added");
		int currentItem = mViewPager.getCurrentItem();
		mViewPager.getAdapter().notifyDataSetChanged();
		lazyloadDeviceMapAdapter(this, event.getRoom());
			
		if(currentRoom != null && currentRoom.compareTo(App.getRoomAtPosition(currentItem)) > 0) {
			Log.v(this.toString(), "Shifting index ");
			mViewPager.setCurrentItem(currentItem+1, false);
		}
		
	}
	
	public void onEventMainThread(Events.RoomRemoved event) {
		Log.v(this.toString(), "Room removed");
		mViewPager.getAdapter().notifyDataSetChanged();
		lazyloadDeviceMapAdapter(this, event.getRoom()).clearItems();

		
	}
	
	public void onEventMainThread(Events.DeviceAddedToRoom event) {
		Log.v(this.toString(), "DeviceAddedToRoom");
		lazyloadDeviceMapAdapter(this, event.getRoom()).addItem(event.getDevice());
	}
	
	public void onEventMainThread(Events.DeviceRemovedFromRoom event) {
		Log.v(this.toString(), "DeviceRemovedFromRoom");
		lazyloadDeviceMapAdapter(this, event.getRoom()).removeItem(event.getDevice());
	}
	
	public static DeviceMapAdapter lazyloadDeviceMapAdapter(Context context, Room room) {
		DeviceMapAdapter m = deviceMapAdapter.get(room.getId());
		if (m == null) {
			m = new DeviceMapAdapter(context, room.getDevices());
			deviceMapAdapter.put(room.getId(), m);
		}
		return m;
	}
	
    public static class RoomsFragmentPagerAdapter extends FragmentStatePagerAdapter {
    	
    	public RoomsFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
            Log.v(this.toString(), "RoomsFragmentPagerAdapter instantiated ");
        }
        
    	@Override
		public int getItemPosition(Object object) {
    		return POSITION_NONE;
    	}
    	
        @Override
        public int getCount() {
            return App.getRoomCount();
        }

        @Override
        public Fragment getItem(int position) {
            return MainActivity.RoomFragment.newInstance(App.getRoomAtPosition(position).getId());
        }         
        
        @Override
        public CharSequence getPageTitle(int position) {
        	return App.getRoomAtPosition(position).getId()	;
        }
        
//        @Override
//        public Object instantiateItem(ViewGroup container, int position) {
//        	RoomFragment f = (RoomFragment) super.instantiateItem(container, position);
//        	MainActivity.roomFragments.put(App.getRoomAtPosition(position).getId(), f);
//        	return f;
//        }
//        
//        @Override
//        public void destroyItem(ViewGroup container, int position, Object object) {
//        	super.destroyItem(container, position, object);
//        	MainActivity.roomFragments.remove(object);
//        }
//        
        
    }
    
    public static class RoomFragment extends Fragment {
        Room room;

		static RoomFragment newInstance(String id) {
        	RoomFragment f = new RoomFragment();
            Bundle args = new Bundle();
            args.putString("id", id);
            f.setArguments(args);
            return f;
        }


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            room = getArguments() != null ? App.getRoom(getArguments().getString("id")) : null;            
        }

        @Override
        public void onSaveInstanceState (Bundle outState) {
        	super.onSaveInstanceState(outState);
        	outState.putString("id", room.getId());
 
        }


        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_room, container, false);
            ListView lv = (ListView)v.findViewById(R.id.devices_list);
            lv.setAdapter(MainActivity.lazyloadDeviceMapAdapter(getActivity(), room));
            return v;
        }
    }
}
