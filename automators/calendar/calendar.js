#!/usr/bin/env node
var express = require('express');
var oauth = require('oauth');
var os = require("os");

var homa = require('homa');
var argv = homa.paramHelper.describe("systemId", "The unique client ID that determines where settings on the /sys topic are received")
												.describe("calendarQueryInterval", "The number of minutes between queries to the Google Calendar")
												.default("systemId", "458293-GoogleCalendarBridge")
												.default("calendarQueryInterval", 30).argv;

var clientId = "127336077993-68nj95v0g50cmp51ijcto80o3pfvmnfh.apps.googleusercontent.com";  // The Google API secrets are yet publicly shared. This might change in the future
var clientSecret	 = "SXiWh51Q9otWN4_CjY0Mtcm0";
var accessToken, accessTokenRefreshIn, oa;
var settings = {};
var bootstrapCompleted = false;
var calendarQueryInterval = argv.calendarQueryInterval*60*1000;


// Changing these might break things
var MQTT_TOPIC_SYS = "/sys/"+homa.argv.systemId + "/#"
var MQTT_TOPIC_CALENDAR_ID = "/sys/" + argv.systemId + "/calendarId";
var MQTT_TOPIC_REFRESH_TOKEN = "/sys/" + argv.systemId + "/refreshToken";

(function connect() {
	homa.mqttHelper.connect();
})();


homa.mqttHelper.on('connected', function(packet) {
	homa.mqttHelper.subscribe(MQTT_TOPIC_SYS);
});

homa.mqttHelper.on('receive', function(packet) {
	settings[packet.topic] = packet.payload;
	if (bootstrapComplete() && !bootstrapCompleted) {
		bootstrapCompleted = true;
		homa.logger.info("CALENDAR", "Bootstrap completed. Waiting 5 seconds for refresh token");
		setTimeout(function () {oauth2expressBotstrap(); oauth2bootstrap();} , 5*1000); // 5 seconds grace period to receive refresh token. Otherwise request authentication from user 
	}
});

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

function oauth2bootstrap() {
	oa = new oauth.OAuth2(clientId, clientSecret, "https://accounts.google.com/o", "/oauth2/auth", "/oauth2/token");
	if(settings[MQTT_TOPIC_REFRESH_TOKEN] == undefined || settings[MQTT_TOPIC_REFRESH_TOKEN] == "" ) {
		homa.logger.info("OAUTH", "Requesting new access- and refresh token");
		homa.logger.info("OAUTH", "No refresh token provided. \n            Please point your browser to: " +os.hostname()+":8553/");
	} else {
		oauth2refreshAccessToken();
	}
}

function oauth2getAccessTokenCallback(err, access_token, refresh_token, results){
	if (err) {
	    homa.logger.error("OAUTH", "Error: %s", JSON.stringify(err));
	} else {
	    accessToken = access_token;
	    accessTokenRefreshIn = !!results.expires_in ? (results.expires_in-600)*1000: accessTokenRefreshIn;
	    settings[MQTT_TOPIC_REFRESH_TOKEN] = !!refresh_token ? refresh_token : settings[MQTT_TOPIC_REFRESH_TOKEN];

	    homa.mqttHelper.publish(MQTT_TOPIC_REFRESH_TOKEN, settings[MQTT_TOPIC_REFRESH_TOKEN], true); // save refresh token on broker for future starts

	    homa.logger.info("OAUTH", "Access token: " + accessToken);
	   	homa.logger.info("OAUTH", "Access token refresh in: " + accessTokenRefreshIn+"ms");
	    homa.logger.info("OAUTH", "Refresh token: " + settings[MQTT_TOPIC_REFRESH_TOKEN]);
	   	homa.logger.info("OAUTH", "Token type: " + results.token_type);
	   	setTimeout(oauth2refreshAccessToken, accessTokenRefreshIn);
	   	process.nextTick(calendarQuery);
	}
}

function oauth2refreshAccessToken() {
		homa.logger.info("OAUTH", "Refreshing access token");
	  	oa.getOAuthAccessToken(settings[MQTT_TOPIC_REFRESH_TOKEN], {grant_type:'refresh_token'}, oauth2getAccessTokenCallback);
}


function oauth2expressBotstrap() {
	e = express();
	e.listen(8553);
	e.get('/callback', function(req, res) {
	  oa.getOAuthAccessToken(req.query.code, {grant_type:'authorization_code', redirect_uri:'http://localhost:8553/callback'}, oauth2getAccessTokenCallback); 
		res.send("OAUTH     Ok. To re authenticate, simply point your browser to "+os.hostname()+":8553/");

	});

	e.get('/', function(req, res) {
		url = oa.getAuthorizeUrl({scope:"https://www.google.com/calendar/feeds", approval_prompt:'force', access_type:'offline', response_type:'code', redirect_uri:'http://localhost:8553/callback'})
		res.redirect(url);
	});	
}

function calendarScheduleQuery(){
	setTimeout(calendarQuery, calendarQueryInterval);
	process.nextTick(calendarQuery);
}

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
					var payload = JSON.parse('{'+item.description+'}');

					// Schedule start events
					for(key in payload.start){
						homa.mqttHelper.schedulePublish(new Date(item.start.dateTime), key, payload.start[key], true); 
					}

					// Schedule end events
					for(key in payload.end){
						homa.mqttHelper.schedulePublish(new Date(item.end.dateTime), key, payload.end[key], true); 
					}
				} catch (e) {
					homa.logger.error("CALENDAR", "Error: %s", e); 
					continue;
				}
    	}
		}
	});	
}