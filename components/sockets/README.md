# HomA - Sockets
This component allows the control of generic 433Mhz wireless power sockets. 
If you have access to etching equipment, a schematic for a custom Arduino shield is included in the ```pcb``` directory. 


### Basic Requirements
* Arduino Uno or compatible
* Ethernet Shield (Anything with a W5100 Ethernet Chip)
* 433Mhz transmitter that is compatible with the [RC-Switch](https://code.google.com/p/rc-switch/) library. 

### Arduino Shield requirements
* Etching equipment
* 7805 Linear regulator
* SMA Jack
* 100nF Capacitor
* 330nF Capacitor
* Power Jack

### Installation
* Adapt the Arduino Sketch to your needs and flash it to your Arduino. 
* Adapt the MAC adress to something unique in your network
* Use a different CLIENTID if you are running more than one instance.


### Usage
To add a new typeA (10 Dip) wireless power socket
```
$ publish.js [--brokerHost 127.0.0.1] [--brokerPort 1883] --topic "/sys/CLIENTID/11011-01000" --payload "typeA"
```
Where 11011 is the position of the physical socket's group DIP switches, 01000 is the position of the socket's device DIP switches.
