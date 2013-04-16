#!/usr/bin/env node
var express = require('express');
var oauth = require('oauth');
var os = require("os");
var https= require('https');
var querystring = require('querystring');

var homa = require('homa');
var argv = homa.paramHelper.describe("systemId", "The unique client ID that determines where settings on the /sys topic are received")
												.describe("calendarQueryInterval", "The number of minutes between queries to the Google Calendar")
												.default("systemId", "458293-GoogleCalendarBridge")
												.default("calendarQueryInterval", 30).argv;

var clientId = "456464392453-ca2n79hj54jdqoana5oqh5rl632rvcu5.apps.googleusercontent.com";  // The Google API secrets are yet publicly shared. This might change in the future
var clientSecret	 = "W5SxdDqrlovnkf7f1WNwm4qw";
var accessToken, accessTokenRefreshIn, oa;
var settings = {};
var deviceCode;
var bootstrapCompleted = false;
var scheduled = false;
var calendarQueryInterval = argv.calendarQueryInterval*60*1000;

// Changing these might break things
var MQTT_TOPIC_SYS = "/sys/"+ argv.systemId + "/#"
var MQTT_TOPIC_CALENDAR_ID = "/sys/" + argv.systemId + "/calendarId";
var MQTT_TOPIC_REFRESH_TOKEN = "/sys/" + argv.systemId + "/refreshToken";

// Connects to the broker when the application is started
(function connect() {
	homa.mqttHelper.connect();
})();

// Subscribes to the sys topic to receive config values
homa.mqttHelper.on('connect', function(packet) {
	homa.mqttHelper.subscribe(MQTT_TOPIC_SYS);
});

// Called when a MQTT message is received. Calls oauth2bootstrap() when all required config parameters are received
homa.mqttHelper.on('message', function(packet) {
	settings[packet.topic] = packet.payload;
	if (bootstrapComplete() && !bootstrapCompleted) {
		bootstrapCompleted = true;
		homa.logger.info("CALENDAR", "Bootstrap completed. Waiting 5 seconds for refresh token");
		setTimeout(function () {oauth2bootstrap();} , 5*1000); // 5 seconds grace period to receive refresh token. Otherwise request authentication from user 
	}
});

// Checks if all required config parameters are received
function bootstrapComplete() {
		var pass = true
		var requiredItems = [MQTT_TOPIC_CALENDAR_ID];
		for(i=0;i<requiredItems.length;i++){
			if(settings[requiredItems[i]] == undefined || settings[requiredItems[i]] == ""){
				homa.logger.info("CALENDAR", "Waiting to receive setting: " + requiredItems[i] );
				pass = false;
			}
		}
		return pass;
}

// Checks if there is a saved OAuth refresh token or if the application has to be authorized to access the user's calendar
function oauth2bootstrap() {
	oa = new oauth.OAuth2(clientId, clientSecret, "https://accounts.google.com/o", "/oauth2/auth", "/oauth2/token");
	if(settings[MQTT_TOPIC_REFRESH_TOKEN] == undefined || settings[MQTT_TOPIC_REFRESH_TOKEN] == "" )
		oauth2authorize(); // No refresh token. Request autorization
	else
		oauth2refreshAccessToken(); // Refresh token present, Request a OAuth2 access token and start querying the calendar
}

// Requests authorization (using https://developers.google.com/accounts/docs/OAuth2ForDevices) to access the user's calendar. 
function oauth2authorize(){
	homa.logger.info("OAUTH2", "No refresh token provided. Requesting authorization from user.");
	// Create inital request to obtain a "user code"
	var initialRequestData = querystring.stringify({'client_id' : clientId, 'scope' : 'https://www.googleapis.com/auth/calendar'}); 
	var initialRequestOptions = {host: 'accounts.google.com', port : 443, path: '/o/oauth2/device/code', method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'Content-Length': initialRequestData.length}};
	var initialRequest = https.request(initialRequestOptions, function(response) {
	  response.setEncoding('utf8');
	  response.on('data', function (chunk) {
	    	var initialResponse = JSON.parse(chunk);
	    	// Query function that periodically checks if the user has entered the code and granted access already
	  		var query = function() {
					var secondaryRequestData = querystring.stringify({'client_id' : clientId, 'client_secret' : clientSecret, 'code' : initialResponse.device_code, 'grant_type' : 'http://oauth.net/grant_type/device/1.0'}); 
					var secondaryRequestOptions = {host: 'accounts.google.com', port : 443, path: '/o/oauth2/token', method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'Content-Length': secondaryRequestData.length}};						
					var secondaryRequest = https.request(secondaryRequestOptions, function(response) {
	  				response.setEncoding('utf8');
	  				response.on('data', function (chunk) {
	  						var secondaryResponse = JSON.parse(chunk);
	  						if(secondaryResponse.error) {	// User has not yet entered the code and granted access
	  							setTimeout(query, initialResponse.interval*1000+5000);
									homa.logger.info("OAUTH2", "Authorization pending. Please go to %s and enter the code: %s", initialResponse.verification_url, initialResponse.user_code);
	  						} else {	// User has entered the code to authorize this application. The response includes an initial OAuth access and refresh token.
									oauth2parseAccessToken(secondaryResponse);
	  						}
						});
	  			});
					secondaryRequest.on('error', function(e) {homa.logger.error('Secondary request returned: %s', e.message);});
					secondaryRequest.write(secondaryRequestData);  
					secondaryRequest.end();  
	  		}

	  		// Send query
	  		query();
	  });
	});

	initialRequest.on('error', function(e) {homa.logger.error('Initial request returned: %s', e.message);});
	initialRequest.write(initialRequestData);  
	initialRequest.end();  
}

// Parses a API response that contains an access token (and possible a refresh token)
// Schedules the refresh of the access token before it expires
// Also initiates the scheduling of regular calendar queries 
function oauth2parseAccessToken(results) {
  accessToken = results.access_token;
  accessTokenRefreshIn = !!results.expires_in ? (results.expires_in-600)*1000: accessTokenRefreshIn;
  settings[MQTT_TOPIC_REFRESH_TOKEN] = !!results.refresh_token ? results.refresh_token : settings[MQTT_TOPIC_REFRESH_TOKEN];

  homa.mqttHelper.publish(MQTT_TOPIC_REFRESH_TOKEN, settings[MQTT_TOPIC_REFRESH_TOKEN], true); // save refresh token on broker for future starts

  homa.logger.info("OAUTH", "Access token: " + accessToken);
 	homa.logger.info("OAUTH", "Access token refresh in: " + accessTokenRefreshIn+"ms");
  homa.logger.info("OAUTH", "Refresh token: " + settings[MQTT_TOPIC_REFRESH_TOKEN]);
 	homa.logger.info("OAUTH", "Token type: " + results.token_type);
 	setTimeout(oauth2refreshAccessToken, accessTokenRefreshIn);
 	if(!scheduled)
 		process.nextTick(calendarScheduleQuery);
}

// Refreshes the OAuth access token with an existing refresh token
function oauth2refreshAccessToken() {
	homa.logger.info("OAUTH", "Refreshing access token");
  oa.getOAuthAccessToken(settings[MQTT_TOPIC_REFRESH_TOKEN], {grant_type:'refresh_token'}, function(err, access_token, refresh_token, results) {
		if (err)
		  homa.logger.error("OAUTH", "Error: %s", JSON.stringify(err));
		else
			oauth2parseAccessToken(results);
		});
}

// Schedules regular calendar queries 
function calendarScheduleQuery(){
	setTimeout(calendarQuery, calendarQueryInterval);
	process.nextTick(calendarQuery);
}

// Queries the calendar API and schedules publishes depending on the returned events that start during the specified query intervall
function calendarQuery() {
	var timeMax = encodeURIComponent(new Date((new Date()).getTime()+ (calendarQueryInterval + (5*60*1000))).toISOString());
	var query = "https://www.googleapis.com/calendar/v3/calendars/"+settings[MQTT_TOPIC_CALENDAR_ID]+"/events?singleEvents=true&fields=items(id%2Cdescription%2Cstart%2Cend%2Csummary)&orderBy=startTime&timeMin="+encodeURIComponent(new Date().toISOString())+"&timeMax="+timeMax;	
	
	homa.logger.info("CALENDAR", "Executing query: " + query); 
	oa.get( query, accessToken, function (error, result, response) {
		if(error) {
			homa.logger.error("CALENDAR", "Error: %s", error); 
		} else {
			try {
				var items = JSON.parse(result).items
				if (items == undefined) {
					return;
				}
				homa.mqttHelper.unschedulePublishes();

			} catch (e) {
				return;
			}

			// Schedule events
			for(i=0;i<items.length;i++){
  			var item = items[i];
  			try {
					item.description = (item.description.substring(0,1) != '{' ? '{' : '') + item.description + (item.description.substring(item.description.length-1,item.description) != '}' ? '}' : '');
					try {
						var payload = JSON.parse(item.description);
					} catch (e) {
						homa.logger.error("CALENDAR", "Unable to parse event description: %s", item.description);
						homa.logger.error("CALENDAR", e);
						continue
					}
					// Schedule start events
					for(key in payload.start){
						homa.mqttHelper.schedulePublish(new Date(item.start.dateTime), key, payload.start[key], true); 
					}

					// Schedule end events
					for(key in payload.end){
						homa.mqttHelper.schedulePublish(new Date(item.end.dateTime), key, payload.end[key], true); 
					}
				} catch (e) {
					homa.logger.error("CALENDAR", "%s", e); 
					continue;
				}
    	}
		}
	});	
}