package st.alr.homA;

import de.greenrobot.event.EventBus;
import st.alr.homA.*;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends FragmentActivity {
	private RoomsFragmentPagerAdapter roomsFragmentPagerAdapter;
	private ViewPager mViewPager;

	
    public static class RoomFragment extends Fragment {
        int mNum;

        /**
         * Create a new instance of CountingFragment, providing "num"
         * as an argument.
         */
        static RoomFragment newInstance(int num) {
        	RoomFragment f = new RoomFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putInt("num", num);
            f.setArguments(args);

            return f;
        }

        /**
         * When creating, retrieve this instance's number from its arguments.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mNum = getArguments() != null ? getArguments().getInt("num") : 1;
        }

        /**
         * The Fragment's UI is just a simple text view showing its
         * instance number.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_room, container, false);
            View tv = v.findViewById(R.id.roomname);
            ((TextView)tv).setText("Fragment #" + App.getRoomAtPosition(mNum).getId());
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
		setContentView(R.layout.activity_main);
		roomsFragmentPagerAdapter = new RoomsFragmentPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(roomsFragmentPagerAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {		
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	
	
    public static class RoomsFragmentPagerAdapter extends FragmentPagerAdapter {
        public RoomsFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
            Log.v(this.toString(), "RoomsFragmentPagerAdapter instantiated");
            EventBus.getDefault().register(this);
        }
        
    	public void onEventMainThread(Events.RoomAdded event) {
    		Log.v(this.toString(), "Room added");
    		this.notifyDataSetChanged();
    	}
    	public void onEventMainThread(Events.RoomRemoved event) {
    		Log.v(this.toString(), "Room removed");
    		this.notifyDataSetChanged();
    	}

        @Override
        public int getCount() {
            return App.getRoomCount();
        }

        @Override
        public Fragment getItem(int position) {
            return MainActivity.RoomFragment.newInstance(position);
        }
        
        @Override
        public int getItemPosition(Object object){
        	return POSITION_NONE;
        }
    }

    




}
