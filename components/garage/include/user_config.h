#ifndef __USER_CONFIG_H__
#define __USER_CONFIG_H__

#define APP_VERSION 7

#define WPS						// enable WPS

// MQTT settings see mqtt_config.h
#include "mqtt_config.h"

#define OTA_HOST "update.euhm"	// IP or domain name
#define OTA_PORT 80
#define OTA_PATH "/esp8266fw/garage/"

// HomA definitions
#define HOMA_SYSTEM_ID	"123456-garage"
#define HOMA_DEVICE		"Garage"
#define HOMA_ROOM		"Garage"
#define HOMA_HOME		"Home"

// configuration of interrupt driven GPIO ports (see wiringESP.c)
#define WPS_PIN		14
#define DOOR_PIN		12
#define CISTERN_PIN	13

#define USE_OPTIMIZE_PRINTF
#define ERROR(format, ...) os_printf(format, ## __VA_ARGS__)
#define CRLF "\r\n"

#endif

