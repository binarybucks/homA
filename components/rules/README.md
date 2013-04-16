# HomA - Rules.js
This is a small Node.js command-line utility that evaluates rules in order to automate MQTT publishes based on conditions. 

### Installation
```
$ npm install
```

### Start
``` 
$ node rules.js [--brokerHost 127.0.0.1] [--brokerPort 1883]
```

Rules can be defined in the ruleset.nools file. Further documentation about rule syntax and usage is available at https://github.com/C2FO/nools.

Enable service on systems that run systemd
```
$ sudo systemctl enable $HOMA_BASEDIR/components/rules/homa-rules.service
```