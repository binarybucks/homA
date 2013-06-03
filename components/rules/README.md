# HomA - Rules
This component evaluates rules defined in ```ruleset.nools``` in order to automate MQTT publishes based on conditions. 

### Installation
Install the required dependencies through npm
```none
$ npm install
```

### Usage
``` 
$ ./rules [--brokerHost 127.0.0.1] [--brokerPort 1883]
```

Rules are defined in the ruleset.nools file.
Further documentation about the rule syntax and usage is available at https://github.com/C2FO/nools.

New messages that are received on the MQTT bus are automatically asserted into the knowledge base so that your rules may act on them. 

In the ruleset, you have access to ```Message``` objects with the properties 
   * ```p``` : the payload of the message
   * ```t``` : the topic to which the payload was published
   * ```changed``` : whether the topic had a different value before
   * ```retained``` : wheter the message was a retained and originally published earlier 

Matched rules can be used to e.g. publish new messages by using the ```publish()``` function to trigger actions in different components of the system.
When a rule matches, it is a good idea to use the ```forget()``` function to retract the knowledge that made that rule fire from the knowledgebase, so that the rule does not match again immideately when new knowledge is asserted. 
Also, each time a new message is received, the ```Clock``` object is updated.


### Systemd
If your system supports it, you can start the application as a daemon from systemd by using the provided template.
```none
$ sudo ln -s $HOMA_BASEDIR/misc/systemd/homa@.service /etc/systemd/system/multi-user.target.wants/homa@rules.service
$ sudo systemctl --system daemon-reload
$ sudo systemctl start homa@rules.service
```

When running as a systemd service, additional parameters besides the brokerHost and brokerPort can be provided by a new configuration entry in /etc/homa/homa.conf
```
HOMA_COMPONENT_OPTS_solar="--systemId 'yourNewsystemId'"
```

Logs are then availble in the systemd journal 
```
$ sudo journalctl -u homa@rules.service -n 100 -f
```
