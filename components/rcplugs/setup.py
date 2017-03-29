#!/usr/bin/env python2
# -*- coding: utf-8
# $Id$
# Setup rcPlugs is a MQTT RC-Switch bridge used by HomA framework.
# Creates the following retained topics:
# /sys/<systemId>/<systemCode>-<unitCode>, payload: <type>
# /devices/<systemId>/meta/room, payload: room name
# /devices/<systemId>/meta/name, payload: device name
# /devices/<systemId>/controls/<systemCode>-<unitCode>/meta/type, payload: switch
# /devices/<systemId>/controls/<systemCode>-<unitCode>/meta/name, payload: control name
# /devices/<systemId>/controls/<systemCode>-<unitCode>, payload: 0 (off state)
# Holger Mueller
# 2017/03/09 initial revision

import paho.mqtt.client as mqtt
import mqtt_config		# defines host, port, user, pwd, ca_certs

# config here ...
debug = False
systemId = "123456-rcplugs"
device = "rcPlugs"
room = "Home"
# config plugs here
# topic is build from <systemCode>-<unitCode>
mqtt_arr = [
	{'topic': '11111-10000', 'type': 'typeA', 'name': 'rcPlug A'},
	{'topic': '11111-01000', 'type': 'typeA', 'name': 'rcPlug B'},
	{'topic': '11111-00100', 'type': 'typeA', 'name': 'rcPlug C'}]


def get_topic(t1 = None, t2 = None, t3 = None):
	"Create topic string."
	topic = "/devices/"+ systemId
	if t1:
		topic += "/"+ t1
	if t2:
		topic += "/"+ t2
	if t3:
		topic += "/"+ t3
	#if debug: print("get_topic(): '"+ topic+ "'")
	return topic

def homa_init(mqttc):
	"Publish HomA setup messages to MQTT broker."
	print("Publishing HomA setup data (systemId %s) ..." % systemId)
	# set room name
	mqttc.publish(get_topic("meta/room"), room, retain=True)
	# set device name
	mqttc.publish(get_topic("meta/name"), device, retain=True)
	# setup controls
	order = 0
	for mqtt_dict in mqtt_arr:
		order += 1
		mqttc.publish("/sys/%s/%s" % (systemId, mqtt_dict['topic']), "typeA", retain=True)
		mqttc.publish(get_topic("controls", mqtt_dict['topic'], "meta/type"), "switch", retain=True)
		mqttc.publish(get_topic("controls", mqtt_dict['topic'], "meta/order"), order, retain=True)
		mqttc.publish(get_topic("controls", mqtt_dict['topic'], "meta/name"), mqtt_dict['name'], retain=True)
		mqttc.publish(get_topic("controls", mqtt_dict['topic']), 0, retain=True) # default off
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