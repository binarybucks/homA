# HomA - Calendar.js
This is a Google Calendar to MQTT bridge that can publish to user defined topics at the start and end of an Google Calendar event. 

### Installation
When you have followed the general [installation instructions](https://github.com/binarybucks/homA/wiki/Installation) run
```
$ npm install
```

### Usage
Start the application and publish the calendar id of the calendar that contains your automation events.
``` 
$ calendar.js [--brokerHost 127.0.0.1] [--brokerPort 1883] [--systemId $SYSTEMID] [--queryIntervall 60]
$ publish.js [--brokerHost 127.0.0.1] [--brokerPort 1883] --topic "/sys/$SYSTEMID/calendarId" --payload "xxx@group.calendar.google.com" --retained
```

To grant the application access to the Google Calendar, open the adress displayed by the application with a browser of your choice while logged in to a Google account that has access to the calendar. 

When you are running the application on an other host than your local computer, the automatic redirect after you authroized the application will fail. 
In that case, copy the URL you have been redirected to and open it on the host that is running the application. In case that host does not have a graphical shell installed
you can use a command-line tool like wget ```$ wget http://localhost:8553/callback?code=PUT_YOUR_CALLBACK_CODE_HERE```

After successfull authentication, topic:payload tuples can be scheduled to be published at the start and end of an calendar event. 
A description of the following format can be added to events in the specified calendar. 
```
"start":{"/topic":"payload", "/foo/bar":1},"end":{"/foo/bar/":0}
```



