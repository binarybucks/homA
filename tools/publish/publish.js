#!/usr/bin/env node
var homa = require('homa');
		homa.argv = homa.argv.describe("topic", "The topic to which the payload will be published")
										.describe("payload", "The payload that will be published to the topic")
										.describe("retained", "Whether to set the retained flag on the message that will be published")
										.demand("topic")
										.demand("payload")
										.alias("topic", "t")
										.alias("payload", "p")
										.alias("retained", "r")
										.default("retained", false).argv;

homa.mqttHelper.on('connected', function() {
	homa.mqttHelper.publish(homa.argv.topic, homa.argv.payload === true ? "" : homa.argv.payload, homa.argv.retained);
	homa.mqttHelper.disconnect();
});

(function connect() {
	homa.mqttHelper.connect();
})();