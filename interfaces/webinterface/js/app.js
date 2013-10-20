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
    defaults: { connectivity: "disconnected", 
                logging: "0", 
                port: "18883", 
                host: document.location.hostname || "127.0.0.1"
              },

    initialize: function() {
      for (var key in this.defaults)
        this.set(key, localStorage.getItem(key) || this.defaults[key]);
    },
    sync: function() {},// Overwritten to kill default sync behaviour
    save: function(data) {
      for (var key in data) {
        this.set(key, data[key]);
        localStorage.setItem(key, data[key]);
      }
      Router.back();
    }
  });
  var Logger = Backbone.Model.extend({
    initialize: function(){
      this.set("logger", console.log); 
      Settings.get("logging")==='1' ? this.enable() : this.disable();
    },
    enable: function(){window['console']['log'] = this.get("logger");},
    disable: function(){  
      console.log("Console.log disabled. Set local storage logging=1 to enable");
      console.log=function(){};}
  });

  var Control = Backbone.Model.extend({
    defaults: function() {return {value: 0, type: "undefined", topic: null, order: 0 };}, 
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
      var cleanedName = roomName || "unassigned";
      var targetRoom = Rooms.get(cleanedName);

      if(targetRoom != null && this.hasRoom() && this.get("room").get("id") == cleanedName)// Dont move when current room == target room
        return;

      console.log("Moving Device to room: %s", cleanedName);

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
  var ControlCollection = Backbone.Collection.extend({model: Control, initialize: function() {
    this.sort_key = 'order';
},
comparator: function(a, b) {
    // Assuming that the sort_key values can be compared with '>' and '<',
    // modifying this to account for extra processing on the sort_key model
    // attributes is fairly straight forward.
    a = a.get(this.sort_key);
    b = b.get(this.sort_key);
    return a > b ?  1
         : a < b ? -1
         :          0;
} }
);

  var SettingsView = Backbone.View.extend({
    className: "view", 
    id: "settings-view",
    template: $("#settings-template").html(),
    events: {"click .button.save":  "save"},

    initialize: function() {
      this.model.view = this; 
      // this.model.on('change', this.render, this);
    },

    render: function () {
        var tmpl = _.template(this.template);
        this.$el.html(tmpl(this.model.toJSON()));
        return this;
    },
    save: function(e) { 
      var arr = this.$el.find('form').serializeArray();
      var data = _(arr).reduce(function(acc, field){acc[field.name] = field.value;return acc;}, {});
      var reconnect = (this.model.get("host") != data["host"] || this.model.get("port") != data["port"])
      this.model.save(data);
      if(reconnect)
        App.reconnect();
    },
   });

   var RoomLinkView = Backbone.View.extend({
    className: "room-link", 
    tagName: "li",
    template: $("#room-link-template").html(),
    initialize: function() {this.model.roomLink = this;},
    render: function () {
        var tmpl = _.template(this.template);
        var id, link; 
        if (this.model instanceof Backbone.Model) { // when model is actually a model, its the view for a room
          id = this.model.get("id");
          link = "#rooms/"+id;
        } else { // When model is a collection, this is the "All" view that has to be treated a bit differently
          id = "All"
          link = "#";
        }
        this.$el.html(tmpl({link: link, id: id}));
        return this;
    },
   });

  var PlaceholderView = Backbone.View.extend({
    template: $("#view-placeholder-template").html(),
    className: "view", 
    id: "placeholder-view",
    initialize: function() {this.model.on('add', this.addModel, this);},
    render: function () {this.$el.html( _.template(this.template)(this.model.toJSON()));return this;},
    addModel: function(addedObject) {if(this.options.readyComparator(addedObject)) {Backbone.history.loadUrl(this.options.callbackRoute);}},
  });

  var RoomView = Backbone.View.extend({
    className: "view", 
    id: "room-view",

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
    onClose: function() {this.collection.off(); this.model.roomLink.$el.removeClass("active");},
    layoutCards: function(){this.$('.card').wookmark(this.layoutCardsOptions);},
    finish: function(){
      this.layoutCards();
      if(this.model.roomLink)
        this.model.roomLink.$el.addClass("active");
    },
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
    className: "subview control-view",
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
      } else if (this.model.get("type") == "image") {
        this.dynamicRender = this.imageRender;
        this.dynamicModelValueChanged = this.imageModelValueChanged;
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

    // Specialized methods for type text (read-only)
    textRender: function() {
      var tmpl = this.templateByType("text");
      this.$el.html(tmpl(this.model.toJSON()));
      return this;
    },
    textModelValueChanged: function(model) {this.render();},

    // Specialized methods for type image (read-only)
    imageRender: function() {
      var tmpl = this.templateByType("image");
      this.$el.html(tmpl(this.model.toJSON()));
      return this;
    },
    imageModelValueChanged: function(model) {this.render();},


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


  var DeviceSettingsView = Backbone.View.extend({
    template: $("#device-settings-template").html(),
    id: "device-settings-view",
    className: "view", 
    events: {"click .button.save":  "save",},
    initialize: function() {
      this.model.view = this;
      _.bindAll(this, 'save');
      this.model.on('change', this.render, this);
    },
    render: function() {
      var tmpl = _.template(this.template);
      var roomName = this.model.hasRoom() ? this.model.get("room").get("id") : "unassigned"
      this.$el.html(tmpl(_.extend( this.model.toJSON(), {roomname: roomName})));
      this.delegateEvents();
      return this;
    },
    save: function(e) { 
      var arr = this.$el.find('form').serializeArray();
      var data = _(arr).reduce(function(acc, field){acc[field.name] = field.value;return acc;}, {});
      for(setting in data)
        App.publishForDevice(this.model.get("id"), "/meta/"+setting, data[setting]);
      Router.back();
    },
  });

  var DeviceView = Backbone.View.extend({
    template: $("#device-template").html(),
    className: "card", 
    initialize: function() {
      this.model.on('change', this.render, this);
      this.model.on('destroy', this.remove, this);
      this.model.controls.on('add', this.addControl, this);
      this.model.controls.on('remove', this.render, this);
      this.model.controls.on('sort', this.render, this);
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
      this.$(".subviews").append(controlView.render().el);
    },
  });

  /* BASE APPLICATION LOGIC & MQTT EVENT HANDLING */
  var Application = Backbone.View.extend({
    el: $("body"),
    container: $("#container"),
    roomLinks: $("#room-links > ul"),
    connectivity: $("#connectivity"),

    mqttClient: undefined,
    connectivityTimeoutId: undefined,

    initialize: function() {
      Settings.on('change:connectivity', this.connectivityChanged, this);
      Rooms.on('add', this.addRoom, this);
      Rooms.on('remove', this.removeRoom, this);
      _.bindAll(this, 'connect', 'connected', 'publish', 'publishForDevice', 'connectionLost', 'disconnect', 'disconnected');
      this.addRoom(Devices);
    },
    connectivityChanged: function(e){
      if(this.connectivityTimeoutId)
        clearTimeout(this.connectivityTimeoutId);
      console.log("Connectivity changed to: %s", e.get("connectivity"));
      this.connectivity.removeClass("visible");
      this.connectivity.html(e.get("connectivity"));
      this.connectivity.addClass("visible");
      var that = this; 
      this.connectivityTimeoutId = setTimeout(function(){that.connectivity.removeClass("visible")}, 5000);

    },
    addRoom: function(room) {
      var roomLinkView = new RoomLinkView({model: room});
      this.roomLinks.append(roomLinkView.render().$el);
    },
    removeRoom: function(room) {room.roomLink.close();},
    showView: function(view) {
      if (this.currentView)
        this.currentView.close();
      this.currentView = view;
      this.render(this.currentView);
    },
    render: function(view){
      this.container.html(view.render().$el);
      view.delegateEvents();
      view.finish();
    },
    reconnect: function(){
      console.log("Reconnecting");
      this.disconnect();
      this.connect(); 
    },
    connect: function() {
      Settings.set("connectivity", "connecting");
      this.mqttClient = new Messaging.Client(Settings.get("host"), parseInt(Settings.get("port")), "homA-web-"+Math.random().toString(36).substring(6));
      this.mqttClient.onConnectionLost = this.connectionLost;
      this.mqttClient.onMessageArrived = this.messageArrived;
      this.mqttClient.connect({onSuccess:this.connected, useSSL: false});
    },
    connected: function(response){
      Settings.set("connectivity", "connected");
      this.mqttClient.subscribe('/devices/+/controls/+/meta/+', 0);
      this.mqttClient.subscribe('/devices/+/controls/+', 0);
      this.mqttClient.subscribe('/devices/+/meta/#', 0);
      window.onbeforeunload = function(){App.disconnect()}; 
    },
    disconnect: function() {
      if(Settings.get("connectivity") == "connected")
        this.mqttClient.disconnect(); 
    },
    disconnected: function() {
      Settings.set("connectivity", "disconnected");
      console.log("Connection terminated");

      for (var i = 0, l = Devices.length; i < l; i++)
        Devices.pop();        

      for (i = 0, l = Rooms.length; i < l; i++)
        Rooms.pop();        
    },
    connectionLost: function(response){ 
      if (response.errorCode !== 0) {
        console.log("onConnectionLost:"+response.errorMessage);
        setTimeout(function () {App.connect();}, 5000); // Schedule reconnect if this was not a planned disconnect
      }

      this.disconnected();
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
        else if (topic[5] == "meta" && topic[6] != null) {           // Control meta 

          if(topic[6] == "order") {                                 // Todo: move sorting to a model.on('change:order') event
            control.set("order", parseInt(payload));
            device.controls.sort();
          } else {
            control.set(topic[6], payload);
          }
        }
      } else if(topic[3] == "meta" ) {                             // TODO: Could be moved to the setter to facilitate parsing
        if (topic[4] == "room")                                    // Device Room
          device.moveToRoom(payload);
        else if(topic[4] == "name")                                // Device name
          device.set('name', payload);
      }
     console.log("-----------/ RECEIVED-----------");
    },
    publish: function(topic, value) {
      value = value != undefined ? value : "";
      console.log("Publishing " + topic+":"+value);
      var message = new Messaging.Message(value);
      message.destinationName = topic+"/on";
      message.retained = true;
      this.mqttClient.send(message); 
    },
    publishForDevice: function(deviceId, subtopic, value) {
      this.publish("/devices/"+deviceId+subtopic, value);
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
        this.routesHit = 0;
        Backbone.history.on('route', function() { this.routesHit++; }, this);
    },
    index: function() {
      var indexView = new RoomView({model: Devices});
      App.showView(indexView);   
    },
    // TODO: the room and deviceSettings route behave exactly the same. They should be merged to a single method
    room: function(id) {
      var room = Rooms.get(id); // Room might not yet exists
      var view; 
      var matchId = id;

      if (room == null)
        view = new PlaceholderView({model: Rooms, callbackRoute: Backbone.history.fragment, readyComparator: function(addedObject){return addedObject.get("id") == matchId;}});
      else
        view = new RoomView({model: room});
      App.showView(view);
    },
    deviceSettings: function(id) {
      var device = Devices.get(id); // Device might not yet exists
      var view; 
      var matchId = id;
      console.log("Backbone.history");
      console.log(Backbone.history);

      if (device == null)
        view = new PlaceholderView({model: Devices, callbackRoute: Backbone.history.fragment, readyComparator: function(addedObject){return addedObject.get("id") == matchId;}});
      else
        view = new DeviceSettingsView({model: device});
      App.showView(view);
    },
    settings: function () {
      var settingsView = new SettingsView({model: Settings});
      App.showView(settingsView);
    },
      back: function() {
    if(this.routesHit > 1) {
      //more than one route hit -> user did not land to current page directly
      window.history.back();
    } else {
      //otherwise go to the home page. Use replaceState if available so
      //the navigation doesn't create an extra history entry
      this.navigate('/', {trigger:true, replace:true});
    }
  }

  });
  var Settings = new ApplicationSettings;
  var Logger = new Logger();
  var Devices = new DeviceCollection;
  var Rooms = new RoomCollection;
  var App = new Application;
  var Router = new ApplicationRouter;
  App.connect();
})();