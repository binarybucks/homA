#!/usr/bin/env node
var homa = require('homa');
		homa.argv = homa.argv.argv;
var messages = {};

homa.mqttHelper.on('connect', function(packet) {
	homa.mqttHelper.subscribe('#');
	unpublish();
});

homa.mqttHelper.on('message', function(packet) {
  if(packet.payload != "" && packet.payload != undefined) {
 		messages[packet.topic.toString()] = packet.payload;
	} else {
		delete messages[packet.topic];
	}
	process.nextTick(printMessages);
});

function unpublish() {
		ask("", function(data) {
			if(data != undefined && parseInt(data) <= Object.keys(messages).length-1) {
				process.nextTick(function(){homa.mqttHelper.publish(Object.keys(messages)[data], "", true);});
				process.nextTick(unpublish);
			} else {
				printMessages();
				process.nextTick(unpublish);
			}
		});
}

function printMessages(){
	console.log("\n\n\n");
	var i = 0;
	for (key in messages) {
		console.log(" " + homa.stringHelper.pad(i.toString(), 4, " ")+": " + key + ":" + messages[key]);
		i++;
	};
	process.stdout.write("       Enter # to unpublish: ");
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

(function connect() {
	homa.mqttHelper.connect();
})();


