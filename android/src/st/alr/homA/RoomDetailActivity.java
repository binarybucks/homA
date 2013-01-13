package st.alr.homA;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;

public class RoomDetailActivity extends Activity {
	LayoutInflater inflater;
	Room room;
	BroadcastReceiver deviceAddedToRoomReceiver;
	BroadcastReceiver deviceRemovedFromRoomReceiver;
	BroadcastReceiver mqttConnectivityChangedReceiver;

	LinearLayout ll;
	HashMap<String, View> deviceViews = new HashMap<String, View>();

	private ArrayList<ValueChangedObserver> openObservers = new ArrayList<ValueChangedObserver>();

	@Override
	public void onDestroy() {
		clearObservers();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
	    App.activityPaused();
		super.onPause();
	}

	@Override
	protected void onResume() {
	    App.activityActivated();
		super.onResume();
	}
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		inflater = getLayoutInflater();

		ScrollView sw = new ScrollView(this);
		ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(16, 0, 16, 0);
		ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

		if (getIntent().getStringExtra("id") != null) {
			room = App.getRoom(getIntent().getStringExtra("id"));	
			
			
			// when the app has been terminated due to inactivity, the intent is started without extras, thus without a room id. 
			// In that case go back to the secure room list activity 
			if (room == null) {
				Intent listIntent = new Intent(this, RoomListActivity.class);
				startActivity(listIntent);
			}
			
			setTitle(room.getId());
			establishObservers();

			for (Device device : room.getDevices().values()) {
				addViewForDevice(device);
			}
		}

		sw.addView(ll);
		setContentView(sw);
	}

	// protected void onPause(){
	// super.onPause();
	// clearObservers();
	// }
	//
	// protected void onResume() {
	// super.onResume();
	// establishObservers();
	// }

	public void addViewForDevice(Device device) {
		LinearLayout deviceLayout = new LinearLayout(this);
		deviceLayout.setOrientation(LinearLayout.VERTICAL);
		deviceLayout.setPadding(16, 0, 16, 0);
		deviceLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

		View headerView = inflater.inflate(R.layout.room_detail_device_heading, null);
		TextView tv = (TextView) headerView.findViewById(R.id.device_heading_textview);
		tv.setText(device.toString());

		deviceLayout.addView(headerView);

		int i = 0;
		int size = device.getControls().values().size();
		for (Control control : device.getControls().values()) {

			View controlView = getControlView(control);

			// Hide divider of last control
			if (i++ == size) {
				controlView.findViewById(R.id.list_divider).setVisibility(View.INVISIBLE);
			}

			deviceLayout.addView(controlView);
		}
		deviceViews.put(device.getId(), deviceLayout);
		ll.addView(deviceLayout);
	}

	public void removeViewForDecice(Device device) {
		ll.removeView(deviceViews.get(device.getId()));
		deviceViews.remove(device.getId());
		// deviceViews.get(device.getId()).setVisibility(View.VISIBLE);

	}

	public View getControlView(Control control) {
		if (control.getType().equals("switch")) {
			return switchView(control);
		} else if (control.getType().equals("range")) {
			return rangeView(control);
		} else if (control.getType().equals("text")) {
			return textView(control);
		} else {
			return unknownView(control);
		}
	}

	public View rangeView(final Control control) {
		View view = inflater.inflate(R.layout.room_detail_child_item_range, null);
		TextView name = (TextView) view.findViewById(R.id.controlName_range);
		final SeekBar value = (SeekBar) view.findViewById(R.id.controlValue_range);

		name.setText(control.getId());

		ValueChangedObserver o = new ValueChangedObserver() {
			@Override
			public void onValueChange(String valueStr) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						value.setProgress(Math.round(Float.parseFloat(control.getValue())));
					}
				});
			}
		};
		control.setValueChangedObserver(o);
		openObservers.add(o);

		value.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					String payload = Integer.toString(progress);
					App.publishMqtt(control.getTopic(), payload);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		value.setMax(255);
		value.setProgress(Math.round(Float.parseFloat(control.getValue())));

		return view;
	}

	public View switchView(final Control control) {
		View view = inflater.inflate(R.layout.room_detail_child_item_switch, null);
		TextView name = (TextView) view.findViewById(R.id.controlName_switch);
		final Switch value = (Switch) view.findViewById(R.id.controlValue_switch);

		name.setText(control.getId());
		value.setChecked(control.getValue().equals("1"));

		control.setValueChangedObserver(new ValueChangedObserver() {
			@Override
			public void onValueChange(String valueStr) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						value.setChecked(control.getValue().equals("1"));
					}
				});
			}
		});

		value.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String payload = control.getValue().equals("1") ? "0" : "1";
				App.publishMqtt(control.getTopic(), payload);
			}
		});

		return view;
	}

	public View textView(final Control control) {
		View view = inflater.inflate(R.layout.room_detail_child_item_text, null);
		final TextView name = (TextView) view.findViewById(R.id.controlName_text);
		final TextView value = (TextView) view.findViewById(R.id.controlValue_text);

		control.setValueChangedObserver(new ValueChangedObserver() {
			@Override
			public void onValueChange(String valueStr) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						name.setText(control.getId());
						value.setText(control.getValue());
					}
				});
			}
		});

		name.setText(control.getId());
		value.setText(control.getValue());

		return view;
	}

	public View unknownView(final Control control) {
		View view = inflater.inflate(R.layout.room_detail_child_item_unknown, null);
		final TextView name = (TextView) view.findViewById(R.id.controlName_unknown);

		control.setValueChangedObserver(new ValueChangedObserver() {
			@Override
			public void onValueChange(String valueStr) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						name.setText(control.getId());
					}
				});
			}
		});

		name.setText(control.getId());

		return view;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				NavUtils.navigateUpTo(this, new Intent(this, RoomListActivity.class));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void establishObservers() {
		deviceAddedToRoomReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getStringExtra("roomID").equals(room.getId())) {
					addViewForDevice(App.getDevice(intent.getStringExtra("deviceID")));
				}
			}
		};

		IntentFilter filter1 = new IntentFilter();
		filter1.addAction(App.DEVICE_ADDED_TO_ROOM);
		registerReceiver(deviceAddedToRoomReceiver, filter1);

		deviceRemovedFromRoomReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getStringExtra("roomID").equals(room.getId())) {
					removeViewForDecice(App.getDevice(intent.getStringExtra("deviceID")));
				}
			}
		};
		IntentFilter filter2 = new IntentFilter();
		filter2.addAction(App.DEVICE_REMOVED_FROM_ROOM);
		registerReceiver(deviceRemovedFromRoomReceiver, filter2);

		mqttConnectivityChangedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				 Log.v(this.toString(), "Setting controls enabled accoring to new connectivity state");
				 boolean enabled = App.getState() == App.MQTT_CONNECTIVITY_CONNECTED;
				 for (View view : deviceViews.values()) {
					view.setEnabled(enabled);
				 }
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(App.MQTT_CONNECTIVITY_CHANGED);
		registerReceiver(mqttConnectivityChangedReceiver, filter);

	}

	private void clearObservers() {
		if (room != null) {
			for (Device device : room.getDevices().values()) {
				for (Control control : device.getControls().values()) {
					control.removeValueChangedObserver();
				}
			}
			unregisterReceiver(deviceAddedToRoomReceiver);
			unregisterReceiver(deviceRemovedFromRoomReceiver);
			unregisterReceiver(mqttConnectivityChangedReceiver);
		}
	}

}
