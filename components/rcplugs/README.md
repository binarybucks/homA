# HomA - rcPlugs
This component allows the control of generic 433Mhz wireless power outlet sockets. 

### Basic Requirements
* Raspberry Pi
* 433Mhz transmitter that is compatible with the [RC-Switch](https://code.google.com/p/rc-switch/) library, e.g. from ebay.

### Installation
* Connect the transmitter to 5V, Ground and [WiringPi 1](https://pinout.xyz/pinout/wiringpi)
* Install the required dependencies
```none
$ apt-get install wiringpi
$ git clone https://github.com/hmueller01/433Utils
$ make
$ sudo ln -s $HOMA_BASEDIR/components/rcplugs/433Utils/RPi_utils/send /usr/local/bin/rfsend
```
Create a ```mqtt_config.py``` file with this content (modify as needed):
```none
host = "localhost"
port = 1883
user = ""
pwd = ""
ca_certs = ""
```
* Modify ```setup.py``` to your needs.

### Usage
Run the basic setup once
Start the application manually 
```none
$ ./setup.py
```

To add an additional typeA (10 Dip) wireless power socket use
```
$ publish.js [--brokerHost 127.0.0.1] [--brokerPort 1883] --topic "/sys/CLIENTID/11011-01000" --payload "typeA"
```
Where 11011 is the position of the physical socket's group DIP switches, 01000 is the position of the socket's device DIP switches.

Start the application manually 
```none
$ ./rcplugs
```

### Systemd
If your system supports it, you can start the application as a daemon from systemd by using the provided template.
```none
$ sudo ln -s $HOMA_BASEDIR/misc/systemd/homa@.service /etc/systemd/system/multi-user.target.wants/homa@rcplugs.service
$ sudo systemctl --system daemon-reload
$ sudo systemctl start homa@rcplugs.service
```

Logs are then availble in the systemd journal 
```
$ sudo journalctl -u homa@rcplugs.service -n 100 -f
```
