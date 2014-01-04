
package st.alr.homA;

import st.alr.homA.model.Control;
import st.alr.homA.model.Device;
import st.alr.homA.model.Room;
import st.alr.homA.services.ServiceMqtt;
import st.alr.homA.support.Defaults;
import st.alr.homA.support.DeviceAdapter;
import st.alr.homA.support.Events;
import st.alr.homA.support.ValueSortedMap;
import st.alr.homA.view.ControlView;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import de.greenrobot.event.EventBus;

public class ActivityMain extends FragmentActivity {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;   
    RelativeLayout disconnectedLayout;
    LinearLayout connectedLayout;
    private CharSequence title;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        
        
        
        int itemId = item.getItemId();
        
            if (mDrawerToggle.onOptionsItemSelected(item)) {
              return true;
            }
            // Handle your other action bar items...

        if (itemId == R.id.menu_settings) {
            i = new Intent(this, ActivityPreferences.class);
            startActivity(i);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void onEventMainThread(Events.StateChanged.ServiceMqtt event) {
        updateViewVisibility();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent service = new Intent(this, ServiceMqtt.class);
        startService(service);
    }

    private void updateViewVisibility() {
        if (ServiceMqtt.getState() == st.alr.homA.support.Defaults.State.ServiceMqtt.CONNECTED) {
            Log.v(this.toString(), "Showing connected layout");
            connectedLayout.setVisibility(View.VISIBLE);
            disconnectedLayout.setVisibility(View.INVISIBLE);
            setActionbarTitle();
            
        } else {
            Log.v(this.toString(), "Showing disconnected layout");

            connectedLayout.setVisibility(View.INVISIBLE);
            disconnectedLayout.setVisibility(View.VISIBLE);
            setActionbarTitleAppname();

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateViewVisibility();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        if (NfcAdapter.getDefaultAdapter(this) == null
//                || !NfcAdapter.getDefaultAdapter(this).isEnabled()) {
//            menu.removeItem(R.id.menu_nfc);
//        }

        return true;
    }

    /**
     * @category START
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        
        
        disconnectedLayout = (RelativeLayout) findViewById(R.id.disconnectedLayout);
        connectedLayout = (LinearLayout) findViewById(R.id.connectedLayout);

        updateViewVisibility();


        // Set the adapter for the list view
        mDrawerList.setAdapter(App.getRoomListAdapter());
        setActionbarTitleAppname();


        mDrawerList.setOnItemClickListener(new OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectRoom(App.getRoom(position));
                mDrawerLayout.closeDrawer(mDrawerList);
                
            }

            
        });

        
        
        
        mDrawerToggle = new ActionBarDrawerToggle(this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_navigation_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.na,  /* "open drawer" description */
                R.string.na  /* "close drawer" description */) {


            
            
/** Called when a drawer has settled in a completely closed state. */

            @Override
            public void onDrawerClosed(View view) {
                setActionbarTitle();
            }

            
/** Called when a drawer has settled in a completely open state. */

            @Override
            public void onDrawerOpened(View drawerView) {
                setActionbarTitleAppname();
            }


        };
        
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerToggle.syncState();
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        
        Room selected =  App.getRoom(PreferenceManager.getDefaultSharedPreferences(this).getString("selectedRoomId", "")); 
        if(selected != null)
            selectRoom(selected);
        
        EventBus.getDefault().register(this);
    }
    
    protected void setActionbarTitleAppname() {
        String appname = getString(R.string.appName);
        String abtitle = (String) getActionBar().getTitle();
        if(abtitle != null && !abtitle.equals(appname))
            title = abtitle;        
           
        getActionBar().setTitle(appname);
        
    }

    protected void setActionbarTitle(String t){   
     
        Log.v(this.toString(), "setActionbarTitle with parameter to to " + t);

        getActionBar().setTitle(t);
        title = t;
    }
    
    protected void setActionbarTitle() {
        Log.v(this.toString(), "setActionbarTitle to " + title);
        if(title != null)
            getActionBar().setTitle(title);
        else 
            setActionbarTitleAppname();
    }

    private void selectRoom(Room r) {
        Log.v(this.toString(), "selecting " + r.getId());
        Fragment f = RoomFragment.newInstance(r);
        
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                       .replace(R.id.content_frame, f)
                       .commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("selectedRoomId", r.getId()).commit();
        Log.v(this.toString(), "selected2 "+ PreferenceManager.getDefaultSharedPreferences(this).getString("selectedRoomId", ""));

        setActionbarTitle(r.getId());
    }
    
    public void onEventMainThread(Events.RoomAdded e) {
        if(e.getRoom().getId().equals(PreferenceManager.getDefaultSharedPreferences(this).getString("selectedRoomId", ""))) {
            selectRoom(e.getRoom());
        } else {
            Log.v(this.toString(), "selected "+ PreferenceManager.getDefaultSharedPreferences(this).getString("selectedRoomId", ""));
            Log.v(this.toString(), "room " +e.getRoom().getId());
            
        }
    }
    
    public static class RoomFragment extends Fragment {
        Room room;
        DeviceAdapter adapter;
        private ListView listView;
        
        public static RoomFragment newInstance(Room r) {
            RoomFragment f = new RoomFragment();            
            Bundle args = new Bundle();
            args.putString("roomId", r.getId());
            f.setArguments(args);

            return f;
        }
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            room = App.getRoom(getArguments().getString("roomId"));
            if(room == null) {
                getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
                Log.e(this.toString(), "Clearing fragment for removed room");
                return;
            }
            

        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            
            
            View v = inflater.inflate(R.layout.fragment_room, container, false);
            if(room == null)
                return v;
            
            this.listView = (ListView)v.findViewById(R.id.devices_list);
            listView.setAdapter(room.getAdapter());
            listView.setOnItemClickListener(new OnItemClickListener(){

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    android.support.v4.app.FragmentManager fm = getFragmentManager();
                    android.support.v4.app.FragmentTransaction ft = fm.beginTransaction();

                    DeviceFragment d = DeviceFragment.newInstance(room.getId(), room.getDevice(position).toString());
                    ft.add(d, "tag");
                    ft.commit();                    
                }
                
            });
            return v;
        }
    }

    public static class DeviceFragment extends android.support.v4.app.DialogFragment {
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

        public void onEventMainThread(Events.StateChanged.ServiceMqtt event) {
            if (event.getState() != Defaults.State.ServiceMqtt.CONNECTED) {
                Log.v(this.toString(), "Lost connection, closing currently open dialog");
                android.support.v4.app.FragmentManager fragmentManager = getFragmentManager();
                android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager
                        .beginTransaction();
                fragmentTransaction.remove(this);
                fragmentTransaction.commit();
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            // After long times of inactivity with an open dialog, Android might
            // decide to swap out the room or device
            // In this case there is nothing left that we can save.
            // Restoring the fragment from the bundle will likely return nothing
            // (see setArgs).
            if (device != null && room != null) {
                outState.putString("roomId", room.getId());
                outState.putString("deviceId", device.toString());
            }
        }

        private boolean setArgs(Bundle savedInstanceState) {
            Bundle b;
            if (savedInstanceState != null)
                b = savedInstanceState;
            else
                b = getArguments();

            room = App.getRoom(b.getString("roomId"));
            if (room == null) {
                Log.e(this.toString(), "DeviceFragment for phantom room: " + b.getString("roomId"));
                return false;
            }

            device = room.getDevice(b.getString("deviceId"));

            return true;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LinearLayout outerLayout = new LinearLayout(this.getActivity());
            outerLayout.setOrientation(LinearLayout.VERTICAL);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            if (setArgs(savedInstanceState)) {
                EventBus.getDefault().register(this);

                // Use the Builder class for convenient dialog construction
                builder.setTitle(device.getName());

                ScrollView sw = new ScrollView(this.getActivity());

                LinearLayout ll = new LinearLayout(this.getActivity());
                ll.setOrientation(LinearLayout.VERTICAL);
                ll.setPadding(16, 0, 16, 0);
                for (Control control : device.getControls().values()) {
                    ll.addView(getControlView(control).attachToControl(control).getLayout());
                }

                sw.addView(ll);
                outerLayout.addView(sw);
            }

            builder.setView(outerLayout);
            return builder.create();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = super.onCreateView(inflater, container, savedInstanceState);
            getDialog().setCanceledOnTouchOutside(true);
            return v;
        }

        public ControlView getControlView(Control control) {
            ControlView v = null;

            if (control.getMeta("type", "text").equals("switch")) {
                v = new st.alr.homA.view.ControlViewSwitch(getActivity());

            } else if (control.getMeta("type", "text").equals("range")) {
                v = new st.alr.homA.view.ControlViewRange(getActivity());

            } else {
                v = new st.alr.homA.view.ControlViewText(getActivity());

            }

            return v;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            Log.v(this.toString(), "DeviceFragment: onDestroyView");

            ValueSortedMap<String, Control> controls;

            if ((device != null) && ((controls = device.getControls()) != null))
                for (Control control : controls.values())
                    control.removeValueChangedObserver();

            EventBus.getDefault().unregister(this);
        }
    }
}
