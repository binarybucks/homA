package st.alr.homA.support;

import java.util.ArrayList;

import st.alr.homA.model.Device;
import st.alr.homA.model.Quickpublish;
import st.alr.homA.model.Room;
import st.alr.homA.services.ServiceMqtt;
public class Events {
    



    public static class RoomsCleared {

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

    public static class QuickpublishNotificationRemoved {
        Quickpublish q;
        public QuickpublishNotificationRemoved(Quickpublish q) {
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

    public static class StateChanged {
        public static class ServiceMqtt {
            private Defaults.State.ServiceMqtt state;
            private Object extra;
            
            public ServiceMqtt(Defaults.State.ServiceMqtt state) {
               this(state, null);
            }
            
            public ServiceMqtt(Defaults.State.ServiceMqtt state, Object extra) {
                this.state = state;
                this.extra = extra;
            }
            public Defaults.State.ServiceMqtt getState() {
                return this.state;
            }
            public Object getExtra() {
                return extra;
            }
            
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
