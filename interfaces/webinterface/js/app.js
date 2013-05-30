(function(){
  /* MODELS */
  Backbone.View.prototype.close = function() {
    this.off();
    this.remove();
    if (this.onClose) //For custom handlers to clean up events bound to anything else than this.model and this.collection
      this.onClose();
    if (this.model)
      this.model.off(null, null, this);
    if (this.collection)
      this.collection.off(null, null, this);
  }
  Backbone.View.prototype.finish = function() {}

  var ApplicationSettings = Backbone.Model.extend({
    defaults: function () {return {connectionStatus: "disconnected", loggerEnabled: false};},
    initialize: function() {
        this.set({"broker": localStorage.getItem("homA_broker") || document.location.hostname || "127.0.0.1"});
        this.set({"loggerEnabled": localStorage.getItem("homA_loggerEnabled") == 1 });
    },
    sync: function() {},// Overwritten to kill default sync behaviour
    save: function() {
      if(this.get("broker")) 
        localStorage.setItem("homA_broker", this.get("broker"))
      if(this.get("loggerEnabled")) 
        localStorage.setItem("homA_loggerEnabled", this.get("loggerEnabled"))
    },
  });
  var Logger = Backbone.Model.extend({
    initialize: function(){
      this.set("logger", console.log); 
      console.log("Log enabled: %s", Settings.get("loggerEnabled"));
      Settings.get("loggerEnabled") ? this.enable() : this.disable();
    },
    enable: function(){window['console']['log'] = this.get("logger");},
    disable: function(){console.log=function(){};}
  });

  var Control = Backbone.Model.extend({
    defaults: function() {return {value: 0, type: "undefined", topic: null };},
  });

  var Device = Backbone.Model.extend({
    defaults: function() {return {name: "", room: undefined };},
    initialize: function() {this.controls = new ControlCollection;},
    hasRoom: function(){return this.get("room") != undefined && this.get("room") != null;},
    removeFromCurrentRoom: function() {
      if (this.hasRoom()) {
        this.get("room").devices.remove(this);
        if (this.get("room").devices.length == 0)
          Rooms.remove(this.get("room"));
      } 
    },
    moveToRoom: function(roomName) {
      console.log("Moving Device to room");
      var cleanedName = roomName || "unassigned";
      var targetRoom = Rooms.get(cleanedName);
      console.log("Target room: %s", cleanedName);
      if(this.hasRoom())
        console.log("Current room: %s", this.get("room").get("id"));
      if(targetRoom != null && this.hasRoom() && this.get("room").get("id") == cleanedName)// Dont move when current room == target room
        return;
      this.removeFromCurrentRoom();
      if (targetRoom == null) {
        console.log("Creating room %s", cleanedName);
        targetRoom = new Room({id: cleanedName});
        Rooms.add(targetRoom);
      } 
      targetRoom.devices.add(this);
      this.set("room", targetRoom);
    },
  });

  var Room = Backbone.Model.extend({initialize: function() {this.devices = new DeviceCollection;}});
  var DeviceCollection = Backbone.Collection.extend({model: Device});
  var RoomCollection = Backbone.Collection.extend({model: Room});
  var ControlCollection = Backbone.Collection.extend({model: Control});

  /* VIEWS */
  var ToplevelView = Backbone.View.extend({rerenderToplevelView: function() {App.renderToplevelView(this);}})

  var SettingsView = ToplevelView.extend({
    template: $("#settings-template").html(),
    events: {"keypress #brokerInput":  "saveServerOnEnter"},
    initialize: function() {
      this.model.view = this; 
      this.model.on('change', this.render, this);
    },
    render: function () {
        var tmpl = _.template(this.template);
        this.$el.html(tmpl(this.model.toJSON()));
        return this;
    },
    saveServerOnEnter: function(e) { 
      console.log("Saving settings");
      if (e.keyCode == 13) {
        this.model.set("broker", e.target.value);
        this.model.save();
        // TODO: disconnect and connect to new broker if value changed
      }
    },
   });

   var RoomLinkView = Backbone.View.extend({
    className: "room-link", 
    tagName: "li",
    template: $("#room-link-template").html(),
    initialize: function() {this.model.roomLink = this; },
    render: function () {
        var tmpl = _.template(this.template);
        this.$el.html(tmpl(this.model.toJSON()));
        return this;
    },
   });

  var PlaceholderView = Backbone.View.extend({
    template: $("#view-placeholder-template").html(),
    className: "view", 
    initialize: function() {this.model.on('add', this.addModel, this);},
    render: function () {this.$el.html( _.template(this.template)(_.extend(this.model.toJSON(), {id: this.id, backText: this.options.backText, backHref: this.options.backHref})));return this;},
    addModel: function(addedObject) {if(addedObject.get("id") == this.id) {Backbone.history.loadUrl(this.options.callbackRoute);}},
  });

  var RoomView = ToplevelView.extend({
    className: "devices", 

    initialize: function() {
      if(this.model instanceof Backbone.Model) {// this.model == ordinary room
        this.collection = this.model.devices;
        this.model.bind('remove', this.removeSelf, this);
        this.model.view = this;
      } else {// this.model == Rooms collection 
        this.collection = this.model;
      }
      this.collection.on('add', this.addDevice, this);
      this.collection.on('remove', this.removeDevice, this);
      this.layoutCardsOptions = {autoResize:true, container:this.$el,offset:25};
      _.bindAll(this, 'layoutCards');
    },
    onClose: function() {this.collection.off();},
    layoutCards: function(){this.$('.card').wookmark(this.layoutCardsOptions);},
    finish: function(){this.layoutCards();},
    render: function () {
      for (var i = 0, l = this.collection.length; i < l; i++)
          this.addDevice(this.collection.models[i]);
      return this;
    },
    addDevice: function(device) {
      var deviceView = new DeviceView({model: device});
      var renderedDeviceView = deviceView.render()
      this.$el.append(renderedDeviceView.el);
      renderedDeviceView.$el.resize(this.layoutCards);
      this.layoutCards();
    },
    removeDevice: function(device) {device.view.close();},
    removeSelf: function(room) {Backbone.history.loadUrl( "rooms/"+room.get("id") )}
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
      this.dynamicInputValueChanged = this.dynamicRender = this.dynamicInhibitInputUpdates = this.dynamicAllowInputUpdates = this.dynamicModelValueChanged = this.methodNotImplemented;

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
    render: function() {return this.dynamicRender();},
    inputValueChanged: function(e) {this.dynamicInputValueChanged(e);},
    modelValueChanged: function(m) {this.dynamicModelValueChanged(m);},
    modelTypeChanged: function() {this.specialize();this.render();},
    inhibitInputUpdates: function(e) {this.dynamicInhibitInputUpdates(e);},
    allowInputUpdates: function(e) {this.dynamicAllowInputUpdates(e);},

    // Specialized methods for type range
    rangeRender: function() {
      var tmpl = this.templateByType("range");
      this.$el.html(tmpl(this.model.toJSON()));
      this.input = this.$('input');
      this.input.attr('max', this.model.get("max") || 255)
      return this;
    },
    rangeInhibitInputUpdates: function(e) {this.allowUpdates = false;},    
    rangeAllowInputUpdates: function(e) {this.allowUpdates = true;},
    rangeInputValueChanged: function(e) {App.publish(this.model.get("topic"), e.target.value);},
    rangeModelValueChanged: function(m) {if (this.allowUpdates) this.render();},

    // Specialized methods for type switch
    switchRender: function() {
      var tmpl = this.templateByType("switch");
      this.$el.html(tmpl(_.extend(this.model.toJSON(), {checkedAttribute: this.model.get("value") == 1 ? "checked=\"true\"" : ""})));
      this.input = this.$('input');
      return this;
    },
    switchInputValueChanged: function(event) {App.publish(this.model.get("topic"), event.target.checked == 0 ? "0" : "1");},
    switchModelValueChanged: function(model) {this.render();},

    // Specialized methods for type text (readonly)
    textRender: function() {
      var tmpl = this.templateByType("text");
      this.$el.html(tmpl(this.model.toJSON()));
      return this;
    },
    textModelValueChanged: function(model) {this.render();},

    // Specialized methods for type undefined
    undefinedRender: function() {
      var tmpl = this.templateByType("undefined");
      this.$el.html(tmpl(this.model.toJSON()));
      return this;
    },

    // Helper methods
    methodNotImplemented: function() {},
    templateByType: function(type) {return _.template($("#" + type +"-control-template").html());},
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
    publishMeta: function(e, type) {
      var value = e.target.value;
      App.publish("/devices/"+this.model.get("id")+"/meta/"+type, value ? value : "", 0, true);
    },
    publishNameInputOnEnter: function(e) { if (e.keyCode == 13) this.publishMeta(e, "name");}, // enter in nameInput
    publishRoomInputOnEnter: function(e) { if (e.keyCode == 13) this.publishMeta(e, "room");}, // enter in roomInput
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
      for (var i = 0, l = this.model.controls.length; i < l; i++)
        this.addControl(this.model.controls.models[i]);
      return this;
    },
    addControl: function(control) {
      var controlView = new ControlView({model: control});
      this.$(".controls").append(controlView.render().el);
    },
  });

  /* BASE APPLICATION LOGIC & MQTT EVENT HANDLING */
  var Application = Backbone.View.extend({
    el: $("#container"),
    mqttClient: undefined,
    initialize: function() {
      Rooms.on('add', this.addRoom, this);
      Rooms.on('remove', this.removeRoom, this);
      _.bindAll(this, 'connected', 'publish');
    },
    addRoom: function(room) {
      var roomLinkView = new RoomLinkView({model: room});
      $('#room-links ul').append(roomLinkView.render().$el);
    },
    removeRoom: function(room) {room.roomLink.close();},
    showView: function(view) {
      if (this.currentView)
        this.currentView.close();
      this.currentView = view;
      this.render(this.currentView);
    },
    render: function(view){
      this.$el.html(view.render().$el);
      view.delegateEvents();
      view.finish();
    },
    connect: function() {
      console.log("Connecting")
      this.mqttClient = new Messaging.Client(Settings.get("broker"), Number(1337), "homA-web-"+Math.random().toString(36).substring(6));
      this.mqttClient.onConnectionLost = this.connectionLost;
      this.mqttClient.onMessageArrived = this.messageArrived;
      this.mqttClient.connect({onSuccess:this.connected});
    },
    connected: function(response){
      console.log("Connected");
      Settings.set("connectionStatus", "connected");
      this.mqttClient.subscribe('/devices/+/controls/+/meta/+', 0);
      this.mqttClient.subscribe('/devices/+/controls/+', 0);
      this.mqttClient.subscribe('/devices/+/meta/#', 0);
      window.onbeforeunload = function(){/*TODO: this.mqttClient.disconnect();*/};
    },
    connectionLost: function(response){ 
      if (response.errorCode !== 0)
        console.log("onConnectionLost:"+response.errorMessage);
      Settings.set("connectionStatus", "disconnected");
      console.log("Connection terminated");
      setTimeout(function () {this.connect();}, 5000);
    },
    messageArrived: function(message){
      // Topic array parsing:
      // Received string:     /devices/$uniqueDeviceId/controls/$deviceUniqueControlId/meta/type
      // topic array index:  0/      1/              2/       3/                     4/   5/   6

      var payload = message.payloadString;
      var topic = message.destinationName.split("/");
      console.log("-----------RECEIVED-----------\nReceived: "+topic+":"+payload);    
      // Ensure the device for the message exists
      var device = Devices.get(topic[2]);
      if (device == null) {
        device = new Device({id: topic[2]});
        Devices.add(device);
        device.moveToRoom(undefined);
      } 
      if(topic[3] == "controls") {
        var control = device.controls.get(topic[4]);
        if (control == null) {
          control = new Control({id: topic[4]});
          device.controls.add(control);
          control.set("topic", "/devices/"+ topic[2] + "/controls/" + topic[4]);
        } 
        if(topic[5] == null)                                       // Control value   
          control.set("value", payload);
        else if (topic[5] == "meta" && topic[6] != null)           // Control meta 
          control.set(topic[6], payload);
      } else if(topic[3] == "meta" ) {                             // TODO: Could be moved to the setter to facilitate parsing
        if (topic[4] == "room")                                    // Device Room
          device.moveToRoom(payload);
        else if(topic[4] == "name")                                // Device name
          device.set('name', payload);
      }
     console.log("-----------/ RECEIVED-----------");
    },
    publish: function(topic, value) {
      console.log("Publishing " + topic+":"+value);
      var message = new Messaging.Message(value);
      message.destinationName = topic+"/on";
      message.retained = true;
      this.mqttClient.send(message); 
    }
  });

  var ApplicationRouter = Backbone.Router.extend({
    routes: {
      "settings" : "settings",
      "devices/:device/settings": "deviceSettings",
      "rooms/:room": "room",
      "": "index",
      "/": "index",
    },
    initialize: function(){
        Backbone.history.start({pushState : false});
    },
    index: function() {
      var indexView = new RoomView({model: Devices});
      App.showView(indexView);   
    },
    room: function(id) {
      var room = Rooms.get(id); // Room might not yet exists
      var view; 
      if (room == null)
        view = new PlaceholderView({model: Rooms, id: id, backText: 'Index', backHref: '#', callbackRoute: Backbone.history.fragment});
      else
        view = new RoomView({model: room});
      App.showView(view);
    },
    settings: function () {
      var settingsView = new SettingsView({model: Settings});
      App.showView(settingsView);
    },
    deviceSettings: function(id) {
      var device = Devices.get(id); // Device might not yet exists
      var view; 
      if (device == null)
        view = new PlaceholderView({model: Devices, id: id, backText: "Index", backHref: '#', callbackRoute: Backbone.history.fragment});
      else
        view = new DeviceSettingsView({model: device});
      App.showView(view);
    },
  });
  var Settings = new ApplicationSettings;
  var Logger = new Logger();
  var Devices = new DeviceCollection;
  var Rooms = new RoomCollection;
  var App = new Application;
  var Router = new ApplicationRouter;
  App.connect();
})();