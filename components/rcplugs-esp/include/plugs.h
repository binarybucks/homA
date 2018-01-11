/*
 * plugs.h
 *
 *  Created on: 16.12.2017
 *      Author: hmueller
 */

#ifndef __PLUGS_H__
#define __PLUGS_H__

typedef struct _plugs {
	char systemCode[5+1];
	char unitCode[5+1];
	char *control;
	int type;
	struct _plugs *next;
} plugs_t;

typedef enum {
    typeA = 1,
    typeB,
    typeUnkown = -1
} plug_type_t;

plugs_t * getPlug(const char *systemCode, const char *unitCode);
void addPlug(MQTT_Client *client, const char *systemCode, const char *unitCode, const char *control, const char *type);
void removePlug(MQTT_Client *client, const char *systemCode, const char *unitCode);
void setPlug(MQTT_Client *client, RCSwitch transmitter, const char *systemCode, const char *unitCode, const char *control, const char *payload);

#endif /* __PLUGS_H__ */
