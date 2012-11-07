$(function(){
  var mqttSocket = new Mosquitto();


  // Backbone.View.prototype.close = function(){
  //   this.remove();
  //   this.unbind();
  //   if (this.onClose){ // Provide custom functionality to remove event bindings in subclasses
  //     this.onClose();
  //   }
  // }

  Backbone.View.prototype.close = function() {
    this.remove();
    this.unbind(); // alias of this.off();
    if (this.model) {
      this.model.off(null, null, this);
    }
    if (this.collection) {
      this.collection.off(null, null, this);
    }
  }


  /*
   *
   *  MODELS
   *
   */

  var Status = Backbone.Model.extend({
    defaults: function () {
      return {
        connectionStatus: "disconnected", 
        broker: "ws://127.0.0.1/mqtt" 
      };
    },
  });


  var Control = Backbone.Model.extend({
    defaults: function() {
      return {
        value: 0,
        type: "undefined",
        topic: null              
      };
    },
  
    initialize: function() {
      _.bindAll(this, 'toExtendedJSON', 'checkedAttribute', 'valueAttribute');
    },

    // provide custom functionality to format values
    toExtendedJSON: function(){
      var json = _.extend( this.toJSON(), { 
                                            checkedAttribute: this.checkedAttribute(), 
                                            valueAttribute: this.valueAttribute()
                                          });
      return json;
    },

    checkedAttribute: function() {
      return this.get("value") == 1 ? "checked=\"true\"" : "";
    },

    valueAttribute: function(){
      return "value=\""+ this.get("value")+"\"";
    },


  });

  var Device = Backbone.Model.extend({
    defaults: function() {
      return {
        name: ""
      };
    },

    initialize: function() {
      this.controls = new ControlCollection;
    },

    removeFromCurrentRoom: function() {
      if (this.room != undefined && this.room != null) {
        console.log("device has room, removing from it");
        this.room.devices.remove(this);


        if (this.room.devices.length == 0) {

          console.log("Room is empty, removing it");
          Rooms.remove(this.room);
        }


      } 
    },




    moveToRoom: function(roomName) {
      var cleanedName = roomName || "unassigned";
      this.removeFromCurrentRoom();

      var room = Rooms.get(cleanedName);


      if (room == null) {
                console.log("room does not exist");

        Rooms.add(new Room({id: cleanedName}));
                        console.log("recursing");

        this.moveToRoom(cleanedName);
      } else {
                        console.log("room exists");

        room.devices.add(this);
                        console.log("device added to room");

        this.room = room;
                        console.log("room set as this devices room");

      } 
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

  var SettingsView = Backbone.View.extend({
    template: $("#settings-template").html(),

    initialize: function() {
      this.model.view = this; 
      this.model.on('change', this.render, this);
    },


    render: function () {
        var tmpl = _.template(this.template);
        this.$el.html(tmpl(this.model.toJSON()));
        return this;
    },
   });




  var RoomListView = Backbone.View.extend({
    idName: "room-list", 
    tagName: "div",
    template: $("#room-list-template").html(),

    initialize: function() {
        this.model.on('add', this.addRoom, this);
        this.model.on('remove', this.removeRoom, this);
        _.bindAll(this, 'addRoom', 'removeRoom', 'render');

    },

    addRoom: function(room) {
      console.log("Room added: " + room.get("id"));
      var detailViewLink = new RoomDetailLinkView({model: room});
      this.$('#room-detail-links').append(detailViewLink.render().$el);
    },

    render: function () {
        var tmpl = _.template(this.template);
        this.$el.html(tmpl());

        // According to http://jsperf.com/backbone-js-collection-iteration for iteration with collection.models is pretty fast
        console.log("number of rooms: " + this.model.length);
        for (var i = 0, l = this.model.length; i < l; i++) {
            this.addRoom(this.model.models[i]);
        }

        return this;
    },
    removeRoom: function(room) {
      console.log("room removed, removing detail link view");
       room.detailViewLink.close();
    },



  });



   var RoomDetailLinkView = Backbone.View.extend({
    className: "room-detail-link", 
    tagName: "li",
    template: $("#room-detail-link-template").html(),

    initialize: function() {
      this.model.detailViewLink = this; 

    },

    render: function () {
        var tmpl = _.template(this.template);
        this.$el.html(tmpl(this.model.toJSON()));
        return this;
    },

 


   });


  var RoomDetailViewPlaceholder = Backbone.View.extend({
    template: $("#room-detail-placeholder-template").html(),
    className: "room", 

    initialize: function() {
      this.model.on('add', this.addRoom, this);
    },

    render: function () {
        var tmpl = _.template(this.template);
          this.$el.html(tmpl({id: this.id}));
        return this;
    },

    addRoom: function(room) {
      if(room.get("id") == this.id) {
        Router.room(this.id);
      }
    },
  });



  var RoomDetailView = Backbone.View.extend({
    template: $("#room-detail-template").html(),
    className: "room", 

    initialize: function() {
      this.model.devices.on('add', this.addDevice, this);
      this.model.devices.on('remove', this.removeDevice, this);

      this.model.bind('remove', this.remove, this);
      this.model.view = this;
    },

    // render: function() {

    render: function () {
        var tmpl = _.template(this.template);
        this.$el.html(tmpl(this.model.toJSON()));
        for (var i = 0, l = this.model.devices.length; i < l; i++) {
            this.addDevice(this.model.devices.models[i]);
        }

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

    events: {
      "click input[type=checkbox]":  "checkboxChanged",
      "click input[type=range]":  "rangeChanged"
    },

     initialize: function() {
      _.bindAll(this, 'checkboxChanged');
      this.model.on('change', this.render, this);
      this.model.view = this;
    },

    render: function() {
      var tmpl = _.template($("#" + this.model.get("type") +"-control-template").html());
      this.$el.html(tmpl(this.model.toExtendedJSON()));
      return this;
    },




    rangeChanged: function(event) {
      console.log(event);
    },

    checkboxChanged: function(event) {
      mqttSocket.publish(this.model.get("topic"), event.srcElement.checked == 0 ? "0" : "1", 0, true);
    }

  });


  var DeviceSettingsView = Backbone.View.extend({
    template: $("#device-template").html(),
    className: "device-settings",

    initialize: function() {
      this.model.on('change', this.render, this);
      this.model.view = this;
    },  

    render: function() {
      var tmpl = _.template(this.template);
     
      var json = _.extend( this.toJSON(), { 
                                            checkedAttribute: this.checkedAttribute(), 
                                            valueAttribute: this.valueAttribute()
                                          });

       this.$el.html(tmpl(_.extend( this.model.toJSON(), {roomname: this.model.get(room).get("id")})));
      return this;
    },

  });

  var DeviceView = Backbone.View.extend({
    template: $("#device-template").html(),
    className: "device", 


    events: {
      "dblclick h1":  "edit",
      "keypress .edit"  : "updateOnEnter",
      "blur .edit"      : "close"

    },


    initialize: function() {
      console.log("new DeviceView created for: " + this.model.id);
      this.model.on('change', this.render, this);
      this.model.on('destroy', this.remove, this);
      this.model.controls.on('add', this.addControl, this);
      console.log("input");
      console.log(this.input);
      this.model.view = this;
    },  

    render: function() {
      var tmpl = _.template(this.template);
      this.$el.html(tmpl(this.model.toJSON()));
        for (var i = 0, l = this.model.controls.length; i < l; i++) {
            this.addControl(this.model.controls.models[i]);
        }
      this.input = this.$('.edit');

      return this;
    },

    addControl: function(control) {
      var controlView = new ControlView({model: control});
      this.$(".controls").append(controlView.render().el);
    },

// Switch this view into `"editing"` mode, displaying the input field.
    edit: function() {
      this.$el.addClass("editing");
      this.input.focus();
    },

    // Close the `"editing"` mode, saving changes to the todo.
    close: function() {
      var value = this.input.val();
      if (!value) {
        console.log("clearing");
        mqttSocket.publish("/devices/"+this.model.get("id")+"/meta/name", null, 0, true);
        // this.clear();
      } else {
        console.log("saving");
        // this.model.save({title: value});
        mqttSocket.publish("/devices/"+this.model.get("id")+"/meta/name", value, 0, true);

        this.$el.removeClass("editing");
      }
    },

    // If you hit `enter`, we're through editing the item.
    updateOnEnter: function(e) {
      console.log("updateOnEnter");
      if (e.keyCode == 13) this.close();
    },

    // Remove the item, destroy the model.
    clear: function() {
      console.log("clearing");
      // this.model.destroy();
    }

  });



  // Manages view transition 
  var AppView = Backbone.View.extend({
    el: $("#container"),

    showView: function(view) {
      if (this.currentView){
        this.currentView.close();
      }

      this.currentView = view;
      this.currentView.render();

      this.$el.html(this.currentView.$el);
    }

  });


  /*
   *
   *  BASE APPLICATION LOGIC & MQTT EVENT HANDLING
   *
   */

  var ApplicationRouter = Backbone.Router.extend({
  routes: {
    "settings" : "settings",
    "devices/:device/settings": "deviceSettings",
    "rooms/:room": "room",
    "": "index",
    "/": "index",


  },
  initialize: function() {console.log("Router inizalized");},

  index: function() {
    console.log("showing roomListView");

    var roomListView = new RoomListView({model: Rooms});
    App.showView(roomListView);
  },

  settings: function () {
    var settingsView = new SettingsView({model: Settings});
    App.showView(settingsView);
  },

  deviceSettings: function(deviceId) {
    console.log("device settings");
    var device = Devices.get(deviceId); // Room might not yet exists
    if (device == null) {
      console.log("device not yet loaded");
    } else {
      var deviceSettingsView = new DeviceSettingsView({model: device});
      App.showView(deviceSettingsView);
    }
  },

  room: function(id) {
    console.log("showing roomDetailView for room: " + id);
    var room = Rooms.get(id); // Room might not yet exists
    if (room == null) {
      // render "room not yet available. wait or go back to room list" view
      var roomDetailViewPlaceholder = new RoomDetailViewPlaceholder({model: Rooms, id: id});
      App.showView(roomDetailViewPlaceholder);
    } else {
      var roomDetailView = new RoomDetailView({model: room});
      App.showView(roomDetailView);
    }
   },





});



  mqttSocket.onconnect = function(rc){
    console.log("Connection established");
    // Status.set("connectionStatus", "connected");
    mqttSocket.subscribe('/devices/#', 0);
  };

  mqttSocket.ondisconnect = function(rc){ 
    // Status.set("connectionStatus", "disconnected");
    console.log("Connection terminated");
  };

  mqttSocket.onmessage = function(topic, payload, qos){

    console.log("-----------RECEIVED-----------");
    console.log("Received: "+topic+":"+payload);    
    var splitTopic = topic.split("/");

    // Ensure the device for the message exists
    var deviceId = splitTopic[2]
    var device = Devices.get(deviceId);
    if (device == null) {
      device = new Device({id: deviceId});
      Devices.add(device);
      device.moveToRoom(undefined);
    }

    // Topic parsing
    if(splitTopic[3] == "controls") {
      var controlName = splitTopic[4];  
      var control = device.controls.get(controlName);
      if (control == null) {
        control = new Control({id: controlName});
        device.controls.add(control);

        control.set("topic", topic.replace("/type", ""));

      }

      if(splitTopic[5] == null) {                                       // Control value
        control.set("value", payload);
      } else {                                                          // Control type 
        control.set("type", payload);
      } 
    } else if(splitTopic[3] == "meta" ) { 
      if (splitTopic[4] == "room") {                                    // Device Room
        device.moveToRoom(payload);
      } else if(splitTopic[4] == "name") {                              // Device name
        device.set('name', payload);
      }
      device.set(splitTopic[4], payload);
    }
    console.log("-----------/ RECEIVED-----------");
  };







  var Settings = new Status;
  var Devices = new DeviceCollection;
  var Rooms = new RoomCollection;
  var App = new AppView;

  var Router = new ApplicationRouter;
  Backbone.history.start({pushState : false});

  mqttSocket.connect("ws://192.168.8.2/mqtt");

});
