# HomA - Publish.js
This is a small Node.js command-line utility to publish a single message to an MQTT broker. 

### Installation
```
$ npm install
```
### Usage
```
$ node publish.js [--brokerHost 127.0.0.1] [--brokerPort 1883] [--topic "/foo/bar"] [--payload "baz"] [--retained]
```

Arguments in [] are optional. If not specified via command-line parameters, topic and payload are read from stdin.  
