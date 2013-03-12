// Meta package for the HomA framework
// 2013 Alexander Rust <mail@alr.st>
// Use it as you like if you find id usefull

var util = require('util');
var events = require('events');
var mqtt = require('mqtt');
var schedule = require('node-schedule');
var log = require('npmlog')
var paramHelper = require('optimist').describe("brokerHost", "The MQTT broker's hostname or IP adress. Can also be set via ENV HOMA_BROKER_HOST").describe("brokerPort", "The MQTT broker's port. Can also be set via ENV HOMA_BROKER_PORT");
		paramHelper = process.env.HOMA_BROKER_HOST ? paramHelper.default("brokerHost", process.env.HOMA_BROKER_HOST) : paramHelper.demand("brokerHost");
		paramHelper = process.env.HOMA_BROKER_PORT ? paramHelper.default("brokerPort", process.env.HOMA_BROKER_PORT) : paramHelper.default("brokerPort", 1883);

log.disableColor();

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

var MqttHelper = function() {
	var self = this;
	self.mqttClient;
	self.scheduledPublishes = [];

	this.connect = function(host, port, callback) {
		self.mqttClient = mqtt.createClient(port || exports.paramHelper.argv.brokerPort, host || exports.paramHelper.argv.brokerHost, {keepalive: 40});
		log.info("MQTT", "Connecting to %s:%s", host || module.exports.paramHelper.argv.brokerHost, port || exports.paramHelper.argv.brokerPort);
	
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

	this.publish = function(topic, payload, retained) {
		log.info("MQTT", "Publishing %s:%s (retained=%s)", topic, payload, retained);
		self.mqttClient.publish(topic.toString(), payload.toString(), {qos: 0, retain: retained});
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

	this.subscribe  = function(topic) {
		self.mqttClient.subscribe(topic);
	}

 	this.unsubscribe = function(topic) {
		self.mqttClient.unsubscribe(topic);
	}
}
util.inherits(MqttHelper, events.EventEmitter);

module.exports.mqttHelper = new MqttHelper();
module.exports.stringHelper = new StringHelper();
module.exports.paramHelper = paramHelper; 

module.exports.scheduler = schedule;
module.exports.logger = log;