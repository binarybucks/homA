// Meta package for the HomA framework
// 2013 Alexander Rust <mail@alr.st>
// Use it as you like if you find id usefull

var util = require('util');
var events = require('events');
var mqtt = require('mqttjs');
var schedule = require('node-schedule');
var argv = require('optimist').describe("brokerHost", "The MQTT broker's hostname or IP adress. Can also be set via ENV HOMA_BROKER_HOST").describe("brokerPort", "The MQTT broker's port. Can also be set via ENV HOMA_BROKER_PORT");
		argv = process.env.HOMA_BROKER_HOST ? argv.default("brokerHost", process.env.HOMA_BROKER_HOST) : argv.demand("brokerHost");
		argv = process.env.HOMA_BROKER_PORT ? argv.default("brokerPort", process.env.HOMA_BROKER_PORT) : argv.default("brokerPort", 1883);

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

var LogHelper = function() {
	this.log = function(tag, message) {
		console.log("%s %s", exports.stringHelper.pad(tag, 12, " ", true), message)
	}
}

var MqttHelper = function() {
	var TAG = "MQTT"
	var mqttClient;
	var scheduledPublishes = [];
	var self = this;

	this.connect = function(host, port, callback) {
		exports.logHelper.log(TAG, "Connecting to " +  (host || module.exports.argv.brokerHost) + ":" + (port || exports.argv.brokerPort));
		mqtt.createClient(port || exports.argv.brokerPort, host || exports.argv.brokerHost, function(err, client) {
		  if (err) {
		  	exports.logHelper.log(TAG, err.toString());
		  	process.exit(1);
		  }

		  self.mqttClient = client;
		  client.connect({keepalive: 40000});

		  client.on('connack', function(packet) {
	        if (packet.returnCode === 0) {
	            setInterval(function() {client.pingreq();}, 30000);
	            self.emit('connected', packet);
	        } else {
	        	exports.logHelper.log(TAG, "Connack error");
	          process.exit(-1);
	        }
		  });

		  client.on('close', function() {
		  	exports.logHelper.log(TAG, "Connection closed");
		    process.exit(-1);
		  });

		  client.on('error', function(e) {
		  	exports.logHelper.log(TAG, "Error " + e.toString);
		    process.exit(-1);
		  });

		 	client.on('publish', function(packet) {
		 		self.emit('receive', packet);
			});
		});
	}

	this.publish = function(topic, payload, retained) {
		exports.logHelper.log(TAG, "Publishing " + topic + ":" + payload + "(retained: " + retained + ")");
		self.mqttClient.publish({ topic: topic.toString(), payload: payload.toString(), qos: 0, retain: retained});
	}

	this.schedulePublish = function(date, topic, payload, retained){
		exports.logHelper.log("SCHEDULE", "At " + date + " publishing " + topic + ":" + payload + "(retained: " + retained || false + ")");
		var job = schedule.scheduleJob(date, function(){
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
		self.mqttClient.disconnect();
	}

	this.subscribe  = function(topic) {
		self.mqttClient.subscribe({topic: topic});
	}

 	this.unsubscribe = function(topic) {
		self.mqttClient.unsubscribe({topic: topic});
	}
}
util.inherits(MqttHelper, events.EventEmitter);

module.exports.mqttHelper = new MqttHelper();
module.exports.stringHelper = new StringHelper();
module.exports.logHelper = new LogHelper();

module.exports.scheduler = schedule;
module.exports.argv = argv; 