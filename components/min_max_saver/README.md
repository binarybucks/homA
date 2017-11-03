# HomA - min/max saver
This component is a universal min/max saver used by HomA framework.

### Basic Requirements
* At least a running component that publishes values, those min or max values are of interest.

### Installation
* Create a ```mqtt_config.py``` file with this content (modify as needed):
```none
host = "localhost"
port = 1883
user = ""
pwd = ""
ca_certs = ""
```
* Modify ```setup.py``` to your needs, e.g. config the min/max saver here.

### Usage
Run the basic setup once
```none
$ ./setup.py
```

Start the application manually 
```none
$ ./min_max_saver
```

Optional: To quickly add an additional min/max saver use
```
$ publish.js [--brokerHost 127.0.0.1] [--brokerPort 1883] --topic "/sys/CLIENTID/min/<minSystemId>/<minControlId>" --payload "24"
```
or configure it in setup.py

### Systemd
If your system supports it, you can start the application as a daemon from systemd by using the provided template.
```none
$ sudo ln -s $HOMA_BASEDIR/misc/systemd/homa@.service /etc/systemd/system/multi-user.target.wants/homa@min_max_saver.service
$ sudo systemctl --system daemon-reload
$ sudo systemctl start homa@min_max_saver.service
```

Logs are then availble in the systemd journal 
```
$ sudo journalctl -u homa@min_max_saver.service -n 100 -f
```
