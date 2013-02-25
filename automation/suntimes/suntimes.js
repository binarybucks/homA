#!/usr/bin/env node
var date = require("datejs")
var schedule = require('node-schedule');
var client = require('homa-mqttjs');
var suncalc = require('suncalc');

		client.argv = client.argv.describe("latitude", "Latitude at current location")
												.describe("longitude", "Longitude at current location")
												.demand("latitude")
												.demand("longitude")
												.alias("latitude", "lat")
												.alias("longitude", "long")
												.argv;


var events = [];


(function(){
client.connect();
var rule = new schedule.RecurrenceRule();
rule.hour = 0;
var j = schedule.scheduleJob(rule, querySuntimes);
querySuntimes();
})();


function schedulePublish(date, topic, payload){
	console.log("Schedule    At "  + date + " publishing " + topic + ":" + payload +"\n");
	var job = schedule.scheduleJob(date, function(){
			client.publish(topic, payload, false);
	});
	events.push(job);

}

function querySuntimes(){
	console.log("SUNTIMES    Querying suntimes for " + client.argv.latitude + ":"+client.argv.longitude);
	var times = suncalc.getTimes(new Date(), client.argv.latitude,client.argv.longitude);


	// Unschedule all events
	for (var i=0; i<events.length; i++) {
		events[i].cancel();
	}
	console.log("sunrise  (top edge of the sun appears on the horizon): "+times.sunrise);
	schedulePublish(times.sunrise, "/events/sun", "sunrise");

	console.log("sunriseEnd (bottom edge of the sun touches the horizon): "+times.sunriseEnd);
	schedulePublish(times.sunriseEnd, "/events/sun", "sunriseEnd");

	console.log("goldenHourEnd (morning golden hour ends): "+times.goldenHourEnd);
	schedulePublish(times.goldenHourEnd, "/events/sun", "goldenHourEnd");

	console.log("solarNoon (solar noon (sun is in the highest position):"+times.solarNoon);
	schedulePublish(times.solarNoon, "/events/sun", "solarNoon");

 	console.log("goldenHour (evening golden hour starts): "+times.goldenHour);
	schedulePublish(times.goldenHour, "/events/sun", "goldenHour");

	console.log("sunsetStart (bottom edge of the sun touches the horizon): "+times.sunsetStart);
	schedulePublish(times.sunsetStart, "/events/sun", "sunsetStart");

	console.log("sunset (sun disappears below the horizon, evening civil twilight starts): "+times.sunset);
	schedulePublish(times.sunset, "/events/sun", "sunset");

	console.log("dusk (evening nautical twilight starts): "+times.dusk);
	schedulePublish(times.dusk, "/events/sun", "dusk");

	console.log("nauticalDusk (evening astronomical twilight starts): "+times.nauticalDusk);
	schedulePublish(times.nauticalDusk, "/events/sun", "nauticalDusk");

	console.log("night (dark enough for astronomical observations): "+times.night);
	schedulePublish(times.night, "/events/sun", "night");

	console.log("nightEnd (morning astronomical twilight starts): "+times.nightEnd);
	schedulePublish(times.nightEnd, "/events/sun", "nightEnd");

	console.log("nauticalDawn (morning nautical twilight starts): "+times.nauticalDawn);
	schedulePublish(times.nauticalDawn, "/events/sun", "nauticalDawn");

	console.log("dawn (morning nautical twilight ends, morning civil twilight starts): "+times.dawn);
	schedulePublish(times.dawn, "/events/sun", "dawn");


}

