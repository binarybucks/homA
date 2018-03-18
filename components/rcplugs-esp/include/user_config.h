#ifndef __USER_CONFIG_H__
#define __USER_CONFIG_H__

#define APP_VERSION 27

#define WPS						// enable WPS

// MQTT settings see mqtt_config.h
#include "mqtt_config.h"

#define OTA_HOST "update.euhm"	// IP or domain name
#define OTA_PORT 80
#define OTA_PATH "/esp8266fw/rcswitch/"

// HomA definitions
#define HOMA_SYSTEM_ID	"123457-rcplugs"
//#define HOMA_DEVICE		"rcplugs"
//#define HOMA_ROOM		"rcPlugs"

// configuration of GPIO pins
#define PIN_433TX		12
#define PIN_WPS			13

#define USE_OPTIMIZE_PRINTF
#ifndef ERROR
#define ERROR(format, ...) os_printf(format, ## __VA_ARGS__)
#endif
#define CRLF "\r\n"

#endif

