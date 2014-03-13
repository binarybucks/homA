# HomA - Solar
This component shows the time of dawn and dusk in the framework's interfaces and publishes various other events to ```/events/sun``` based on the solar position.


### Installation
```
$ npm install
```

### Start
Start the application and publish the latitude and longitude values of your home.
The default $SYSTEMID is ```294028-solar```.

``` 
$ ./solar [--brokerHost 127.0.0.1] [--brokerPort 1883] [--systemId $SYSTEMID]
$ publish.js -t "/sys/$SYSTEMID/latitude" -p "48.802545" -r
$ publish.js -t "/sys/$SYSTEMID/longitude" -p "9.226254" -r
```

The latitude and longitude values of your home can easily be obtained with the help of Google Maps. Just point it to the desired location, right-click the map and select _What is here?_. 

Please note that because of an issue with a 3rd party library homA is using, negative number payloads are not interpreted correctly. As a result, if your latitude or longitude is a negative value don't use the publish command above; instead you can either publish directly using mosquitto_pub:

``` 
mosquitto_pub -d -t "/sys/$SYSTEMID/longitude" -m -9.226254 -r
```

or use the following syntax:

``` 
$ publish.js -t "/sys/$SYSTEMID/longitude" --payload=-9.226254 -r

```

### Systemd
If your system supports it, you can start the application as a daemon from systemd by using the provided template.
```none
$ sudo ln -s $HOMA_BASEDIR/misc/systemd/homa@.service /etc/systemd/system/multi-user.target.wants/homa@solar.service
$ sudo systemctl --system daemon-reload
$ sudo systemctl start homa@solar.service
```

When running as a systemd service, additional parameters besides the brokerHost and brokerPort can be provided by a new configuration entry in /etc/homa/homa.conf
```
HOMA_COMPONENT_OPTS_solar="--systemId 'yourNewsystemId'"
```

Logs are then availble in the systemd journal 
```
$ sudo journalctl -u homa@solar.service -n 100 -f
```
