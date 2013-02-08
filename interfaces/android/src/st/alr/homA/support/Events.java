package st.alr.homA.support;

import st.alr.homA.App;
import st.alr.homA.Device;
import st.alr.homA.Room;

public class Events {
	public static class MqttReconnectMightBeRequired {
		
	}
	public static class MqttConnectivityChanged {
		private App.MQTT_CONNECTIVITY connectivity;
		public MqttConnectivityChanged(App.MQTT_CONNECTIVITY connectivity) {
			this.connectivity = connectivity;
		}
		public App.MQTT_CONNECTIVITY getConnectivity() {
			return connectivity;
		}
	}
	
	public static class RoomAdded {
		Room room;
		public RoomAdded(Room room) {
			this.room = room;
		}
		public Room getRoom() {
			return this.room;
		}
	}
	
	public static class RoomRemoved {
		Room room;
		public RoomRemoved(Room room) {
			this.room = room;
		}
		public Room getRoom() {
			return this.room;
		}
	}
	public static class DeviceAdded {
		Device device;
		public DeviceAdded(Device device) {
			this.device = device;
		}
		public Device getDevice() {
			return this.device;
		}
	}
	
	public static class DeviceRemoved {
		Device device;
		public DeviceRemoved(Device device) {
			this.device = device;
		}
		public Device getDevice() {
			return this.device;
		}
	}
	
	public static class DeviceAddedToRoom {
		Device device;
		Room room;
		public DeviceAddedToRoom(Device device, Room room) {
			this.device = device;
			this.room = room;
		}
		public Device getDevice() {
			return this.device;
		}
		public Room getRoom() {
			return this.room;
		}
	}
	
	public static class DeviceRemovedFromRoom {
		Device device;
		Room room;
		public DeviceRemovedFromRoom(Device device, Room room) {
			this.device = device;
			this.room = room;
		}
		public Device getDevice() {
			return this.device;
		}
		public Room getRoom() {
			return this.room;
		}
	}
}
