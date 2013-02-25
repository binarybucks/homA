#!/usr/bin/env node
var date = require("datejs")
var suncalc = require('suncalc');
var client = require('homa-mqttjs');
		client.argv = client.argv.describe("latitude", "Latitude at current location")
												.describe("longitude", "Longitude at current location")
												.demand("latitude")
												.demand("longitude")
												.alias("latitude", "lat")
												.alias("longitude", "long")
												.argv;

(function(){
	client.connect();
})();

client.events.on('connected', function(packet) {
	var rule = new client.scheduler.RecurrenceRule();
	rule.hour = 0;
	var j = client.scheduler.scheduleJob(rule, querySuntimes);
	querySuntimes();

	client.publish("/devices/294028-solar/controls/Sunset/type", "text", true);
	client.publish("/devices/294028-solar/controls/Sunrise/type", "text", true);
});


function querySuntimes(){
	console.log("SOLAR       Querying solar positions for " + client.argv.latitude + ":"+client.argv.longitude);
	var times = suncalc.getTimes(new Date(), client.argv.latitude,client.argv.longitude);

	client.unschedulePublishes();

	for(key in times) {
		client.schedulePublish(times[key], "/events/sun", key.toString(), false);
	}

	client.publish("/devices/294028-solar/controls/Sunrise", client.pad(times.sunrise.getHours(), 2, "0") +":"+client.pad(times.sunrise.getMinutes(), 2, "0"), true);
	client.publish("/devices/294028-solar/controls/Sunset", client.pad(times.sunset.getHours(), 2, "0")+":"+client.pad(times.sunset.getMinutes(), 2, "0"), true);

	console.log("SOLAR       sunrise  (top edge of the sun appears on the horizon): "+times.sunrise + "\n" + 
	 						"SOLAR       sunriseEnd (bottom edge of the sun touches the horizon): "+times.sunriseEnd + "\n" + 
							"SOLAR       goldenHourEnd (morning golden hour ends): "+times.goldenHourEnd + "\n" + 
							"SOLAR       solarNoon (solar noon (sun is in the highest position):"+times.solarNoon + "\n" + 
 							"SOLAR       goldenHour (evening golden hour starts): "+times.goldenHour + "\n" + 
							"SOLAR       sunsetStart (bottom edge of the sun touches the horizon): "+times.sunsetStart + "\n" + 
							"SOLAR       sunset (sun disappears below the horizon, evening civil twilight starts): "+times.sunset + "\n" + 
							"SOLAR       dusk (evening nautical twilight starts): "+times.dusk + "\n" + 
							"SOLAR       nauticalDusk (evening astronomical twilight starts): "+times.nauticalDusk + "\n" + 
							"SOLAR       night (dark enough for astronomical observations): "+times.night + "\n" + 
							"SOLAR       nightEnd (morning astronomical twilight starts): "+times.nightEnd + "\n" + 
							"SOLAR       nauticalDawn (morning nautical twilight starts): "+times.nauticalDawn + "\n" + 
							"SOLAR       dawn (morning nautical twilight ends, morning civil twilight starts): "+times.dawn);
}