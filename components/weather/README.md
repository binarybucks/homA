# HomA - Weather
This component shows the current temperature in addition to morning, noon, and evening temperatures of today and tomorrow. 
For an API description see http://openweathermap.org/api. 

### Installation
```
$ npm install
```

### Start
Start the application and publish the latitude and longitude values of your home.
The default $SYSTEMID is ```383274-weather```.

``` 
$ ./solar [--brokerHost 127.0.0.1] [--brokerPort 1883] [--systemId $SYSTEMID]
$ publish.js -t "/sys/$SYSTEMID/latitude" -p "48.802545" -r
$ publish.js -t "/sys/$SYSTEMID/longitude" -p "9.226254" -r
```

The latitude and longitude values of your home can easily be obtained with the help of Google Maps. Just point it to the desired location, right-click the map and select _What is here?_. 

### Systemd
If your system supports it, you can start the application as a daemon from systemd by using the provided template.
```none
$ sudo ln -s $HOMA_BASEDIR/misc/systemd/homa@.service /etc/systemd/system/multi-user.target.wants/homa@weather.service
$ sudo systemctl --system daemon-reload
$ sudo systemctl start homa@weather.service
```

When running as a systemd service, additional parameters besides the brokerHost and brokerPort can be provided by a new configuration entry in /etc/homa/homa.conf
```
HOMA_COMPONENT_OPTS_weather="--systemId 'yourNewsystemId'"
```

Logs are then availble in the systemd journal 
```
$ sudo journalctl -u homa@weather.service -n 100 -f
```
