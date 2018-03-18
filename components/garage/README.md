# HomA - Garage
This component is an ESP8266 project of a garage unit (e.g. to check garage door status, switch the pump of the well, and so on). 

### Basic Requirements
* ESP8266 board, e.g. ESP12E (ebay)
* ...

### Installation
* Build a board based on the [schematics](schematics/garage-esp12-v001.pdf) given
* Install the cross compiler [esp-open-sdk](https://github.com/pfalcon/esp-open-sdk)
* Install the [ESP8266 SDK](https://github.com/espressif/ESP8266_NONOS_SDK)
* Modify ```Makefile```, ```include/user_config.h``` and ```include/mqtt_config.h``` to your needs.
* Create a ```include/user_secret.h``` file with this content (modify as needed):
```none
#ifndef __USER_SECRET_H__
#define __USER_SECRET_H__

// Secret definitions, which shall not go to version control!

// MQTT username and password
#define MQTT_USER	"xxx"
#define MQTT_PASS	"xxx"

// WiFi ssid and password, if WPS is not used.
#define STA_SSID	""
#define STA_PASS	""

#endif // __USER_SECRET_H__

```
* Compile the project
```none
$ cd $HOMA_BASEDIR/components/garage
$ make
```
* Start the ESP8266 board in boot mode and flash the firmware
```none
$ make flash
```
* Once it is running it can be updated by FOTA. Increase the ```APP_VERSION``` in ```include/user_config.h``` and trigger the update by
```none
$ make publish
```

