package st.alr.homA;

import java.util.HashMap;

import com.commonsware.cwac.merge.MergeAdapter;

import st.alr.homA.R;
import st.alr.homA.R.string;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;


public class Device {
	private String id;
	private String name; 
	private Room room;
	private HashMap<String, Control> controls;
	
	//ControlsHashMapAdapter controlsAdapter;
	
//	public ControlsHashMapAdapter getControlsAdapter() {
//		return controlsAdapter;
//	}

	public Room getRoom() {
		return room;
	}

	private Context context;
	
	public Device (String id, Context context) {
		this(id, null, context);
	}
	
	public Device (String id, String name, Context context) {
		this.id = id;
		this.name = name;
		this.controls = new HashMap<String, Control>();
		this.context = context;
	//	controlsAdapter = new ControlsHashMapAdapter(context, controls);
	}

	void removeFromCurrentRoom() {
		
	      if (this.room != null) {
	          this.room.removeDevice(this);
	         
	          if (this.room.getDevices().size() == 0) {
	            Log.v(this.toString(), "Room " + this.room.getId() +" is empty, removing it");
	            ((App)context.getApplicationContext()).removeRoom(this.room);
	          }
	        } 

	}
	

	void moveToRoom(String roomname) {
	    App app = (App)context.getApplicationContext(); 
	    
		String cleanedName = roomname != null && !roomname.equals("")?  roomname : context.getString(R.string.defaultRoomName);
	     
	     Room newRoom = app.getRoom(cleanedName);
	    
	   
	      if (newRoom == null) {
	    	  newRoom = new Room(context, cleanedName);
	    	  app.addRoom(newRoom);
	      } 

	      removeFromCurrentRoom();
	      newRoom.addDevice(this);
	      this.room = newRoom;
	}
	
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
	public Control getControlWithId(String id) {
		return controls.get(id);
	}
    public void addControl(Control control) {
    	this.controls.put(control.getId(), control);
    	controlsAdapterDatasourceChanged();
    }

	public HashMap<String, Control> getControls() {
		return controls;
	}

	public String toString(){
		return this.name != null  && !this.name.equals("") ? this.name : this.id;
	}
	
	public void controlsAdapterDatasourceChanged() {
	    ((App)context.getApplicationContext()).getUiThreadHandler().post(new Runnable() {
            @Override
            public void run() {
            	Log.v(this.toString(), "Datasource changeddd");
            }
          });
	}

}
