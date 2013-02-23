#!/usr/bin/env node
var client = require('homa-mqttjs');
		client.argv = client.argv.describe("topic", "The topic to which the payload will be published")
												.describe("payload", "The payload that will be published to the topic")
												.describe("retained", "Whether to set the retained flag on the message that will be published")
												.demand("topic")
												.demand("payload")
												.alias("topic", "t")
												.alias("payload", "p")
												.default("retained", false).argv;


(function connect() {
	client.connect();
})();


// A payload of "" somehow becomes "true" during argv evaluation.
// To fix this we manually check if the payload equals "true" and replace it with a propper emtpy string
client.events.on('connected', function() {
	client.publish(client.argv.topic, client.argv.payload === true ? "" : client.argv.payload, client.argv.retained);
	client.disconnect();
});