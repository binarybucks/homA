# HomA - Node module
This is a small module that provides wrapper functions and tools for components of the HomA smart home framework. 

This module provides a common command-line syntax to HomA components that are based on this module. Per default only the broker host and port have to be provided by the user in order to start a component that is based on this module. The parameters can either be supplied with the parameters ```--brokerHost``` and ```--brokerPort``` or they are read from the environment variables ```HOMA_BROKER_HOST``` and ```HOMA_BROKER_PORT```. If both are provided, the command-line parameters take precedence. 

According to the [wiki](https://github.com/binarybucks/homA/wiki/Conventions#settings) all further configuration should be saved in an MQTT topic according to the deviceId of the component. The settingsHelper that is provided by this module offers the necessary functions for this task.  

If no [systemId](https://github.com/binarybucks/homA/wiki/Conventions#systemids) is provided through the ```--systemId``` parameter, the component's default systemId is used. If you are only using a single instance of a component there shouldn't be aby need to use a different systemId than the default one. 

When writing a new component based on this module, the default systemId can be provided and acquired through 
```
#!/usr/bin/env node
var homa = require('homa');
var systemId = homa.paramsWithDefaultSystemId("391349-logger");
```
This requires this module, sets the default systemId to 391349-logger and finishes the parameters so that they can later be accessed through ```homa.params.paramKey```. 


## mqttHelper
Provides helper methods and wraps functions of the mqtt node module.

* ```connect(host, port)``` Connects to the MQTT broker
* ```publish(topic, payload, retained)``` Publishes a payload to the specified topic retained or non-retained
* ```schedulePublish(date, topic, payload, retained)``` calls publish(...) with the specified parameters at date
* ```unschedulePublishes()``` Unschedules all previously scheduled publishes
* ```disconnect()``` Disconnects form the broker
* ```subscribe(topic)``` Subscribes to a MQTT topic
* ```unsubscibe(topic)``` Unsubscribes from a previously subscribed MQTT topic

It also emits the following events



## stringHelper
Provides helper methods for string formating
* ```pad(n, width, symbol, back)``` Adds symbols to the beginning or enf of string n until it reaches the specified width. 

## settings
Provides functions to store [settings](https://github.com/binarybucks/homA/wiki/Conventions#settings) in an MQTT topic according to the systemId. The general idea behind this is to store everything that is not required to connect to the MQTT broker in a topic of the format /sys/$deviceId/parameter. After connecting to the broker, the module will subscribe to all parameter that are marked as required or optional. 

In the ```message``` event emitted by the mqttHelper, one can use the ```insert(packet.topic, packet.payload)``` function to add any received message into the current settings. If the topic matches the /sys topic format described eariler and the topic parameters is required or optional it can later be used similar to a normal config parameter. 

Also, one can use the ```isBootstrapCompleted()``` to check if all required parameters were received from the broker. To mark a certain parameter key as required, use the provided function ```require(parameter)```. Optional paramters whose values should be retreived can be denoted with the ```optional(parameter)``` funcion. 

To save a value of a parameter at the correct MQTT topic, one can use the ```save(parameter, value)``` function. Additionally, parameter values that were previously received and inserted can be retreived to use them with the help of the ```get(parameter)``` function. This will return the value of the paramter if a message for it was received and inserted. 

The following provides a basic example to use the settingsHelper to read and write two settings from the /sys topic.

```
var homa = require('homa') // Require module
var systemId = homa.paramsWithDefaultSystemId("123456-test"); // /sys topic format is /sys/123456-test/parmeter
						
// Connect to the broker						
(function connect() {
    homa.mqttHelper.connect();
})();

// Called after the connection to the broker is established
// Subscribes to "/sys/123456-test/parmeter1" and "/sys/123456-test/parmeter2"
// but homa.settings.isBootstrapCompleted() will return true after homa.settings.insert("/sys/123456-test/parmeter1", "value") was called 

homa.mqttHelper.on('connect', function(packet) {	
		homa.settings.require('parameter1'); // Set required settings
		homa.settings.optional('parameter2');
});

// Called when a MQTT message for a subscribed topic is received
homa.mqttHelper.on('message', function(packet) {
	homa.settings.insert(packet.topic, packet.payload); // Check if a required settings parameter was received. If so, save it for later. 

	// Check if all required values are received. If so, the parameter bootstrap is completed and the normal code execution can start. 
	// The isLocked() and lock() functions prevent that the inner code is executed more than once, e.g. when a required or optional parameter is published to the according topic again

	if (!homa.settings.isLocked() && homa.settings.isBootstrapCompleted()) {
		homa.settings.lock(); // Prevent this code form being executed again
		console.log("Hello world, parameter1 is: %s, parameter2 (if received) is: %s", homa.settings.get("parmeter1"), homa.settings.get("parameter2"));

		// Save some value for parameter2
		homa.settings.save("parameter2", "some value");
	}
});

```

## logger
Exposes an [npmlog](https://npmjs.org/package/npmlog) instance

## params
Exposes an [optimist](https://npmjs.org/package/optimist) instance. Note that before accessing any values paramsWithDefaultSystemId(systemId) has to be called. 

