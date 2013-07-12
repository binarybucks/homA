package st.alr.homA.support;

import java.util.ArrayList;

import st.alr.homA.model.Device;
import st.alr.homA.model.Quickpublish;
import st.alr.homA.model.Room;
import st.alr.homA.services.ServiceMqtt;

public class Events {
    public class QuickpublishChanged {

    }

    public static class RoomsCleared {

    }

    public static class DeviceRenamed {
        Device device;

        public DeviceRenamed(Device device) {
            this.device = device;
        }

        public Device getDevice() {
            return this.device;
        }
    }
    
    public static class QuickpublishNotificationAdded {
        Quickpublish q;
        public QuickpublishNotificationAdded(Quickpublish q) {
            this.q = q;
        }
        public Quickpublish getQuickpublish() {
            return q;
        }
    }

    public static class QuickpublishNotificationChanged {
        Quickpublish q;
        public QuickpublishNotificationChanged(Quickpublish q) {
            this.q = q;
        }
        public Quickpublish getQuickpublishes() {
            return q;
        }
    }

    
    public static class MqttConnectivityChanged {
		private ServiceMqtt.MQTT_CONNECTIVITY connectivity;

		public MqttConnectivityChanged(
				ServiceMqtt.MQTT_CONNECTIVITY connectivity) {
			this.connectivity = connectivity;
		}

		public ServiceMqtt.MQTT_CONNECTIVITY getConnectivity() {
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
