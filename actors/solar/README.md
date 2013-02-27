# HomA - Solar.js
This is a small Node.js command-line utility that shows the time of dawn and dusk in the framework's interfaces and publishes various other events to ```/events/sun``` based on the solar position.


### Installation
```
$ npm install
```

### Start
``` 
$ node rules.js (--latitude "latitude"] [--longitude "longitude"]
```

The latitude and longitude values of your home can easily be obtained with the help of Google Maps. Just point it to the desired location, right-click the map and select _What is here?_. 