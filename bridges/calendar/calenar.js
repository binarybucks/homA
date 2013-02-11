#!/usr/bin/env node

var express = require('express');
var oauth = require('oauth');
var mqtt = require('mqttjs')
var os = require("os");
var schedule = require('node-schedule');
var argv = require('optimist').argv;


// Can be edited savely
var MQTT_CLIENT_ID ='458293-GoogleCalendarBridge'
var MQTT_BROKER_PORT = 1883
var MQTT_BROKER_HOST = 'localhost'
var CALENDAR_QUERY_INTERVALL = 3600*2;

var clientId = "127336077993-68nj95v0g50cmp51ijcto80o3pfvmnfh.apps.googleusercontent.com";  // The Google API secrets are yet publicly shared. This might change in the future
var clientSecret	 = "SXiWh51Q9otWN4_CjY0Mtcm0";

var accessToken;
var accessTokenRefreshIn;
var oa;
var mqttClient;
var events = [];
var settings = {};
var bootstrapCompleted = false;




function mqttBootstrap() {
	mqtt.createClient(MQTT_BROKER_PORT, MQTT_BROKER_HOST, function(err, client) {
	  if (err) {
	  	console.log('MQTT        %s', err);
	  	process.exit(1);
	  }
	  client.connect({keepalive: 3000});
	  mqttClient = client;

	  client.on('connack', function(packet) {
	    if (packet.returnCode === 0) {
	    	console.log('MQTT        Connection established');
	    	client.subscribe({topic: "/sys/" + MQTT_CLIENT_ID + "/#"});
	    } else {
	      console.log('MQTT        Connack error %d', packet.returnCode);
	      process.exit(-1);
	    }
	  });

	  client.on('close', function() {
	  	console.log('MQTT        Connection closed');
	    process.exit(-1);
	  });

	  client.on('error', function(e) {
	    console.log('MQTT        Error: %s', e);
	    process.exit(-1);
	  });

	 	client.on('publish', function(packet) {
	 		console.log("MQTT        Received: " + packet.topic + ":" + packet.payload);
	 		settings[packet.topic] = packet.payload;
	 		if (bootstrapComplete() && !bootstrapCompleted) {
	 			bootstrapCompleted = true;
	 			console.log("Settings    Bootstrap completed. Waiting 5 seconds for refresh token");
				setTimeout(function () {oauth2expressBotstrap(); oauth2bootstrap();} , 5*1000); // 5 seconds grace period to receive refresh token. Otherwise request authentication from user 
			}
		});
	});
}


function bootstrapComplete() {
		var pass = true
		var requiredItems = [MQTT_TOPIC_CALENDAR_ID];
		for(i=0;i<requiredItems.length;i++){
			if(settings[requiredItems[i]] == undefined || settings[requiredItems[i]] == ""){
				console.log("Settings    Not yet received: " + requiredItems[i] )
				pass = false;
			}
		}
		return pass;
}

function oauth2bootstrap() {
	oa = new oauth.OAuth2(clientId, clientSecret, "https://accounts.google.com/o", "/oauth2/auth", "/oauth2/token");
	if(settings[MQTT_TOPIC_REFRESH_TOKEN] == undefined || settings[MQTT_TOPIC_REFRESH_TOKEN] == "" ) {
		console.log("OAUTH       Requesting new access- and refresh token");
		console.log("OAUTH       No refresh token provided. \n            Please point your browser to: " +os.hostname()+":8553/");
	} else {
		oauth2refreshAccessToken();
	}
}

function oauth2getAccessTokenCallback(err, access_token, refresh_token, results){
	if (err) {
	    console.log('Error       ' + JSON.stringify(err));
	} else {
	    accessToken = access_token;
	    accessTokenRefreshIn = !!results.expires_in ? (results.expires_in-600)*1000: accessTokenRefreshIn;
	    settings[MQTT_TOPIC_REFRESH_TOKEN] = !!refresh_token ? refresh_token : settings[MQTT_TOPIC_REFRESH_TOKEN];

	    mqttPublish(MQTT_TOPIC_REFRESH_TOKEN, settings[MQTT_TOPIC_REFRESH_TOKEN]); // save refresh token on broker for future starts

	    console.log('OAUTH       Access token: ' + accessToken);
	    console.log('OAUTH       Access token refresh in: ' + accessTokenRefreshIn+"ms");
	    console.log('OAUTH       Refresh token: ' + settings[MQTT_TOPIC_REFRESH_TOKEN]);
	   	console.log('OAUTH       Token type: ' + results.token_type);
	   	setTimeout(oauth2refreshAccessToken, accessTokenRefreshIn);
	   	process.nextTick(calendarQuery);
	}
}

function oauth2refreshAccessToken() {
		console.log("OAUTH       Refreshing access token");
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
	setTimeout(calendarQuery, CALENDAR_QUERY_INTERVALL);
	process.nextTick(calendarQuery);
}

function calendarSchedule(date, topic, payload){
	console.log("Calendar    At "  + date + " publishing " + topic + ":" + payload );
	var job = schedule.scheduleJob(date, function(){
			mqttPublish(topic, payload);
	});
}

function calendarQuery() {
	var query = "https://www.googleapis.com/calendar/v3/calendars/"+settings[MQTT_TOPIC_CALENDAR_ID]+"/events?singleEvents=true&fields=items(id%2Cdescription%2Cstart%2Cend%2Csummary)&orderBy=startTime&timeMin="+encodeURIComponent(new Date().toISOString());	
	console.log("Calendar    Executing query: " + query);

	oa.get( query, accessToken, function (error, result, response) {
		if(error) {
			console.log("Error       " + error);
		} else {
			try {
				var items = JSON.parse(result).items
			} catch (e) {
				return;
			}

			// Unschedule all events
			for (var i=0; i<events.length; i++) {
				events[i].cancel();
			}

			// Schedule events
			for(i=0;i<items.length;i++){
  			var item = items[i];
  			try {
					var payload = JSON.parse('{'+item.description+'}');

					for(key in payload.start){
						calendarSchedule(new Date(item.start.dateTime), key, payload.start[key]);
					}

					
					for(key in payload.end){
						calendarSchedule(new Date(item.end.dateTime), key, payload.end[key]);
					}

				} catch (e) {
					console.log(e)
					continue;
				}
    	}
		}
	});	
}

function mqttPublish(topic, payload) {
			console.log("publishing " + topic + ":" + payload);
			mqttClient.publish({ topic: topic.toString(), payload: payload.toString(), qos: 0, retain: true});
	
}



if (argv.brokerHost != undefined) {
	MQTT_BROKER_HOST = argv.brokerHost;
}
if (argv.brokerPort != undefined) {
	MQTT_BROKER_PORT = argv.brokerPort;
}
if (argv.brokerClientId != undefined) {
	MQTT_CLIENT_ID = argv.brokerClientId;
}
if (argv.queryIntervall != undefined) {
	CALENDAR_QUERY_INTERVALL = argv.queryIntervall*60;
}
console.log(MQTT_CLIENT_ID);
// Don't touch these unless you know what they're doing
var MQTT_TOPIC_CALENDAR_ID = "/sys/" + MQTT_CLIENT_ID + "/calendarId";
var MQTT_TOPIC_REFRESH_TOKEN = "/sys/" + MQTT_CLIENT_ID + "/refreshToken";

mqttBootstrap();	







