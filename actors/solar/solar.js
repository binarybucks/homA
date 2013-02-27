#!/usr/bin/env node
var date = require("datejs")
var suncalc = require('suncalc');
var homa = require('homa');
		homa.argv = homa.argv.describe("latitude", "Latitude at current location")
												.describe("longitude", "Longitude at current location")
												.demand("latitude")
												.demand("longitude")
												.alias("latitude", "lat")
												.alias("longitude", "long")
												.argv;

(function(){
	homa.mqttHelper.connect();
})();

homa.mqttHelper.on('connected', function(packet) {
	var rule = new homa.scheduler.RecurrenceRule();
	rule.hour = 0;
	var j = homa.scheduler.scheduleJob(rule, function(){querySuntimes();});
	console.log(j);
	querySuntimes();

	homa.mqttHelper.publish("/devices/294028-solar/controls/Sunset/type", "text", true);
	homa.mqttHelper.publish("/devices/294028-solar/controls/Sunrise/type", "text", true);
});


function querySuntimes(){
	homa.logger.info("SOLAR", "Querying solar positions for " + homa.argv.latitude + ":"+homa.argv.longitude);
	var times = suncalc.getTimes(new Date(), homa.argv.latitude,homa.argv.longitude);

	homa.mqttHelper.unschedulePublishes();

	for(key in times) {
		homa.mqttHelper.schedulePublish(times[key], "/events/sun", key.toString(), false);
	}

	homa.mqttHelper.publish("/devices/294028-solar/controls/Sunrise", homa.stringHelper.pad(times.sunrise.getHours(), 2, "0") +":"+homa.stringHelper.pad(times.sunrise.getMinutes(), 2, "0"), true);
	homa.mqttHelper.publish("/devices/294028-solar/controls/Sunset", homa.stringHelper.pad(times.sunset.getHours(), 2, "0")+":"+homa.stringHelper.pad(times.sunset.getMinutes(), 2, "0"), true);

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