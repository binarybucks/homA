var express = require('express');
var oauth = require('oauth');
var mqtt = require('mqttjs')
var os = require("os");


var accessToken;
var accessTokenRefreshIn;
var refreshToken = "1/tWipKs9lwgGI2hKPOWT8Qbnd4ueBFkQ6TcRtTE_dIGw";
var oa;
var clientId = "127336077993-68nj95v0g50cmp51ijcto80o3pfvmnfh.apps.googleusercontent.com";
var clientSecret	 = "SXiWh51Q9otWN4_CjY0Mtcm0";
var mqttBrokerHost = "192.168.8.2";
var mqttBrokerPort = 1883;
var mqttClient;
var calendarQueryInterval = 3600*2;


function mqttBootstrap() {
	mqtt.createClient(mqttBrokerPort, mqttBrokerHost, function(err, client) {
	  if (err) process.exit(1);
	  client.connect({keepalive: 3000});
	  mqttClient = client;

	  client.on('connack', function(packet) {
	    if (packet.returnCode === 0) {
	    	console.log('MQTT: Connection established');

	    } else {
	      console.log('MQTT: Connack error %d', packet.returnCode);
	      process.exit(-1);
	    }
	  });

	  client.on('close', function() {
	    process.exit(0);
	  });

	  client.on('error', function(e) {
	    console.log('MQTT Error: %s', e);
	    process.exit(-1);
	  });
	});
}


function oauth2bootstrap() {
	oa = new oauth.OAuth2(clientId, clientSecret, "https://accounts.google.com/o", "/oauth2/auth", "/oauth2/token");
	if(refreshToken == "") {
		console.log("Requesting new oauth2 access and refresh token");
		console.log("No refresh token provided. \nPlease point your browser to: " +os.hostname()+":8553/");
	} else {
		oauth2refreshAccessToken();
	}
}

function oauth2getAccessTokenCallback(err, access_token, refresh_token, results){
	if (err) {
	    console.log('Error: ' + JSON.stringify(err));
	} else {
	    accessToken = access_token;
	    accessTokenRefreshIn = !!results.expires_in ? (results.expires_in-600)*1000: accessTokenRefreshIn;
	    refreshToken = !!refresh_token ? refresh_token : refreshToken;
	    console.log('access token: ' + accessToken);
	    console.log('access token refresh in: ' + accessTokenRefreshIn+"ms");
	    console.log('refresh token: ' + refreshToken);
	   	console.log('token type: ' + results.token_type);
	   	setTimeout(oauth2refreshAccessToken, accessTokenRefreshIn);
	   			process.nextTick(calendarQuery);

	}
}

function oauth2refreshAccessToken() {
		console.log("Refreshing oauth2 access token");
	  	oa.getOAuthAccessToken(refreshToken, {grant_type:'refresh_token'}, oauth2getAccessTokenCallback);
}


function oauth2expressBotstrap() {
	e = express();
	e.listen(8553);
	e.get('/callback', function(req, res) {
	  oa.getOAuthAccessToken(req.query.code, {grant_type:'authorization_code', redirect_uri:'http://localhost:8553/callback'}, oauth2getAccessTokenCallback); 
		res.send("Ok. To re authenticate, simply point your browser to "+os.hostname()+":8553/");

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
	// oa._headers['GData-Version'] = '3.0'; 

	oa.getProtectedResource(
		"https://www.google.com/calendar/feeds/default/allcalendars/full?alt=jsonc", 
		"GET", 
		accessToken, 
		refreshToken,
		function (error, data, response) {

			var feed = JSON.parse(data);
			console.log("Received: " + feed);
	});


}

mqttBootstrap();
oauth2expressBotstrap();
oauth2bootstrap();






