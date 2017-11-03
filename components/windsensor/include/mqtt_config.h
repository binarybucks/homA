#ifndef __MQTT_CONFIG_H__
#define __MQTT_CONFIG_H__

#include "user_secret.h"

#define MQTT_DEBUG_ON			// define if you want debug info messages

#define CFG_HOLDER	0x00FF55A1	// Change this value to load default configurations
#define CFG_LOCATION	0x79	// Please don't change or if you know what you doing

// define the possible MQTT SSL certificate options
#define NO_TLS						0	// 0: disable SSL/TLS, there must be no certificate verify between MQTT server and ESP8266
#define TLS_WITHOUT_AUTHENTICATION	1	// 1: enable SSL/TLS, but there is no a certificate verify
#define ONE_WAY_ANTHENTICATION		2	// 2: enable SSL/TLS, ESP8266 would verify the SSL server certificate at the same time
#define TWO_WAY_ANTHENTICATION		3	// 3: enable SSL/TLS, ESP8266 would verify the SSL server certificate and SSL server would verify ESP8266 certificate

// MQTT boker host settings
#define MQTT_SECURITY		NO_TLS		// security of the connection
#define MQTT_HOST			"mqtt.euhm"	// IP or domain name
#if MQTT_SECURITY == NO_TLS
#define MQTT_PORT			1883
#else
// TODO: not yet tested ...
#define MQTT_PORT			8883
#define MQTT_SSL_ENABLE
#define CA_CERT_FLASH_ADDRESS 0x77		// CA certificate address in flash to read, 0x77 means address 0x77000
#define CLIENT_CERT_FLASH_ADDRESS 0x78	// client certificate and private key address in flash to read, 0x78 means address 0x78000
// also add mbedtls to the Makefile LIBS
#endif
#define MQTT_BUF_SIZE		1024
#define MQTT_KEEPALIVE		120			// seconds
#define MQTT_RECONNECT_TIMEOUT	5		// seconds
#define QUEUE_BUFFER_SIZE	2048

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

//#define PROTOCOL_NAMEv31	// MQTT version 3.1 compatible with Mosquitto v0.15
#define PROTOCOL_NAMEv311	// MQTT version 3.11 compatible with https://eclipse.org/paho/clients/testing/

#endif // __MQTT_CONFIG_H__
