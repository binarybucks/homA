#!/usr/bin/env node
var date = require("datejs")
var suncalc = require('suncalc');
var homa = require('homa');
var argv = homa.paramHelper.default("systemId", "294028-solar").argv;

// describe("latitude", "Latitude at current location")
// 												.describe("longitude", "Longitude at current location")
// 												.demand("latitude")
// 												.demand("longitude")
// 												.alias("latitude", "lat")
// 												.alias("longitude", "long")
												
var settings = {};
var MQTT_TOPIC_LONGITUDE = "/sys/"+ argv.systemId + "/longitude"
var MQTT_TOPIC_LATITUDE = "/sys/" + argv.systemId + "/latitude";
var bootstrapCompleted = false;

homa.mqttHelper.on('message', function(packet) {
	settings[packet.topic] = packet.payload;
	if (bootstrapComplete() && !bootstrapCompleted) {
		bootstrapCompleted = true;
		homa.logger.info("SOLAR", "Bootstrap completed.");
	homa.mqttHelper.publish("/devices/"+ argv.systemId + "/controls/Sunset/meta/type", "text", true);
	homa.mqttHelper.publish("/devices/"+ argv.systemId + "/controls/Sunrise/meta/type", "text", true);

	homa.scheduler.scheduleJob('0 0 * * *', querySuntimes); // Query every day at midnight
	querySuntimes();
	}
});

function bootstrapComplete() {
		var pass = true
		var requiredItems = [MQTT_TOPIC_LONGITUDE, MQTT_TOPIC_LATITUDE];
		for(i=0;i<requiredItems.length;i++){
			if(settings[requiredItems[i]] == undefined || settings[requiredItems[i]] == ""){
				homa.logger.info("SOLAR", "Waiting to receive setting: " + requiredItems[i] );
				pass = false;
			}
		}
		return pass;
}
(function(){
	homa.mqttHelper.connect();
})();

homa.mqttHelper.on('connect', function(packet) {
	homa.mqttHelper.subscribe("/sys/"+ argv.systemId + "/latitude");
	homa.mqttHelper.subscribe("/sys/"+ argv.systemId + "/longitude");
});

function querySuntimes(){
	homa.logger.info("SOLAR", "Querying solar positions for " + settings[MQTT_TOPIC_LATITUDE] + ":"+ settings[MQTT_TOPIC_LONGITUDE]);
	var times = suncalc.getTimes(new Date(), settings[MQTT_TOPIC_LATITUDE], settings[MQTT_TOPIC_LONGITUDE]);
	homa.mqttHelper.publish("/devices/"+ argv.systemId + "/controls/Sunrise", homa.stringHelper.pad(times.sunrise.getHours(), 2, "0") +":"+homa.stringHelper.pad(times.sunrise.getMinutes(), 2, "0"), true);
	homa.mqttHelper.publish("/devices/"+ argv.systemId + "/controls/Sunset", homa.stringHelper.pad(times.sunset.getHours(), 2, "0")+":"+homa.stringHelper.pad(times.sunset.getMinutes(), 2, "0"), true);

	homa.mqttHelper.unschedulePublishes();
	var currentDate = new Date();
	for(key in times) {
		if(times[key] > currentDate) {
			homa.mqttHelper.schedulePublish(times[key], "/events/sun", key.toString(), false);
		}
	}

	homa.logger.info("SOLAR", "sunrise  (top edge of the sun appears on the horizon): "+times.sunrise);
	homa.logger.info("SOLAR", "sunriseEnd (bottom edge of the sun touches the horizon): "+times.sunriseEnd);
	homa.logger.info("SOLAR", "goldenHourEnd (morning golden hour ends): "+times.goldenHourEnd);
	homa.logger.info("SOLAR", "solarNoon (solar noon (sun is in the highest position):"+times.solarNoon);
 	homa.logger.info("SOLAR", "goldenHour (evening golden hour starts): "+times.goldenHour);
	homa.logger.info("SOLAR", "sunsetStart (bottom edge of the sun touches the horizon): "+times.sunsetStart);
	homa.logger.info("SOLAR", "sunset (sun disappears below the horizon, evening civil twilight starts): "+times.sunset);
	homa.logger.info("SOLAR", "dusk (evening nautical twilight starts): "+times.dusk );
	homa.logger.info("SOLAR", "nauticalDusk (evening astronomical twilight starts): "+times.nauticalDusk);
	homa.logger.info("SOLAR", "night (dark enough for astronomical observations): "+times.night );
	homa.logger.info("SOLAR", "nightEnd (morning astronomical twilight starts): "+times.nightEnd );
	homa.logger.info("SOLAR", "nauticalDawn (morning nautical twilight starts): "+times.nauticalDawn);
	homa.logger.info("SOLAR", "dawn (morning nautical twilight ends, morning civil twilight starts): "+times.dawn);
}