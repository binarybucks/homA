/**
 * @file
 * @brief Functions to manage the plugs.
 * @author hmueller01
 * @date 2017-12-16, 2018-01-10
 */

// C++ wrapper
extern "C" {
// put C includes inside here to avoid undefined references by linker.
#include <c_types.h>
#include <osapi.h>
#include <mem.h>
}

#include "user_config.h"
#include "mqtt/mqtt.h"
#include "mqtt/debug.h"
#include "RCSwitch.h"
#include "plugs.h"


// global variables
LOCAL plugs_t *plugs = NULL;


/**
 ******************************************************************
 * @brief  Get the plug data.
 * @author Holger Mueller
 * @date   2017-12-17
 *
 * @param  systemCode - Plug system code, e.g. "11111".
 * @param  unitCode - Plug unit code, e.g. "10000".
 * @return Pointer to plug data (see plugs) or NULL if not found.
 ******************************************************************
 */
plugs_t * ICACHE_FLASH_ATTR
getPlug(const char *systemCode, const char *unitCode)
{
	plugs_t *plug = plugs;

	while (plug != NULL) {
		if (os_strcmp(plug->systemCode, systemCode) == 0 &&
				os_strcmp(plug->unitCode, unitCode) == 0) {
			INFO("%s: %s-%s found." CRLF,
				__FUNCTION__, systemCode, unitCode);
			return plug;
		}
		plug = plug->next;
	}
	INFO("%s: %s-%s NOT found." CRLF,
		__FUNCTION__, systemCode, unitCode);
  return NULL;
}


/**
 ******************************************************************
 * @brief  Add a plug to the plugs data list and subscribe the plug
 *         to the MQTT broker.
 * @author Holger Mueller
 * @date   2017-12-17
 *
 * @param  client - MQTT client.
 * @param  systemCode - Plug system code, e.g. "11111".
 * @param  unitCode - Plug unit code, e.g. "10000".
 * @param  control - Name of the plug control.
 * @param  type - Plug type, e.g. "typeA" or "typeB".
 ******************************************************************
 */
void ICACHE_FLASH_ATTR
addPlug(MQTT_Client *client, const char *systemCode, const char *unitCode, const char *control, const char *type)
{
	plugs_t *plug = getPlug(systemCode, unitCode);

	if (plug == NULL) {
		// create and add a new plug
		plug = (plugs_t *) os_zalloc(sizeof(plugs_t));
		plug->control = (char *) os_malloc(os_strlen(control) + 1);
		plug->next = NULL;

		if (plugs == NULL) {
			plugs = plug;
		} else {
			plugs_t *p = plugs;
			// find last
			while (p->next != NULL) {
				p = p->next;
			}
			p->next = plug;
		}
		INFO("%s: %s-%s %s '%s' added." CRLF,
				__FUNCTION__, systemCode, unitCode, type, control);
	} else {
		// update the plug
		// unsubscribe topic "/devices/<systemId>-<systemCode>-<unitCode>/controls/<control>/on"
		// e.g. "/devices/123456-rcplugs-11011-01000/controls/Power/on"
		int topic_len = os_strlen("/devices/" HOMA_SYSTEM_ID "-11011-01000/controls//on") +
				os_strlen(plug->control);
		char *topic = (char *) os_malloc(topic_len + 1);
		os_sprintf(topic, "/devices/" HOMA_SYSTEM_ID "-%s-%s/controls/%s/on",
				plug->systemCode, plug->unitCode, plug->control);
		MQTT_UnSubscribe(client, topic);
		os_free(topic);

		os_free(plug->control);
		plug->control = (char *) os_malloc(os_strlen(control) + 1);
		INFO("%s: %s-%s %s '%s' updated." CRLF,
				__FUNCTION__, systemCode, unitCode, type, control);
	}
	// actually set the plug data
	os_memcpy(plug->systemCode, systemCode, sizeof(plug->systemCode));
	os_memcpy(plug->unitCode, unitCode, sizeof(plug->unitCode));
	os_strcpy(plug->control, control);
	if (os_strcmp(type, "typeA") == 0) {
		plug->type = typeA;
	} else if (os_strcmp(type, "typeB") == 0) {
		plug->type = typeB;
	} else {
		plug->type = typeUnkown;
	}

	// subscribe topic "/devices/<systemId>-<systemCode>-<unitCode>/controls/<control>/on"
	// e.g. "/devices/123456-rcplugs-11011-01000/controls/Power/on"
	int topic_len = os_strlen("/devices/" HOMA_SYSTEM_ID "-11011-01000/controls//on") +
			os_strlen(control);
	char *topic = (char *) os_malloc(topic_len + 1);
	os_sprintf(topic, "/devices/" HOMA_SYSTEM_ID "-%s-%s/controls/%s/on",
			systemCode, unitCode, control);
	MQTT_Subscribe(client, topic, 2);
	os_free(topic);
}


/**
 ******************************************************************
 * @brief  Remove a plug from the plugs data list and unsubscribe
 *         the plug from the MQTT broker.
 * @author Holger Mueller
 * @date   2017-12-17
 *
 * @param  client - MQTT client.
 * @param  systemCode - Plug system code, e.g. "11111".
 * @param  unitCode - Plug unit code, e.g. "10000".
 ******************************************************************
 */
void ICACHE_FLASH_ATTR
removePlug(MQTT_Client *client, const char *systemCode, const char *unitCode)
{
	plugs_t *plug = getPlug(systemCode, unitCode);

	if (plug != NULL) {
		// unsubscribe topic "/devices/<systemId>-<systemCode>-<unitCode>/controls/<control>/on"
		// e.g. "/devices/123456-rcplugs-11011-01000/controls/Power/on"
		int topic_len = os_strlen("/devices/" HOMA_SYSTEM_ID "-11011-01000/controls//on") +
				os_strlen(plug->control);
		char *topic = (char *) os_malloc(topic_len + 1);
		os_sprintf(topic, "/devices/" HOMA_SYSTEM_ID "-%s-%s/controls/%s/on",
				plug->systemCode, plug->unitCode, plug->control);
		MQTT_UnSubscribe(client, topic);
		os_free(topic);

		// free allocated control buffer
		os_free(plug->control);

		// unchain plug
		if (plug == plugs) {
			// first item must be removed
			plugs = plug->next;
		} else {
			// find plug in chain
			plugs_t *p = plugs;
			while ((p->next != NULL) && (p->next != plug)) {
				p = p->next;
			}
			// remove from chain
			p->next = plug->next;
		}

		// free allocated plug buffer
		os_free(plug);
		INFO("%s: %s-%s removed." CRLF,
				__FUNCTION__, systemCode, unitCode);
	}
}


/**
 ******************************************************************
 * @brief  Set the plug via RCSwitch (433 MHz).
 * @author Holger Mueller
 * @date   2017-12-17
 *
 * @param  client - MQTT client.
 * @param  transmitter - RCSwitch class.
 * @param  systemCode - Plug system code, e.g. "11111".
 * @param  unitCode - Plug unit code, e.g. "10000".
 * @param  control - Name of the plug control.
 * @param  payload - Switch off "0" or on ("1").
 ******************************************************************
 */
void ICACHE_FLASH_ATTR
setPlug(MQTT_Client *client, RCSwitch transmitter, const char *systemCode, const char *unitCode,
		const char *control, const char *payload)
{
	plugs_t *plug = getPlug(systemCode, unitCode);
	bool switchOn = os_strncmp(payload, "0", 1) != 0;
	const char *invPayload = switchOn ? "0" : "1";

	// create publishing topic
	// "/devices/<systemId>-<systemCode>-<unitCode>/controls/<control>"
	// e.g. "/devices/123456-rcplugs-11011-01000/controls/Power"
	int topic_len = os_strlen("/devices/" HOMA_SYSTEM_ID "-11011-01000/controls/") +
			os_strlen(control);
	char *topic = (char *) os_malloc(topic_len + 1);
	os_sprintf(topic, "/devices/" HOMA_SYSTEM_ID "-%s-%s/controls/%s",
			systemCode, unitCode, control);

	if (plug == NULL) {
		// turn switch back, because we hav'nt found a configured plug
		MQTT_Publish(client, topic, invPayload, 1, 1, true);
		os_free(topic);
		ERROR("%s: Error. Plug %s-%s not found." CRLF,
				__FUNCTION__, systemCode, unitCode);
		return;
	}

	switch (plug->type) {
	case typeA:
		if (switchOn) {
			transmitter.switchOn(plug->systemCode, plug->unitCode);
		} else {
			transmitter.switchOff(plug->systemCode, plug->unitCode);
		}
		// send back switch state
		MQTT_Publish(client, topic, payload, 1, 1, true);
		INFO("%s: Switched plug %s-%s typeA: %s." CRLF,
				__FUNCTION__, systemCode, unitCode, switchOn ? "on" : "off");
		break;
	case typeB:
		if (switchOn) {
			transmitter.switchOn(atoi(plug->systemCode), atoi(plug->unitCode));
		} else {
			transmitter.switchOff(atoi(plug->systemCode), atoi(plug->unitCode));
		}
		// send back switch state
		MQTT_Publish(client, topic, payload, 1, 1, true);
		INFO("%s: Switched plug %s-%s typeB: %s." CRLF,
				__FUNCTION__, systemCode, unitCode, switchOn ? "on" : "off");
		break;
	default:
		// turn switch back, because we hav'nt found a supported plug type
		MQTT_Publish(client, topic, invPayload, 1, 1, true);
		ERROR("%s: Error. Unknown type %d." CRLF,
				__FUNCTION__, plug->type);
		break;
	}
	os_free(topic);
}
