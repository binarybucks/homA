# HomA - Cleaner.js
This is a small Node.js command-line utility to quickly clean up retained MQTT messages. 

### Installation
```
$ npm install
```

### Start
``` 
$ node cleaner.js [--brokerHost 127.0.0.1] [--brokerPort 1883]
```

Input the number of the retained topic you would like to unpublish. New retained messages are added to the list automatically. 