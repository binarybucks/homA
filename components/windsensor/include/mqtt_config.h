#ifndef __MQTT_CONFIG_H__
#define __MQTT_CONFIG_H__

#include "user_secret.h"

#define MQTT_DEBUG_ON			// define if you want debug info messages

#define CFG_HOLDER	0x00FF55A1	// Change this value to load default configurations
#define CFG_LOCATION	0x79	// Please don't change or if you know what you doing

#define MQTT_SSL_ENABLE

// MQTT boker host settings
#define MQTT_SECURITY		0			// 0:non-SSL, 1:SSL
#define MQTT_HOST			"mqtt.euhm"	// IP or domain name
#if MQTT_SECURITY == 0
#define MQTT_PORT			1883
#else
#define MQTT_PORT			8883
#endif
#define MQTT_BUF_SIZE		1024
#define MQTT_KEEPALIVE		120			// seconds

#define MQTT_CLIENT_ID		"ESP-%08X"

// Secret definitions, which shall not go to version control!
// see user_secret.h
//#define MQTT_USER			""
//#define MQTT_PASS			""

// Station settings, if WPS is not used.
// Secret definitions, which shall not go to version control!
// see user_secret.h
//#define STA_SSID			""
//#define STA_PASS			""

#define MQTT_RECONNECT_TIMEOUT	5		// seconds
#define QUEUE_BUFFER_SIZE	2048

//#define PROTOCOL_NAMEv31	// MQTT version 3.1 compatible with Mosquitto v0.15
#define PROTOCOL_NAMEv311	// MQTT version 3.11 compatible with https://eclipse.org/paho/clients/testing/

#endif // __MQTT_CONFIG_H__
