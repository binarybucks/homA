# HomA - Logger
This is a simple component that logs published messages. It can be used to monitor other components or as a starting point for custom components.  

### Installation
Install the required dependencies through npm
```none
$ npm install
```

### Usage
Start the application manually to output published messages to the current terminal
```none
$ ./logger [--brokerHost 127.0.0.1] [--brokerPort 1883]
```

### Systemd
If your system supports it, you can start the application as a daemon from systemd by using the provided template.
```none
$ sudo ln -s $HOMA_BASEDIR/misc/systemd/homa@.service /etc/systemd/system/multi-user.target.wants/homa@logger.service
$ sudo systemctl --system daemon-reload
$ sudo systemctl start homa@logger.service
```

Logs are then availble in the systemd journal 
```
$ sudo journalctl -u homa@logger.service -n 100 -f
```