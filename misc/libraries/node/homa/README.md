# HomA - Node modly
This is a small module that provides wrapper functions and tools for components of the HomA smart home framework 

## Functionality 
* MqttHelper Wrapper around the mqtt module
* StringHelper Provides string formating functions
* ParamHelper Wrapper around optimist. Provides a common command-line syntax for MQTT parameters
* logger Exposes an npmlog instance
* scheduler Exposes a node-schedule instance


To add custom command-line parameters use 
```var argv = homa.paramHelper.describe("paramName", "description").default("systemId", "458293-GoogleCalendarBridge").argv;```

Mind the final .argv that is required by the optimist module to parse the parameters. Parameters can be accessed through ```argv.paramName```

