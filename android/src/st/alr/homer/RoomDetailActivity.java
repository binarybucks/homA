package st.alr.homer;

import st.alr.homer.Control;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/**
 * An activity representing a single Room detail screen. This activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link RoomListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link RoomDetailFragment}.
 */
public class RoomDetailActivity extends Activity {
	LayoutInflater inflater;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		inflater = getLayoutInflater();
		
		ScrollView sw = new ScrollView(this);
		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(16, 0, 16, 0);
		ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));

		if (getIntent().getStringExtra("id") != null) {
			Room room = ((App) getApplicationContext()).getRooms().get(getIntent().getStringExtra("id"));
			setTitle(room.getId());

			for (Device device : room.getDevices().values()) {
				View headerView = inflater.inflate(R.layout.room_detail_device_heading, null);
				TextView tv = (TextView) headerView.findViewById(R.id.device_heading_textview);
				tv.setText(device.toString());

				ll.addView(headerView);

				int i = 0;
				int size = device.getControls().values().size();
				for (Control control : device.getControls().values()) {
					
					
					View controlView = getControlView(control);
					
					// Hide divider of last control
					if (i++ == size) {
						((View)controlView.findViewById(R.id.list_divider)).setVisibility(4);
					}
					
					ll.addView(controlView);		
				}
			}
		}

		sw.addView(ll);
		setContentView(sw);
	}

	public View getControlView(Control control) {
		if(control.getType().equals("switch")) {
			return switchView(control);
		} else if (control.getType().equals("range")){
			return rangeView(control);
		} else if (control.getType().equals("text")){
			return textView(control);
		} else {
			return unknownView(control);
		}
		
	}
	
	public View rangeView(final Control control){
		View view = inflater.inflate(R.layout.room_detail_child_item_range, null);
		TextView name = (TextView) view.findViewById(R.id.controlName);
		final SeekBar value = (SeekBar) view.findViewById(R.id.controlValue);
		
		name.setText(control.getId());
		
	    control.addValueChangedObserver(new ValueChangedObserver() {
			public void onValueChange(String valueStr) {
		            runOnUiThread(new Runnable() {
		                public void run() {
		            		value.setProgress( Math.round(Float.parseFloat(control.getValue())));
		                }
		            });
		        }				
			});
	    
		value.setMax(255);
		value.setProgress( Math.round(Float.parseFloat(control.getValue())));
		
		return view;
	}
	
	public View switchView(final Control control){
		View view = inflater.inflate(R.layout.room_detail_child_item_switch, null);
		TextView name = (TextView) view.findViewById(R.id.controlName);
		final Switch value = (Switch) view.findViewById(R.id.controlValue);
		
		name.setText(control.getId());
		value.setChecked(control.getValue().equals("1"));
		
		
	    control.addValueChangedObserver(new ValueChangedObserver() {
			public void onValueChange(String valueStr) {
		            runOnUiThread(new Runnable() {
		                public void run() {
		            		value.setChecked(control.getValue().equals("1"));
		                }
		            });
		        }				
			});
	    	        
		return view;
	}

	public View textView(final Control control){
		View view = inflater.inflate(R.layout.room_detail_child_item_text, null);
		final TextView name = (TextView) view.findViewById(R.id.controlName);
		final TextView value = (TextView) view.findViewById(R.id.controlValue);
		
		
	    control.addValueChangedObserver(new ValueChangedObserver() {
			public void onValueChange(String valueStr) {
		            runOnUiThread(new Runnable() {
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
	public View unknownView(final Control control){
		View view = inflater.inflate(R.layout.room_detail_child_item_unknown, null);
		final TextView name = (TextView) view.findViewById(R.id.controlName);
		
	    control.addValueChangedObserver(new ValueChangedObserver() {
			public void onValueChange(String valueStr) {
		            runOnUiThread(new Runnable() {
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
			NavUtils.navigateUpTo(this,new Intent(this, RoomListActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
