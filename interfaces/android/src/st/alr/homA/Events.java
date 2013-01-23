package st.alr.homA;

public class Events {
	public static class MqttReconnectMightBeRequired {
		
	}
	public static class MqttConnectivityChanged {
		private short connectivity;
		public MqttConnectivityChanged(short connectivity) {
			this.connectivity = connectivity;
		}
		public short getConnectivity() {
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
