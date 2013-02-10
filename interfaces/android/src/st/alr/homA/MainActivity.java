
package st.alr.homA;

import java.util.HashMap;
import java.util.Locale;

import st.alr.homA.MqttService.LocalBinder;
import st.alr.homA.model.Control;
import st.alr.homA.model.Device;
import st.alr.homA.model.Room;
import st.alr.homA.support.DeviceMapAdapter;
import st.alr.homA.support.Events;
import st.alr.homA.support.ValueChangedObserver;
import st.alr.homA.view.ControlView;
import st.alr.homA.view.RangeControlView;
import st.alr.homA.view.SwitchControlView;
import st.alr.homA.view.TextControlView;
import de.greenrobot.event.EventBus;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends FragmentActivity {
    private RoomsFragmentPagerAdapter roomsFragmentPagerAdapter;
    private static ViewPager mViewPager;
    private static Room currentRoom;
    private static HashMap<String, DeviceMapAdapter> deviceMapAdapter = new HashMap<String, DeviceMapAdapter>();
    
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
    protected void onStart() {
        super.onStart();
        Intent service = new Intent(this, MqttService.class);
        startService(service);
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // outState.putString("id", room.getId());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    public void onEventMainThread(Events.RoomAdded event) {
        Log.v(this.toString(), "Room added: " + event.getRoom().getId());
        int currentItem = mViewPager.getCurrentItem();
        mViewPager.getAdapter().notifyDataSetChanged();
        DeviceMapAdapter m = lazyloadDeviceMapAdapter(this, event.getRoom());
        m.notifyDataSetChanged();

        if (currentRoom != null && currentRoom.compareTo(App.getRoomAtPosition(currentItem)) > 0) {
            Log.v(this.toString(), "Shifting index ");
            mViewPager.setCurrentItem(currentItem + 1, false);
        }

    }

    public void onEventMainThread(Events.RoomRemoved event) {
        Log.v(this.toString(), "Room removed: " + event.getRoom().getId());
        mViewPager.getAdapter().notifyDataSetChanged();
        DeviceMapAdapter m = lazyloadDeviceMapAdapter(this, event.getRoom());
        m.clearItems();
        m.notifyDataSetChanged();

    }

    public void onEventMainThread(Events.DeviceAddedToRoom event) {
        Log.v(this.toString(), "DeviceAddedToRoom: " + event.getDevice().toString() + " "
                + event.getRoom().getId());
        DeviceMapAdapter m = lazyloadDeviceMapAdapter(this, event.getRoom());

        m.addItem(event.getDevice());
        m.notifyDataSetChanged();
    }

    public void onEventMainThread(Events.DeviceRemovedFromRoom event) {
        Log.v(this.toString(), "DeviceRemovedFromRoom: " + event.getDevice().toString() + " "
                + event.getRoom().toString());
        DeviceMapAdapter m = lazyloadDeviceMapAdapter(this, event.getRoom());
        m.removeItem(event.getDevice());
        m.notifyDataSetChanged();
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
            return App.getRoomAtPosition(position).getId().toUpperCase(Locale.ENGLISH);
        }
    }

    public static class DeviceFragment extends DialogFragment {
        Room room; 
        Device device;

        static DeviceFragment newInstance(String roomId, String deviceId) {
            DeviceFragment f = new DeviceFragment();
            Bundle args = new Bundle();
            args.putString("roomId", roomId);
            args.putString("deviceId", deviceId);

            f.setArguments(args);
            return f;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            room = getArguments() != null ? App.getRoom(getArguments().getString("roomId")) : null;
            device = getArguments() != null ? room.getDevices().get(getArguments().getString("deviceId")) : null;

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(device.getName());

            LinearLayout ll = new LinearLayout(this.getActivity());
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setPadding(16, 0, 16, 0);

            for (Control control : device.getControls().values()) {
                ll.addView(getControlView(control)._layout);
            }

            builder.setView(ll);
            return builder.create();
        }





        public ControlView getControlView(Control control) {

            ControlView v;

            switch (control.getType()) {
                case SWITCH:
                    v = new SwitchControlView(getActivity());
                    break;
                case RANGE:
                    v = new RangeControlView(getActivity());
                    break;    
                case TEXT:
                    v = new TextControlView(getActivity());
                    break;    
                default:
                    // TODO: Add view for unknown types here instead of a textview
                    v = new TextControlView(getActivity());
            }

            return v;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            for (Control control : device.getControls().values()) {
                control.removeValueChangedObserver();
            }
        }
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
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString("id", room.getId());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_room, container, false);
            ListView lv = (ListView) v.findViewById(R.id.devices_list);
            lv.setAdapter(MainActivity.lazyloadDeviceMapAdapter(getActivity(), room));

            lv.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long it) {
                    DeviceFragment.newInstance(room.getId(), room.getDevices().values().toArray()[position].toString()).show(getFragmentManager(), "tag");
                }
            });

            return v;
        }

    }
}
