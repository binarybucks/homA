#!/usr/bin/env node
var homa = require('homa');
var argv = homa.paramHelper.describe("topic", "The topic to which the payload will be published")
										.describe("payload", "The payload that will be published to the topic")
										.describe("retained", "Whether to set the retained flag on the message that will be published")
										.demand("topic")
										.demand("payload")
										.alias("topic", "t")
										.alias("payload", "p")
										.alias("retained", "r")
										.default("retained", false).argv;

homa.mqttHelper.on('connect', function() {
	homa.mqttHelper.publish(argv.topic, argv.payload === true ? "" : argv.payload, argv.retained);
	homa.mqttHelper.disconnect();
});

(function connect() {
	homa.mqttHelper.connect();
})();