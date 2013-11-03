// Meta package for the HomA framework
// 2013 Alexander Rust <mail@alr.st>
// Use it as you like if you find id usefull

var util = require('util');
var events = require('events');
var mqtt = require('mqtt');
var schedule = require('node-schedule');
var log = require('npmlog')
log.disableColor();
var global = this; 
var params = require('pessimist').describe("brokerHost", "The MQTT broker's hostname or IP adress. Can also be set via ENV HOMA_BROKER_HOST").describe("brokerPort", "The MQTT broker's port. Can also be set via ENV HOMA_BROKER_PORT").describe("systemId", "The unique client ID that determines where settings on the /sys topic are received");
		params = process.env.HOMA_BROKER_HOST ? params.default("brokerHost", process.env.HOMA_BROKER_HOST) : params.demand("brokerHost");
		params = process.env.HOMA_BROKER_PORT ? params.default("brokerPort", process.env.HOMA_BROKER_PORT) : params.default("brokerPort", 1883);


// finalizes parameters and sets default systemId
module.exports.paramsWithDefaultSystemId = function(systemd){
	params = params.default("systemId", systemd).argv;
	return params.systemId;
}


var StringHelper = function() {
	this.pad = function(n, width, symbol, back) {
	  symbol = symbol || '0';
	  n = n + '';

	  if (n.length >= width)
	  	return n;
	  else
	  	return back || false ? n + new Array(width - n.length + 1).join(symbol) : new Array(width - n.length + 1).join(symbol) + n;
	}
}

var SettingsHelper = function() {
	var self = this; 
	self.settings = {}; // key is full topic string of the form /sys/$deviceId/someParameter
	self.locked = false;
	self.requiredSettings = []; 
	self.optionalSettings = [];

	this.keyToSysTopic = function(key) {
		return "/sys/"+params.systemId+"/"  + key;
	}

	// Check if a required or optional settings value was received from the MQTT broker. If so, save it for later. 
	// force allows to store key:value pairs even if they are not required
	// topic is a complete topic string of the form /sys/$deviceId/someParameter
	this.insert = function(topic, value, force){
		if(force || (self.requiredSettings.indexOf(topic) > -1) || self.optionalSettings.indexOf(topic) > -1)
			self.settings[topic] = value;
	}

	// Saves value for a parameter to the MQTT /sys topic for the current clientId
	this.save = function(parameter, value){
			global.mqttHelper.publish(self.keyToSysTopic(parameter), value, true);
			self.settings[self.keyToSysTopic(parameter)] = value;
	}


	// parameter is in the form someParameter, without /sys topic string
	this.require = function(parameter){
		self.requiredSettings.push(self.keyToSysTopic(parameter));
		global.mqttHelper.subscribe(self.keyToSysTopic(parameter));
	}

	this.optional = function(parameter){
		self.optionalSettings.push(self.keyToSysTopic(parameter));
		global.mqttHelper.subscribe(self.keyToSysTopic(parameter));
	}


	// parameter is in the form someParameter, without /sys topic string
	this.get = function(parameter, inputAsSysTopicStr) {
		return inputAsSysTopicStr ? self.settings[parameter] : self.settings[self.keyToSysTopic(parameter)];
	}


	this.isBootstrapCompleted = function() {
		var pass = true
		for(i=0;i<self.requiredSettings.length;i++){
			if(self.get(self.requiredSettings[i], true) == undefined || self.get(self.requiredSettings[i], true) == ""){
				log.info("SETTINGS", "Waiting to receive: " + self.requiredSettings[i]);
				pass = false;
			}
		}
		if(pass) {
			log.info("SETTINGS", "Bootstrap completed");
		}
		return pass;
	}
	this.isLocked = function(){
		return self.locked;
	}
	this.lock = function() {
		self.locked = true; 
	}
	this.unlock = function() {
		self.locked = false;
	}
}

var MqttHelper = function() {
	var self = this;
	self.mqttClient;
	self.scheduledPublishes = [];

	this.connect = function(host, port, extraParameters) {
		self.mqttClient = mqtt.createClient(port || params.brokerPort, host || params.brokerHost, extraParameters);
		log.info("MQTT", "Connecting to %s:%s", host || params.brokerHost, port || params.brokerPort);
	
	  self.mqttClient.on('connect', function() {
         self.emit('connect');
 	  });

	  self.mqttClient.on('close', function() {
	  	log.info("MQTT", "Connection closed");
	    process.exit(-1);
	  });

	  self.mqttClient.on('error', function(e) {
	  	log.error("MQTT", "Error: %s", e);
	    process.exit(-1);
	  });

	 	self.mqttClient.on('message', function(topic, message, etc) {
	 		self.emit('message', {topic: topic, payload: message, qos: etc.qos, retain: etc.retain, messageId: etc.messageId });
		});
	}

	this.publish = function(topic, payload, retained, ops, callback) {
		var o;
		if(ops)
			o = ops; 
		else 
			o  = {qos: 0, retain: retained};

		log.info("MQTT", "Publishing %s:%s (retained=%s)", topic, payload, o.retain);
		self.mqttClient.publish(topic.toString(), payload.toString(), o, callback);
	}

	this.schedulePublish = function(date, topic, payload, retained){
		log.info("SCHEDULE", "At %s, publishing %s:%s (retained=%s)", date, topic, payload, retained);
		var job = exports.scheduler.scheduleJob(date, function(){
				self.publish(topic, payload, retained || false);
		});
		self.scheduledPublishes.push(job);
		return job;
	}

	this.unschedulePublishes = function() {
		for (var i=0; i<self.scheduledPublishes.length; i++) {
			self.scheduledPublishes[i].cancel();
		}
	}

	this.disconnect =function(){
		self.mqttClient.end();
	}

	this.subscribe  = function(topic, opts, callback) {
		self.mqttClient.subscribe(topic, opts, callback);
	}

 	this.unsubscribe = function(topic) {
		self.mqttClient.unsubscribe(topic);
	}
}
util.inherits(MqttHelper, events.EventEmitter);

module.exports.params = params; 
module.exports.stringHelper = new StringHelper();
module.exports.scheduler = schedule;
module.exports.logger = log;
module.exports.mqttHelper = new MqttHelper();
module.exports.settings = new SettingsHelper();

