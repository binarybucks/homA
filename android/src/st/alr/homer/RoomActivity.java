//package st.alr.homer;
//
//import android.os.Bundle;
//import android.app.Activity;
//import android.content.Intent;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.support.v4.app.NavUtils;
//
//public class RoomActivity extends Activity {
//
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_room);
//		// Show the Up button in the action bar.
//		getActionBar().setDisplayHomeAsUpEnabled(true);
//	}
//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.activity_room, menu);
//		return true;
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//	    case R.id.menu_settings:
//	    	Intent intent = new Intent(RoomActivity.this, SettingsActivity.class);
//	        startActivity(intent);
//	       return true;	
//		case android.R.id.home:
//			// This ID represents the Home or Up button. In the case of this
//			// activity, the Up button is shown. Use NavUtils to allow users
//			// to navigate up one level in the application structure. For
//			// more details, see the Navigation pattern on Android Design:
//			//
//			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
//			//
//			NavUtils.navigateUpFromSameTask(this);
//			return true;
//		}
//		return super.onOptionsItemSelected(item);
//	}
//	
//
//}
//
//<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
//xmlns:tools="http://schemas.android.com/tools"
//android:layout_width="match_parent"
//android:layout_height="match_parent"
//tools:context=".RoomActivity" >
//
//<TextView
//    android:layout_width="wrap_content"
//    android:layout_height="wrap_content"
//    android:layout_centerHorizontal="true"
//    android:layout_centerVertical="true"
//    android:text="@string/placeholder" />
//
////</RelativeLayout>
