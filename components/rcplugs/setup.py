#!/usr/bin/env python2
# -*- coding: utf-8
# $Id$
# Setup rcPlugs is a MQTT RC-Switch bridge used by HomA framework.
# Creates the following retained topics:
# /sys/<systemId>/<systemCode>-<unitCode>, payload: <type>
# /devices/<systemId>-<systemCode>-<unitCode>/meta/room, payload: room name
# /devices/<systemId>-<systemCode>-<unitCode>/meta/name, payload: device name
# /devices/<systemId>-<systemCode>-<unitCode>/controls/Power/meta/type, payload: switch
# /devices/<systemId>-<systemCode>-<unitCode>/controls/Power, payload: 0 (off state)

# Holger Mueller
# 2017/03/09 initial revision
# 2017/04/06 modified to be complient with issue #144 and sockets component

import sys
import paho.mqtt.client as mqtt
import mqtt_config		# defines host, port, user, pwd, ca_certs

# config here ...
debug = False
systemId = "123456-rcplugs"
#device = "rcPlugs"
#room = "Home"
# config plugs here
# topic is build from <systemCode>-<unitCode>
mqtt_arr = [
	{'topic': '11111-10000', 'type': 'typeA', 'room': 'Home', 'device': 'rcPlug A'},
	{'topic': '11111-01000', 'type': 'typeA', 'room': 'Home', 'device': 'rcPlug B'},
	{'topic': '11111-00100', 'type': 'typeA', 'room': 'Home', 'device': 'rcPlug C'}]


def get_topic(t1 = None, t2 = None, t3 = None):
	"Create topic string."
	if not t1:
		print("ERROR get_topic(): t1 not specified!")
		sys.exit()
	topic = "/devices/%s-%s" % (systemId, t1)
	if t2:
		topic += "/"+ t2
	if t3:
		topic += "/"+ t3
	#if debug: print("get_topic(): '"+ topic+ "'")
	return topic

def homa_init(mqttc):
	"Publish HomA setup messages to MQTT broker."
	print("Publishing HomA setup data (systemId %s) ..." % systemId)
	# setup controls
	order = 1
	for mqtt_dict in mqtt_arr:
		mqttc.publish("/sys/%s/%s" % (systemId, mqtt_dict['topic']), mqtt_dict['type'], retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "meta/room"), mqtt_dict['room'], retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "meta/name"), mqtt_dict['device'], retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "controls/Power/meta/type"), "switch", retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "controls/Power/meta/order"), order, retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "controls/Power"), 0, retain=True) # default off
		order += 1
	return

# The callback for when the client receives a CONNACK response from the broker.
def on_connect(client, userdata, flags, rc):
	if debug: print("on_connect(): Connected with result code "+ str(rc))
	# Subscribing in on_connect() means that if we lose the connection and
	# reconnect then subscriptions will be renewed.
	return

# The callback for when a PUBLISH message is received from the broker.
def on_message(client, userdata, msg):
	if debug: print("on_message(): "+ msg.topic+ ":"+ str(msg.payload))
	return

# The callback for when a message is published to the broker.
def on_publish(client, userdata, mid):
	if debug: print("on_publish(): message send "+ str(mid))
	return

def main():
	# connect to MQTT broker
	mqttc = mqtt.Client()
	mqttc.on_connect = on_connect
	mqttc.on_message = on_message
	mqttc.on_publish = on_publish
	if mqtt_config.ca_certs != "":
		#mqttc.tls_insecure_set(True) # Do not use this "True" in production!
		mqttc.tls_set(mqtt_config.ca_certs, certfile=None, keyfile=None, cert_reqs=ssl.CERT_REQUIRED, tls_version=ssl.PROTOCOL_TLSv1, ciphers=None)
	mqttc.username_pw_set(mqtt_config.user, password=mqtt_config.pwd)
	mqttc.connect(mqtt_config.host, port=mqtt_config.port)
	mqttc.loop_start()

	homa_init(mqttc)        # setup HomA MQTT device and control settings

	# wait until all queued topics are published
	mqttc.loop_stop()
	mqttc.disconnect()
	return

if __name__ == "__main__":
	main()
