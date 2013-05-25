(function(){
  var mqttClient;

  Backbone.View.prototype.close = function() {
    this.off();
    this.remove();

    // For custom handlers to clean up events bound to anything else than this.model and this.collection
    if (this.onClose){ 
      this.onClose();
    }

    if (this.model) {
      this.model.off(null, null, this);
    }

    if (this.collection) {
      this.collection.off(null, null, this);
    }
  }
Backbone.View.prototype.finish = function() {/*Overwrite me*/}



  /*
   *
   *  MODELS
   *
   */

  var AppSettings = Backbone.Model.extend({

    defaults: function () {
      return {
        connectionStatus: "disconnected", 
      };
    },

   initialize: function() {
      if (!this.get("title")) { 
        this.set({"server": localStorage.getItem("homA_server") || document.location.hostname || "127.0.0.1"});
        this.set({"devMode": localStorage.getItem("homA_devMode") == 1 });
      }
    },

    sync: function() {// Overwritten to kill default sync behaviour
    },

    save: function() {
      if( this.get("server")) localStorage.setItem("homA_server", this.get("server"))
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
  });

  var Device = Backbone.Model.extend({
    defaults: function() {
      return {
        name: "",
        room: undefined
      };
    },

    initialize: function() {
      this.controls = new ControlCollection;
    },

    removeFromCurrentRoom: function() {
      if (this.get("room") != undefined && this.get("room") != null) {
        console.log("Removing device from room: " + this.get("room").id);
        this.get("room").devices.remove(this);


        if (this.get("room").devices.length == 0) {

          console.log("Room " + this.get("room").id+" is empty, removing it");
          Rooms.remove(this.get("room"));
        }


      } 
    },

    moveToRoom: function(roomName) {
      this.removeFromCurrentRoom();

      var cleanedName = roomName || "unassigned";
      var targetRoom = Rooms.get(cleanedName);

      if (targetRoom == null) {
        console.log("Room " + cleanedName +" does not exist, creating it");
        targetRoom = new Room({id: cleanedName});
        Rooms.add(targetRoom);
      } 

      targetRoom.devices.add(this);
      this.set("room", targetRoom);
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

  var ToplevelView = Backbone.View.extend({
    rerenderToplevelView: function() {
      App.renderToplevelView(this);
    },
  })

  var SettingsView = ToplevelView.extend({
    template: $("#settings-template").html(),


    events: {
      "keypress #serverInput":  "saveServerOnEnter",
    },


    saveServerOnEnter: function(e) { 
      console.log("saving");
      if (e.keyCode == 13) {
        this.model.set("server", e.target.value);
        this.model.save();
      }
    },

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



  var ViewPlaceholder = Backbone.View.extend({
    template: $("#view-placeholder-template").html(),
    className: "view", 

    initialize: function() {
      this.model.on('add', this.addModel, this);
    },

    render: function () {
        var tmpl = _.template(this.template);
       this.$el.html(tmpl(_.extend(this.model.toJSON(), {id: this.id, backText: this.options.backText, backHref: this.options.backHref})));
        return this;
    },

    addModel: function(addedObject) {
      if(addedObject.get("id") == this.id) {
        console.log("view content now available, re routing to: " + this.options.callbackRoute);
        Backbone.history.loadUrl( this.options.callbackRoute ) // Router.navigate does not work to reload route, as the hash did not change
      }
    },
  });

  var RoomDetailView = ToplevelView.extend({
    template: $("#room-detail-template").html(),
    className: "room", 

    initialize: function() {
      this.model.devices.on('add', this.addDevice, this);
      this.model.devices.on('remove', this.removeDevice, this);
      this.model.bind('remove', this.removeSelf, this);
      this.model.view = this;
    },

    onClose: function() {
      this.model.devices.off();
    },

    // As wookmark needs DOM elements to work with, this method only does things after the inital render is finished 
    // and everything has been inserted into the DOM to trigger relayouts when new cards are added.
    layoutCards: function(){},

    finish: function(){ // Called when the view has been inserted into the DOM
      this.layoutCardsOptions = {autoResize:true, container:this.$('.devices'),offset:25,itemWidth:432};
      this.layoutCards=function(){this.$('.card').wookmark(this.layoutCardsOptions);};
      this.layoutCards();
    },

    render: function () {
      var tmpl = _.template(this.template);
      this.$el.html(tmpl(this.model.toJSON()));
      for (var i = 0, l = this.model.devices.length; i < l; i++)
          this.addDevice(this.model.devices.models[i]);
      
      return this;
    },

    addDevice: function(device) {
      var deviceView = new DeviceView({model: device});
      this.$(".devices").append(deviceView.render().el);
      this.layoutCards(); 
    },

    removeDevice: function(device) {
      console.log("Removing device from room: "+ device.get('id') + " " + this.model.get('id'))
      device.view.close();
      if (this.model.devices.length == 0) {
        console.log("Room is empty, removing it");
        Rooms.remove(this.model);
      }
    },

    removeSelf: function(room) {
      Backbone.history.loadUrl( "rooms/"+room.get("id") ) // Router.navigate will not work here to reload the route, as the hash did not change
    }
  });



  var ControlView = Backbone.View.extend({
    className: "control",

    events: {
      "click input[type=checkbox]" : "inputValueChanged",
      "change input[type=range]" : "inputValueChanged",
      "mousedown input[type=range]" : "inhibitInputUpdates",
      "mouseup input[type=range]" : "allowInputUpdates"
    },

    initialize: function() {
      this.model.on('change:type', this.modelTypeChanged, this);  
      this.model.on('change:value', this.modelValueChanged, this);  

      this.specialize();
      this.model.view = this;
      this.allowInputUpdates();
    },

    specialize: function() {
      this.dynamicInputValueChanged = this.methodNotImplemented;
      this.dynamicRender = this.methodNotImplemented;
      this.dynamicInhibitInputUpdates = this.methodNotImplemented;
      this.dynamicAllowInputUpdates = this.methodNotImplemented;
      this.dynamicModelValueChanged = this.methodNotImplemented;

      if (this.model.get("type") == "switch") {
        this.dynamicRender = this.switchRender;
        this.dynamicInputValueChanged = this.switchInputValueChanged;
        this.dynamicModelValueChanged = this.switchModelValueChanged;
      } else if (this.model.get("type") == "range") {
        this.dynamicRender = this.rangeRender;
        this.dynamicInputValueChanged = this.rangeInputValueChanged;
        this.dynamicModelValueChanged = this.rangeModelValueChanged;
        this.dynamicInhibitInputUpdates = this.rangeInhibitInputUpdates;
        this.dynamicAllowInputUpdates = this.rangeAllowInputUpdates;
        this.dynamicAllowInputUpdates();

      } else if (this.model.get("type") == "text") {
        this.dynamicRender = this.textRender;
        this.dynamicModelValueChanged = this.textModelValueChanged;
      } else {
        this.dynamicRender = this.undefinedRender;
      }
    },

    // Wrapper methods
    render: function() {
      return this.dynamicRender();
    },

    inputValueChanged: function(event) {
      this.dynamicInputValueChanged(event);
    },
    
    modelValueChanged: function(model) {
      this.dynamicModelValueChanged(model);
    },

    modelTypeChanged: function() {
      this.specialize();
      this.render();
    },

    inhibitInputUpdates: function(event) {
      this.dynamicInhibitInputUpdates(event);
    },

    allowInputUpdates: function(event) {
      this.dynamicAllowInputUpdates(event);
    },




    // Specialized methods for type range
    rangeRender: function() {
      var tmpl = this.templateByType("range");
      this.$el.html(tmpl(this.model.toJSON()));
      this.input = this.$('input');
      this.input.attr('max', this.model.get("max") || 255)
      return this;
    },

    rangeInhibitInputUpdates: function(event) {
      this.allowUpdates = false; 
    },    

    rangeAllowInputUpdates: function(event) {
      this.allowUpdates = true; 
    },




    rangeInputValueChanged: function(event) {
      App.publishMqtt(this.model.get("topic"), event.target.value);
    },


    rangeModelValueChanged: function(model) {
      if (this.allowUpdates) {
        this.render();
      }
    },

    // Specialized methods for type switch
    switchRender: function() {
      var tmpl = this.templateByType("switch");
      this.$el.html(tmpl(_.extend(this.model.toJSON(), {checkedAttribute: this.model.get("value") == 1 ? "checked=\"true\"" : ""})));
      this.input = this.$('input');
      return this;
    },


    switchInputValueChanged: function(event) {
      App.publishMqtt(this.model.get("topic"), event.target.checked == 0 ? "0" : "1");
    },

    switchModelValueChanged: function(model) {
      this.render();
    },

    // Specialized methods for type (readonly)
    textRender: function() {
      var tmpl = this.templateByType("text");
      this.$el.html(tmpl(this.model.toJSON()));
      return this;
    },

    textModelValueChanged: function(model) {
      this.render();
    },

    // Specialized methods for type undefined
    undefinedRender: function() {
      var tmpl = this.templateByType("undefined");
      this.$el.html(tmpl(this.model.toJSON()));
      return this;
    },


    // Helper methods
    methodNotImplemented: function() {
    },

    templateByType: function(type) {
      return _.template($("#" + type +"-control-template").html());
    },

 });


  var DeviceSettingsView = ToplevelView.extend({
    template: $("#device-settings-template").html(),
    className: "device-settings",


    events: {
      "keypress #nameInput"  : "publishNameInputOnEnter",
      "keypress #roomInput"  : "publishRoomInputOnEnter",
    },

    initialize: function() {
      this.model.on('change', this.rerenderToplevelView, this);
      this.model.view = this;
    },  


    render: function() {
      var tmpl = _.template(this.template);
      var roomName = this.model.get("room") != undefined ? this.model.get("room").get("id") : "unassigned"
      this.$el.html(tmpl(_.extend( this.model.toJSON(), {roomname: roomName, rooms: Rooms})));
      this.delegateEvents();
      return this;
    },

    publishValue: function(e, type) {
      var value = e.target.value;
      mqttClient.publish("/devices/"+this.model.get("id")+"/meta/"+type, value ? value : "", 0, true);
    },

    publishNameInput: function(e) {
      this.publishValue(e, "name");
    },

    publishRoomInput: function(e) {
      this.publishValue(e, "room");
    },

    publishNameInputOnEnter: function(e) { // enter in nameInput
      if (e.keyCode == 13) this.publishNameInput(e);
    },
    publishRoomInputOnEnter: function(e) { // enter in nameInput
      if (e.keyCode == 13) this.publishRoomInput(e);
    },
  });

  var DeviceView = Backbone.View.extend({
    template: $("#device-template").html(),
    className: "device card", 

    initialize: function() {
      this.model.on('change', this.render, this);
      this.model.on('destroy', this.remove, this);
      this.model.controls.on('add', this.addControl, this);
      this.model.controls.on('remove', this.render, this);
      this.model.view = this;
    },  

    render: function() {
      var tmpl = _.template(this.template);
      this.$el.html(tmpl(this.model.toJSON()));
        for (var i = 0, l = this.model.controls.length; i < l; i++) {
            this.addControl(this.model.controls.models[i]);
        }

      return this;
    },

    addControl: function(control) {
      var controlView = new ControlView({model: control});
      this.$(".controls").append(controlView.render().el);
      // controlView.positionRangeBackgroundImage(control.get("value"));
    },



  });



  // Manages view transition in the #container element
  var AppView = Backbone.View.extend({
    el: $("#container"),

    initialize: function() {
    },

    showView: function(view) {
      if (this.currentView){
        this.currentView.close();
      }

      this.currentView = view;
      this.renderToplevelView(this.currentView);
    },

    renderToplevelView: function(view) {
      this.$el.html(view.render().$el);
      view.delegateEvents();
      view.finish();
    },

    publishMqtt: function(topic, value) {
      message = new Messaging.Message(value);
      message.destinationName = topic+"/on";
      message.retained = true;
      mqttClient.send(message); 
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

  index: function() {
    //TODO
    //var roomListView = new RoomListView({model: Rooms});
    //App.showView(roomListView);
  },

  settings: function () {
    var settingsView = new SettingsView({model: Settings});
    App.showView(settingsView);
  },

  deviceSettings: function(id) {
    var device = Devices.get(id); // Room might not yet exists
    var view; 
    if (device == null) {
      view = new ViewPlaceholder({model: Devices, id: id, backText: "Home", backHref: '#', callbackRoute: Backbone.history.fragment});
    } else {
      view = new DeviceSettingsView({model: device});
    }
    App.showView(view);

  },


  room: function(id) {
    var room = Rooms.get(id); // Room might not yet exists
    var view; 
    if (room == null) {
      view = new ViewPlaceholder({model: Rooms, id: id, backText: 'Rooms', backHref: '#', callbackRoute: Backbone.history.fragment});
    } else {
      view = new RoomDetailView({model: room});
    }
    App.showView(view);

   },




});



  mqttOnConnected = function(response){
    console.log("Connection established");
    Settings.set("connectionStatus", "connected");

    mqttClient.subscribe('/devices/+/controls/+/meta/+', 0);
    mqttClient.subscribe('/devices/+/controls/+', 0);
    mqttClient.subscribe('/devices/+/meta/#', 0);
    window.onbeforeunload = function (){mqttClient.disconnect();};
  };

  mqttOnConnectionLost = function(response){ 
    if (response.errorCode !== 0)
      console.log("onConnectionLost:"+responseObject.errorMessage);

    Settings.set("connectionStatus", "disconnected");
    console.log("Connection terminated");
    setTimeout(function () {mqttConnect();}, 5000);
  };

  mqttOnMessageArrived = function(message){
    var payload = message.payloadString;
    var topic = message.destinationName.split("/");
    //console.log("-----------RECEIVED-----------");
    //console.log("Received: "+topic+":"+payload);    


    // Ensure the device for the message exists
    var deviceId = topic[2]
    var device = Devices.get(deviceId);
    if (device == null && payload != "") {
      device = new Device({id: deviceId});
      Devices.add(device);
      device.moveToRoom(undefined);
    } else if (device != null && payload == "") {   
      // Remove the device when any received topic under that device tree is "" (actually this also removes the device when just a control should be removed, but 
      // right now there is no usecase to remove a single control only.)
      // This just removes the device when the webinterface is loaded and a "" is received. This does not ensure persistent removal during reloads. 
      // It's the devices job to ensure that all of its values are correctly unpublished 
      device.removeFromCurrentRoom();
      Devices.remove(deviceId);
      return;
    }

    // Topic parsing
    //  /devices/$uniqueDeviceId/controls/$deviceUniqueControlId/meta/type
    // 0/      1/              2/       3/                     4/   5/   6
    if(topic[3] == "controls") {
      var controlName = topic[4];  
      var control = device.controls.get(controlName);
      if (control == null) {
        control = new Control({id: controlName});
        device.controls.add(control);
        control.set("topic", "/devices/"+ deviceId + "/controls/" + controlName);
      }

      if(topic[5] == null) {                                       // Control value   
        control.set("value", payload);
      } else if (topic[5] == "meta" && topic[6] != null){     // Control meta 
        control.set(topic[6], payload);
      } 
    } else if(topic[3] == "meta" ) { // Could be moved to the setter to facilitate parsing
      if (topic[4] == "room") {                                    // Device Room
        device.moveToRoom(payload);
      } else if(topic[4] == "name") {                              // Device name
        device.set('name', payload);
      }
    }
   //console.log("-----------/ RECEIVED-----------");
  };

  function mqttConnect() {
    console.log("Connecting")
    mqttClient = new Messaging.Client(Settings.get("server"), Number(1337), "homa-webinterface");
    mqttClient.onConnectionLost = mqttOnConnectionLost;
    mqttClient.onMessageArrived = mqttOnMessageArrived;
    mqttClient.connect({onSuccess:mqttOnConnected});
  }

  var Settings = new AppSettings;
  Settings.fetch();
  var Devices = new DeviceCollection;
  var Rooms = new RoomCollection;
  var App = new AppView;

  var Router = new ApplicationRouter;
  Backbone.history.start({pushState : false});
  mqttConnect();
})();
