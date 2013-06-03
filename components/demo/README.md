# HomA - Demo
This is a very simple component that adds a device with three controls (switch, range, text) to the interface. 
You can use it as a base to write your own components or to try out things. 

### Installation
Install the required dependencies through npm
```none
$ npm install
```

### Usage
Start the application manually 
```none
$ ./demo [--brokerHost 127.0.0.1] [--brokerPort 1883]
```

### Systemd
If your system supports it, you can start the application as a daemon from systemd by using the provided template.
```none
$ sudo ln -s $HOMA_BASEDIR/misc/systemd/homa@.service /etc/systemd/system/multi-user.target.wants/homa@demo.service
$ sudo systemctl --system daemon-reload
$ sudo systemctl start homa@demo.service
```

Logs are then availble in the systemd journal 
```
$ sudo journalctl -u homa@demo.service -n 100 -f
```