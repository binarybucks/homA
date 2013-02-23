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

client.events.on('connected', function() {
	client.publish(client.argv.topic, client.argv.payload, client.argv.retained);
	client.disconnect();
});