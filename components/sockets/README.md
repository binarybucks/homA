# HomA - Sockets
This component allows the control of generic 433Mhz wireless power sockets.

### Requirements
* Arduino 
* Ethernet Shield (Anything with a W5100 Ethernet Chip)
* 433Mhz transmitter that is compatible with the [RC-Switch](https://code.google.com/p/rc-switch/) library

### Installation
Adapt the Arduino Sketch to your needs and flash it to your Arduino. 
Use a different CLIENT_ID if you are running more than one instance.

### Usage
To add a new wireless power socket
```
$ node publish.js [--brokerHost 127.0.0.1] [--brokerPort 1883] --topic "/sys/158293-433MhzBridge/devices/Switch-X" --payload "A;11011"
```
Where X is is a device unique number, A corresponds to the ID of the power socket and 11011 is the positions of the five group toggles


==> WORKING AREA <==
/sys/158212-Socket/11011-010000:typeA // 10dip switchOn("11011", "01000");
/sys/158212-Socket/4-2:typeB // rotary switchOn(4, 2)
/sys/158212-Socket/11011-3:typeC // intertechno switchOn('a', 1, 2)

=> /devices/158212-Sockets-11011-010000/controls/power/on

