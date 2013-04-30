# HomA - Calendar
This component is a Google Calendar bridge that can publish to user defined topics at the start and end of an Google Calendar event. 

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

To grant the application access to the Google Calendar, open the adress displayed by the application with a browser of your choice while logged in to a Google account and enter the displayed code. 
The application will periodically check if you already entered the code after which it will begin to check the calendar automatically.  

After successfull authentication, you can start the application automatically from systemd by using the provided template.
```none
$ sudo ln -s $HOMA_BASEDIR/misc/systemd/homa@.service /etc/systemd/system/multi-user.target.wants/homa@calendar.service
$ sudo systemctl enable homa@calendar.service
$ sudo systemctl --system daemon-reload
$ sudo systemctl start homa@calendar.service
```

Logs are then availble in the systemd journal 
```
$ sudo journalctl -u homa@calendar.service -n 100 -f
```

When running topic:payload tuples can be scheduled to be published at the start and end of an calendar event. 
A description of the following format can be added to events in the specified calendar. 
```
"start":{"/topic":"payload", "/foo/bar":1},"end":{"/foo/bar/":0}
```

