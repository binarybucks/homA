# HomA - Energy
This component shows the current power and total energy read by an IR connector (e.g. IR-Kopf by [volkszaehler.org](http://wiki.volkszaehler.org/)) in the framework's interfaces.


### Installation
Install the required dependencies
```none
$ git clone https://github.com/hmueller01/libsml.git
$ make
$ sudo make install
```
Create a ```mqtt_config.py``` file with this content:
```none
host = "localhost"
port = 1883
user = ""
pwd = ""
```
Modify ```energy``` sml_server device ```/dev/vzir0``` to your needs.

### Usage
Start the application manually 
```none
$ ./energy
```

### Systemd
If your system supports it, you can start the application as a daemon from systemd by using the provided template.
```none
$ sudo ln -s $HOMA_BASEDIR/misc/systemd/homa@.service /etc/systemd/system/multi-user.target.wants/homa@energy.service
$ sudo systemctl --system daemon-reload
$ sudo systemctl start homa@energy.service
```

Logs are then availble in the systemd journal 
```
$ sudo journalctl -u homa@energy.service -n 100 -f
```
