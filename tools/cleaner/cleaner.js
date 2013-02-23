#!/usr/bin/env node
var client = require('homa-mqttjs');
		client.argv = client.argv.argv;

var messages = {};

client.events.on('connected', function(packet) {
	client.subscribe('#');
	unpublish();
});

client.events.on('receive', function(packet) {
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
				process.nextTick(function(){client.publish(Object.keys(messages)[data], "", true);});
				process.nextTick(unpublish);
			} else {
				printMessages();
				process.nextTick(unpublish);
			}
		});
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
	console.log("ask");
 var stdin = process.stdin, stdout = process.stdout;
 stdin.resume();
 stdout.write(question);
 stdin.once('data', function(data) {
   data = data.toString().trim();
   callback(data);
 });
}

(function connect() {
	client.connect();
})();


