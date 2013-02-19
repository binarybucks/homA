#!/usr/bin/env node
var mqtt = require('mqttjs')
var argv = require('optimist').usage('Usage: $0 [--brokerHost 127.0.0.1] [--brokerPort 1883] [--topic topic] [--payload payload] [--retained]')
															.string("payload")
															.string("topic")
															.default("brokerHost", '127.0.0.1')
															.default("brokerPort", 1883)
															.default("retained", false)
															.argv;

var MQTT_CLIENT_ID ='publishjs'
var topic = undefined;
var payload = undefined;
var mqttClient;

function mqttConnect() {
	mqtt.createClient(argv.brokerPort, argv.brokerHost, function(err, client) {
	  if (err) {
	  	console.log('MQTT        %s', err);
	  	process.exit(1);
	  }
	  mqttClient = client;
	  client.connect({keepalive: 3000});

	  client.on('connack', function(packet) {
	    if (packet.returnCode === 0) {
	    	if(argv.topic != undefined) {
	    		if(argv.payload != undefined) {
	    			mqttPublish(argv.topic, argv.payload, argv.retained);
	    		} else {
	    			ask("Payload:    ", function(p){
	    				mqttPublish(argv.topic, p, argv.retained);
	    			});
	    		}
	    	} else {
					ask("Topic:      ", function(t){
						if(argv.payload != undefined) {
							mqttPublish(t, argv.payload, argv.retained);
						} else {
							ask("Payload:    ", function(p) {
		    				mqttPublish(t, p, argv.retained);
							});
						}
					});
	    	}

	    } else {
	      console.log('MQTT        Connack error %d', packet.returnCode);
	      process.exit(-1);
	    }
	  });

	  client.on('close', function() {
	    process.exit(-1);
	  });

	  client.on('error', function(e) {
	    console.log('MQTT        Error: %s', e);
	    process.exit(-1);
	  });
	});
}

function mqttPublish(topic, payload, retained) {
	console.log("Publishing " + topic + ":" + payload);
	mqttClient.publish({ topic: topic.toString(), payload: payload, qos: 0, retain: retained});
	mqttClient.disconnect();	
}

function ask(question, callback) {
 var stdin = process.stdin, stdout = process.stdout;
 stdin.resume();
 stdout.write(question);
 stdin.once('data', function(data) {
   data = data.toString().trim();
   callback(data);
 });
}

mqttConnect();
