# HomA - Solar.js
This is a small Node.js command-line utility that shows the time of dawn and dusk in the framework's interfaces and publishes various other events to ```/events/sun``` based on the solar position.


### Installation
```
$ npm install
```

### Start
``` 
$ node rules.js [--brokerHost 127.0.0.1] [--brokerPort 1883] [--systemId $SYSTEMID]
$ publish.js -t "/sys/294028-solar/latitude" -p "48.802545" -r
$ publish.js -t "/sys/294028-solar/longitude" -p "9.226254" -r
```

The latitude and longitude values of your home can easily be obtained with the help of Google Maps. Just point it to the desired location, right-click the map and select _What is here?_. 

Enable service on systems that run systemd
```
$ sudo systemctl enable $HOMA_BASEDIR/components/solar/homa-solar.service
```