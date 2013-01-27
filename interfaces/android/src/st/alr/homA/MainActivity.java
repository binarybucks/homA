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
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends FragmentActivity {
	private RoomsFragmentPagerAdapter roomsFragmentPagerAdapter;
	private static ViewPager mViewPager;
	private static Room currentRoom;
	private static HashMap<String, Fragment> roomFragments = new HashMap<String, Fragment>();

    
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
	
	
	public void onPageSelected(int arg0)
	{
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
				Log.v(this.toString(), "onPageSelected");
				currentRoom = App.getRoomAtPosition(index);
		    }

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}
		});
		mViewPager.setAdapter(roomsFragmentPagerAdapter);
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
		
		if(currentRoom != null && currentRoom.compareTo(App.getRoomAtPosition(currentItem)) > 0) {
			Log.v(this.toString(), "Shifting index ");

			mViewPager.setCurrentItem(currentItem+1, false);
		}
		
	}
	
	public void onEventMainThread(Events.RoomRemoved event) {
		Log.v(this.toString(), "Room removed");
		mViewPager.getAdapter().notifyDataSetChanged();
		//mViewPager.setCurrentItem(mViewPager.getc)
	}
	
	public void onEventMainThread(Events.DeviceAddedToRoom event) {
		Log.v(this.toString(), "DeviceAddedToRoom");
		RoomFragment f = (RoomFragment)roomFragments.get(event.getRoom().getId());
		DeviceMapAdapter a = f.getDevicesAdater();
		
		a.addItem(event.getDevice());
		a.notifyDataSetChanged();
	}

	
    public static class RoomsFragmentPagerAdapter extends FragmentStatePagerAdapter {
    	
    	public RoomsFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
            Log.v(this.toString(), "RoomsFragmentPagerAdapter instantiated ");
        }
        
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
        
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
        	RoomFragment f = (RoomFragment) super.instantiateItem(container, position);
        	MainActivity.roomFragments.put(App.getRoomAtPosition(position).getId(), f);
        	return f;
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        	super.destroyItem(container, position, object);
        	MainActivity.roomFragments.remove(object);
        }
        
        
    }
    
    public static class RoomFragment extends Fragment {
        Room room;
        private DeviceMapAdapter devicesAdater;

        
        public DeviceMapAdapter getDevicesAdater() {
			return devicesAdater;
		}

		static RoomFragment newInstance(String id) {
        	RoomFragment f = new RoomFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putString("id", id);
            f.setArguments(args);

            return f;
        }

        /**
         * When creating, retrieve this instance's number from its arguments.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            room = getArguments() != null ? App.getRoom(getArguments().getString("id")) : null;
            devicesAdater = new DeviceMapAdapter(getActivity(), room.getDevices());
            
        }

        /**
         * The Fragment's UI is just a simple text view showing its
         * instance number.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_room, container, false);
//            TextView tv = (TextView)v.findViewById(R.id.room_name);
            
//            tv.setText("Room #" + room.getId());
            ListView lv = (ListView)v.findViewById(R.id.devices_list);
            lv.setAdapter(devicesAdater);
            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
          //  setListAdapter(new ArrayAdapter<String>(getActivity(),
                  //  android.R.layout.simple_list_item_1, Cheeses.sCheeseStrings));
        }

//        @Override
//        public void onListItemClick(ListView l, View v, int position, long id) {
//            Log.v(this.toString(), "Item clicked: " + id);
//        }
    }
}
