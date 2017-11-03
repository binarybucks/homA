#ifndef __USER_CONFIG_H__
#define __USER_CONFIG_H__

#define APP_VERSION 21

#define WPS						// enable WPS

// MQTT settings see mqtt_config.h
#include "mqtt_config.h"

#define OTA_HOST "update.euhm"	// IP or domain name
#define OTA_PORT 80
#define OTA_PATH "/esp8266fw/windsensor/"

// HomA definitions
#define HOMA_SYSTEM_ID	"123456-windsensor"
#define HOMA_DEVICE		"Windsensor"
#define HOMA_ROOM		"Sensors"

// definitions of the wind speed sensor (e.g. "Schalenanemometer")
#define SPEED_TB	1	// time base[s] of measurement (speed_timer)
#define CIRCUM	0.434	// circumference[m] of anemometer
#define TSR		0.4		// Schnelllaufzahl (SLZ) / tip speed ratio (TSR)
#define PPR		2		// pulses per rotation (speed_count per rotation)

// configuration of interrupt driven GPIO ports (see key.c)
#define KEY_NUM			2
// KEY_0 = WPS button
#define KEY_0_IO_MUX	PERIPHS_IO_MUX_MTCK_U
#define KEY_0_IO_NUM	13
#define KEY_0_IO_FUNC	FUNC_GPIO13
// KEY_1 = speed sensor
#define KEY_1_IO_MUX	PERIPHS_IO_MUX_MTDI_U
#define KEY_1_IO_NUM	12
#define KEY_1_IO_FUNC	FUNC_GPIO12
// spare definition for pin 14
//#define KEY_0_IO_MUX	PERIPHS_IO_MUX_MTMS_U
//#define KEY_0_IO_NUM	14
//#define KEY_0_IO_FUNC	FUNC_GPIO14

#define USE_OPTIMIZE_PRINTF
#define ERROR(format, ...) os_printf(format, ## __VA_ARGS__)
#define CRLF "\r\n"

#endif

