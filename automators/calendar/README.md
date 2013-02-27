# HomA - Calendar.js
This is a Node.js Google Calendar to MQTT bridge that can publish to user defined topics at the start and end of an Google Calendar event. 

### Installation
```
$ npm install
```

### Start
``` 
$ node calendar.js [--brokerHost 127.0.0.1] [--brokerPort 1883] [--brokerClientId 458293-GoogleCalendarBridge] [--queryIntervall 60]
```
```
$ node publish.js [--brokerHost 127.0.0.1] [--brokerPort 1883] --topic "/sys/458293-GoogleCalendarBridge/calendarId" --payload "xxx@group.calendar.google.com"
```

To grant the application access to the Google Calendar, open the adress displayed by the application with a browser of your choice while logged in to a Google account that has access to the calendar. 

After successfull authentication, topic:payload tuples can be scheduled to be published at the start and end of an calendar event. 
A description of the following format can be added to events in the specified calendar.  
```
{"start":{"/topic":"payload", "/foo/bar":1},"end":{"/foo/bar/":0}}
```



