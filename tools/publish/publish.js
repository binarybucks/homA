#!/usr/bin/env node
var homa = require('homa');
homa.params.describe("topic", "The topic to which the payload will be published")
										.describe("payload", "The payload that will be published to the topic")
										.describe("retained", "Whether to set the retained flag on the message that will be published")
										.demand("topic")
										.demand("payload")
										.alias("topic", "t")
										.alias("payload", "p")
										.alias("retained", "r")
										.default("retained", false);
var systemId = homa.paramsWithDefaultSystemId("482924-publish");

homa.mqttHelper.on('connect', function() {
	homa.mqttHelper.publish(homa.params.argv.topic, homa.params.argv.payload === true ? "" : homa.params.argv.payload, homa.params.argv.retained);
	homa.mqttHelper.disconnect();
});

(function connect() {
	homa.mqttHelper.connect();
})();