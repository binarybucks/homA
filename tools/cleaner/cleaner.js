#!/usr/bin/env node
var mqtt = require('mqttjs')
var argv = require('optimist').usage('Usage: $0 [--brokerHost 127.0.0.1] [--brokerPort 1883]')
															.default("brokerHost", '127.0.0.1')
															.default("brokerPort", 1883)
															.argv;

var mqttClient;
var messages = {};


function mqttConnect() {
	mqtt.createClient(argv.brokerPort, argv.brokerHost, function(err, client) {
	  if (err) {
	  	console.log('MQTT        %s', err);
	  	process.exit(1);
	  }
	  mqttClient = client;
	  client.connect({keepalive: 3000});

	  client.on('connack', function(packet) {
	  	client.subscribe({topic: '#'});
			unpublish();

	  });

	  client.on('close', function() {
	    process.exit(-1);
	  });

	  client.on('error', function(e) {
	    console.log('MQTT        Error: %s', e);
	    process.exit(-1);
	  });

	 	client.on('publish', function(packet) {
	  	if(packet.payload != "" && packet.payload != undefined) {
		 		messages[packet.topic.toString()] = packet.payload;
	  	} else {
	  		delete messages[packet.topic];
	  	}
	 		process.nextTick(printMessages);
	
		});
	});
}

function unpublish() {
		ask("", function(data) {
			if(data != undefined && parseInt(data) <= Object.keys(messages).length-1) {
				process.nextTick(function(){mqttPublish(Object.keys(messages)[data], "", true);});
				process.nextTick(unpublish);
			} else {
				printMessages();
				process.nextTick(unpublish);
			}
		});
}


function mqttPublish(topic, payload, retained) {
	mqttClient.publish({ topic: topic.toString(), payload: payload.toString(), qos: 0, retain: retained});
}

function printMessages(){
	console.log("\n\n   #: Topic:Value");
	var i = 0;
	for (key in messages) {
		console.log(pad(i.toString(), 4)+": " + key + ":" + messages[key]);
		i++;
	};
	process.stdout.write("      Enter # to unpublish: ");
}

function pad(num, size) {
    var s = "     " + num;
    return s.substr(s.length-size);
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
