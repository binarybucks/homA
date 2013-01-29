var express = require('express');
var oauth = require('oauth');
var mqtt = require('mqttjs')
var os = require("os");


var accessToken;
var accessTokenRefreshIn;
var refreshToken = "1/tWipKs9lwgGI2hKPOWT8Qbnd4ueBFkQ6TcRtTE_dIGw";
var oa;
var clientId = "127336077993-68nj95v0g50cmp51ijcto80o3pfvmnfh.apps.googleusercontent.com";
var client secret = "SXiWh51Q9otWN4_CjY0Mtcm0";


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

mqttBootstrap();
oauth2expressBotstrap();
oauth2bootstrap();






