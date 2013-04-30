# HomA - Logger.js
This is a command-line utility that logs published messages. 

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

Alternatively, you can start the application automatically from systemd by using the provided template.
```none
$ sudo ln -s $HOMA_BASEDIR/misc/systemd/homa@.service /etc/systemd/system/multi-user.target.wants/homa@logger.service
$ sudo systemctl enable homa@logger.service
$ sudo systemctl start homa@logger.service
```

Logs are then availble in the systemd journal 
```
$ sudo journalctl -u homa@logger.service -n 100 -f
```



