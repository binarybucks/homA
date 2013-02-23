// A simple wrapper around mqttjs for easier usage in my personal projects
// 2013 Alexander Rust <mail@alr.st>
// Use it as you like if you find id usefull

var sys = require('sys');
var mqtt = require('mqttjs');
var argv = require('optimist');
var EventEmitter = require('events').EventEmitter;

// This requires the --brokerHost (--brokerPort) commandline argument if the environment variable HOMA_BROKER_HOST (HOMA_BROKER_PORT) is not set. 
// If the environment variable is set the the default value of brokerHost (brokerPort) is set to the value of the environment variable
// If both, environment variable and commandline argument, are set the commandline argument takes precedence. 
argv = argv.describe("brokerHost", "The MQTT broker's hostname or IP adress. Can also be set via ENV HOMA_BROKER_HOST").describe("brokerPort", "The MQTT broker's port. Can also be set via ENV HOMA_BROKER_PORT");
argv = process.env.HOMA_BROKER_HOST ? argv.default("brokerHost", process.env.HOMA_BROKER_HOST) : argv.demand("brokerHost");
argv = process.env.HOMA_BROKER_PORT ? argv.default("brokerPort", process.env.HOMA_BROKER_PORT) : argv.default("brokerPort", 1883);

module.exports.argv = argv; 
module.exports.events = new EventEmitter();

module.exports.connect = function(host, port, callback) {
	console.log("MQTT        Connecting to %s:%s", host || exports.argv.brokerHost, port || exports.argv.brokerPort );

	mqtt.createClient(port || exports.argv.brokerPort, host || exports.argv.brokerHost, function(err, client) {
	  if (err) {
	  	console.log('MQTT        %s', err);
	  	process.exit(1);
	  }

	  exports.mqttClient = client;
	  client.connect({keepalive: 40000});

	  client.on('connack', function(packet) {
        if (packet.returnCode === 0) {
            setInterval(function() {client.pingreq();}, 30000);
            exports.events.emit('connected', packet);
        } else {
          console.log('MQTT        Connack error %d', packet.returnCode);
          process.exit(-1);
        }
	  });

	  client.on('close', function() {
	  	console.log('MQTT        Connection closed');
	    process.exit(-1);
	  });

	  client.on('error', function(e) {
	    console.log('MQTT        Error: %s', e);
	    process.exit(-1);
	  });

	 	client.on('publish', function(packet) {
	 		exports.events.emit('receive', packet);
		});
	});
}

module.exports.publish = function(topic, payload, retained) {
		console.log("MQTT        Publishing %s:%s (retained=%s)", topic, payload, retained);
		exports.mqttClient.publish({ topic: topic.toString(), payload: payload.toString(), qos: 0, retain: retained});
}

module.exports.disconnect = function() {
	exports.mqttClient.disconnect();
}


module.exports.subscribe = function(topic) {
	exports.mqttClient.subscribe({topic: topic});
}


module.exports.unsubscribe = function(topic) {
	exports.mqttClient.unsubscribe({topic: topic});
}
