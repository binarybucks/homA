# HomA - Windsensor
This component is an ESP8266 project to measure the wind speed by pulses from an anemometer. 

### Basic Requirements
* ESP8266 board, e.g. ESP12E (ebay)
* An anemometer which creates switched pulses while rotating

### Installation
* Build a board based on the [schematics](schematics/windsensor-esp12-v001.pdf) given
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
$ cd $HOMA_BASEDIR/components/windsensor
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

