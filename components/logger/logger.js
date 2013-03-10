#!/usr/bin/env node
var homa = require('homa');
var argv = homa.paramHelper.argv;
var messages = {};

homa.mqttHelper.on('connect', function(packet) {
	homa.mqttHelper.subscribe('#');
});

homa.mqttHelper.on('message', function(packet) {
	console.log("%s topic: %s, payload: %s, qos: %s, messageId: %s, retained: %s", new Date(), packet.topic, packet.payload, packet.qos, packet.mid, packet.retain);
});

(function connect() {
	homa.mqttHelper.connect();
})();


