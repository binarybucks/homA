# HomA - Csm
This component extracts the average power usage from a custom VOLTCRAFT® VSM-100 smartmeter API and displays it in the framework's interfaces.

### Installation
```
$ npm install
```

### Start
Start the application and publish the API URL.
The default $SYSTEMID is ```194729-csm```.

``` 
$ ./solar [--brokerHost 127.0.0.1] [--brokerPort 1883] [--systemId $SYSTEMID]
$ publish.js -t "/sys/$SYSTEMID/apiUrl" -p "http://192.168.8.3:8000/csm/stuff/?Test" -r
```

### API
The API is expected to return JSON smartmeter readings for a GET HTTP reguest in the following format. 
```none
{"Sum": [242.4, 242.6, 243.5, 242.6, 243.3, 243.0, 242.6, 242.6, 242.7, 242.3, 244.0, 243.3, 242.7, 242.9, 242.1, 242.8, 242.3, 243.9, 242.5, 242.6]}
```
For the smartmeter in question every entry is the average usage on all three phases in the last 30 seconds. The API is expected to return the most recent 20 values while providing them is currently left as an exercise to the reader as every smartmeter is different. 


### Systemd
If your system supports it, you can start the application as a daemon from systemd by using the provided template.
```none
$ sudo ln -s $HOMA_BASEDIR/misc/systemd/homa@.service /etc/systemd/system/multi-user.target.wants/homa@csm.service
$ sudo systemctl enable homa@csm.service
$ sudo systemctl --system daemon-reload
$ sudo systemctl start homa@csm.service
```

When running as a systemd service, additional parameters besides the brokerHost and brokerPort can be provided by a new configuration entry in /etc/homa/homa.conf
```
HOMA_COMPONENT_OPTS_csm="--systemId 'yourNewsystemId'"
```

Logs are then availble in the systemd journal 
```
$ sudo journalctl -u homa@csm.service -n 100 -f
```
