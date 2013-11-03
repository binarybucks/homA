#!/usr/bin/env node
var homa = require('homa');
homa.params.describe("topic", "The topic to which the payload will be published")
										.describe("payload", "The payload that will be published to the topic. If no payload is specified an empty string will be published to clear retained messages")
										.describe("retained", "Whether to set the retained flag on the message that will be published")
		   							.demand("topic")
		   							.default("payload", '')		   					
										.alias("topic", "t")
										.default("clear", false)
										.alias("payload", "p")
										.alias("retained", "r")
										.default("retained", false);
var systemId = homa.paramsWithDefaultSystemId("482924-publish");

homa.mqttHelper.on('connect', function() {
	console.log(typeof homa.params.argv.payload)
	if(homa.params.argv.topic != '')
		homa.mqttHelper.publish(homa.params.argv.topic, homa.params.argv.payload, homa.params.argv.retained);
	else
		homa.mqttHelper.publish(homa.params.argv.topic, "", true);

	homa.mqttHelper.disconnect();
});

(function connect() {
	homa.mqttHelper.connect();
})();