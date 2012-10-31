$(function($){


  /*
   *
   *  MODELS
   *
   */

  var Control = Backbone.Model.extend({
    defaults: function() {
      return {
        value: null,
        type: null              
      };
    },

    initialize: function() {
    },
  });

  var Device = Backbone.Model.extend({
    defaults: function() {
      return {
        room: "unassigned",
        name: ""
      };
    },

    initialize: function() {
      this.controls = new ControlCollection;
    },
  });


  var Room = Backbone.Model.extend({
    initialize: function() {
      this.devices = new DeviceCollection;
    },
  });


  var DeviceCollection = Backbone.Collection.extend({
    model: Device,
  });

  var RoomCollection = Backbone.Collection.extend({
    model: Room,
  });

  var ControlCollection = Backbone.Collection.extend({
    model: Control,
  });






  /*
   *
   *  VIEWS
   *
   */

   var RoomMenuView = Backbone.View.extend({
    className: "room-menuitem", 
    tagName: "li",
    template: $("#room-navigation-template").html(),

    initialize: function() {
      this.model.menuView = this; 
    },

    render: function () {
        var tmpl = _.template(this.template);
        this.$el.html(tmpl(this.model.toJSON()));
        return this;
    },


   });

  var RoomView = Backbone.View.extend({
    template: $("#room-template").html(),
    navigationTemplate: $("#room-navigation-template").html(), 
    className: "room", 

    initialize: function() {
      this.model.devices.on('add', this.addDevice, this);
      this.model.devices.on('remove', this.removeDevice, this);

      this.model.bind('remove', this.remove, this);
      this.model.view = this;
    },

    // render: function() {
    //   this.$el.html(this.model.get('id')+":"+this.model.get('value'));
    //   return this;
    // },

    render: function () {
        var tmpl = _.template(this.template);
        this.$el.html(tmpl(this.model.toJSON()));
        return this;
    },

    addDevice: function(device) {
      var deviceView = new DeviceView({model: device});
      this.$(".devices").append(deviceView.render().el);
    },

    removeDevice: function(device) {
      console.log("removing device from room: "+ device.get('id') + " " + this.model.get('id'))
      console.log()
      $(device.view.el).unbind();
      $(device.view.el).remove();

      if (this.model.devices.length == 0) {
        console.log("Room is empty, removing it");
        Rooms.remove(this.model);
      }
    },


  });


  var ControlView = Backbone.View.extend({
    className: "control", 

    initialize: function() {
      this.model.on('change', this.render, this);
      this.model.view = this;
    },

    render: function() {
      this.$el.html(this.model.get('id')+":"+this.model.get('value'));
      return this;
    },

  });

  var DeviceView = Backbone.View.extend({
    template: $("#device-template").html(),
    className: "device", 

    initialize: function() {
      this.model.on('change', this.render, this);
      this.model.on('destroy', this.remove, this);
      this.model.controls.on('add', this.addControl, this);

      this.model.view = this;
    },  

    render: function() {
      var tmpl = _.template(this.template);
      this.$el.html(tmpl(this.model.toJSON()));
      return this;
    },

    addControl: function(control) {
      var controlView = new ControlView({model: control});
      this.$(".controls").append(controlView.render().el);
    },
  });



  var AppView = Backbone.View.extend({
    el: $("#container"),
    initialize: function() {
       Rooms.on('add', this.addRoom, this);
       Rooms.on('remove', this.removeRoom, this);

    },
    addRoom: function(room) {
      console.log("new room added: " + room.get('id'))
      var roomView = new RoomView({model: room});
      var roomMenuView = new RoomMenuView({model: room});

      this.$("#rooms").append(roomView.render().el);
      $('#room-navigation').append(roomMenuView.render().el);

    },

    removeRoom: function(room) {
      console.log("removeRoom");
      $(room.view.el).unbind();
      $(room.view.el).remove();

      $(room.menuView.el).unbind();
      $(room.menuView.el).remove();
    },


  });


  /*
   *
   *  BASE APPLICATION LOGIC & MQTT EVENT HANDLING
   *
   */

  var Router = Backbone.Router.extend({
  routes: {
    "": "index",
    "/:room": "room"
  },

  index: function() {
    var tutorial = new Example.Views.Tutorial();

    // Attach the tutorial page to the DOM
    tutorial.render(function(el) {
      $("#main").html(el);
    });
  },

  room: function() {
    var search = new Example.Views.Search();

    // Attach the search page to the DOM
    search.render(function(el) {
      $("#main").html(el);
    });
  }
});


  var mqttSocket = new Mosquitto();
  var Devices = new DeviceCollection;
  var Rooms = new RoomCollection;


  var App = new AppView;
    mqttSocket.onconnect = function(rc){
      console.log("Connection established");
      mqttSocket.subscribe('/devices/#', 0);
    };

    mqttSocket.ondisconnect = function(rc){ 
      console.log("Connection terminated");
    };

    mqttSocket.onmessage = function(topic, payload, qos){

      console.log("-----------RECEIVED-----------");
      console.log("Received: "+topic+":"+payload);    

      var splitTopic = topic.split("/");

      // Ensure the device for the exists
      var deviceId = splitTopic[2]
      var device = Devices.get(deviceId);
      if (device == null) {
        device = new Device({id: deviceId});
        Devices.add(device);


          var room = Rooms.get(device.get('room'));
          if (room == null) {
            room = new Room({id: device.get('room')});     
            Rooms.add(room);   
          } 
          room.devices.add(device);
      }

      // Topic parsing
      if(splitTopic[3] == "controls") {
        var controlName = splitTopic[4];
        var control = device.controls.get(controlName);
        if (control == null) {
          control = new Control({id: controlName});
          device.controls.add(control);
        }

        if(splitTopic[5] == null) { // Control value
          console.log("Control value for "+ controlName+ " : " + payload);
          control.set("value", payload);
        } else { // Control type 
          console.log("Control type for "+ controlName+ " : " + payload);
          control.set("type", payload);
        } 
      } else if(splitTopic[3] == "meta" ) { 

        if (splitTopic[4] == "room") {
          console.log("looking up room of device:" + device.get('room'));
          var room = Rooms.get(device.get('room'));       


          if (room.get('id') != payload) {

            var newRoom = Rooms.get(payload);
            if (newRoom == null) {
              device.set('room', payload);
              newRoom = new Room({id: payload});  
              console.log("New room created: " + newRoom.get('id'));
              Rooms.add(newRoom);   
            } 
            room.devices.remove(device);
            newRoom.devices.add(device);
          }
        } else if(splitTopic[4] == "name") {
          device.set('name', payload);
        }
          device.set(splitTopic[4], payload);
      }
      console.log("-----------/ RECEIVED-----------");
    };

    mqttSocket.connect("ws://192.168.8.2/mqtt");
});
