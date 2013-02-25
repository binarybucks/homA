# HomA - Publish.js
This is a small Node.js command-line utility to publish a single message to an MQTT broker. 

### Installation
```
$ npm install
```

### Usage
```
$ node publish.js --brokerHost localhost [--brokerPort 1883] [--retained | -e] (--topic | -t) "/foo" (--payload | -p) "bar" 
```

Arguments in [] are optional. 
Additionally, brokerHost and brokerPort can be specified through the environment variables HOMA_BROKER_HOST and HOMA_BROKER_PORT for all HomA command-line applications. 
When provided through an environment variable, the corresponding command-line parameter becomes optional. However, it takes precedence over an environment variable when both are set. 
