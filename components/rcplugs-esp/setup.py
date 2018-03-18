#!/usr/bin/env python2
# -*- coding: utf-8
# Setup rcPlugs is a MQTT RC-Switch bridge used by HomA framework.
# Creates the following retained topics:
# /sys/<systemId>/<systemCode>-<unitCode>/<control>, payload: <type>
# /devices/<systemId>-<systemCode>-<unitCode>/meta/room, payload: room name
# /devices/<systemId>-<systemCode>-<unitCode>/meta/name, payload: device name
# /devices/<systemId>-<systemCode>-<unitCode>/controls/<control>/meta/type, payload: switch
# /devices/<systemId>-<systemCode>-<unitCode>/controls/<control>, payload: 0 (off state)

# Holger Mueller
# 2017/03/09 initial revision
# 2017/04/07 modified to be complient with issue #144 and sockets component
# 2017/10/18 made control name (before fix "Power") configurable
# 2017/12/17 taken from rcPlugs and adopted for rcSwitch (ESP8266 bridge)
# 2017/12/24 added command line switch to remove persistant messages

import sys
import getopt
import paho.mqtt.client as mqtt
import mqtt_config		# defines host, port, user, pwd, ca_certs

# config here ...
debug = False
systemId = "123457-rcplugs"
room = "rcPlugs"
# config plugs here
# topic systemId is build from <systemId>-<systemCode>-<unitCode>
mqtt_arr = [
	{'topic': '11111-10000', 'type': 'typeA', 'room': 'Home', 'device': 'Stern', 'control': 'Power A'},
	{'topic': '11111-01000', 'type': 'typeA', 'room': 'Home', 'device': 'Weihnachtsbaum', 'control': 'Power B'},
	{'topic': '11111-00100', 'type': 'typeA', 'room': 'Home', 'device': 'Lichterkette aussen', 'control': 'Power C'},
	{'topic': '11111-00010', 'type': 'typeA', 'room': '', 'device': '11111-00010', 'control': 'Power D'},
	{'topic': '11111-00001', 'type': 'typeA', 'room': '', 'device': '11111-00001', 'control': 'Power E'}]


def get_topic(systemUnitCode, t1 = None, t2 = None, t3 = None):
	"Create topic string."
	topic = "/devices/%s-%s" % (systemId, systemUnitCode)
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
	mqttc.publish("/devices/%s/meta/room" % systemId, room, retain=True)
	mqttc.publish("/devices/%s/meta/name" % systemId, ".Info", retain=True)
	# setup controls
	order = 1
	for mqtt_dict in mqtt_arr:
		mqttc.publish("/sys/%s/%s/%s" % (systemId, mqtt_dict['topic'], mqtt_dict['control']), mqtt_dict['type'], retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "meta/room"), room, retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "meta/name"), mqtt_dict['device'], retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "controls", mqtt_dict['control'], "meta/type"), "switch", retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "controls", mqtt_dict['control'], "meta/order"), order, retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "controls", mqtt_dict['control'], "meta/room"), mqtt_dict['room'], retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "controls", mqtt_dict['control']), 0, retain=True) # default off
		order += 1
	return

def homa_remove(mqttc):
	"Remove HomA rcplugs messages from MQTT broker."
	print("Removing HomA rcplugs data (systemId %s) ..." % systemId)
	mqttc.publish("/devices/%s/meta/room" % systemId, "", retain=True)
	mqttc.publish("/devices/%s/meta/name" % systemId, "", retain=True)
	# setup controls
	for mqtt_dict in mqtt_arr:
		mqttc.publish("/sys/%s/%s/%s" % (systemId, mqtt_dict['topic'], mqtt_dict['control']), "", retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "meta/room"), "", retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "meta/name"), "", retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "controls", mqtt_dict['control'], "meta/type"), "", retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "controls", mqtt_dict['control'], "meta/order"), "", retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "controls", mqtt_dict['control'], "meta/room"), "", retain=True)
		mqttc.publish(get_topic(mqtt_dict['topic'], "controls", mqtt_dict['control']), "", retain=True)
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

def usage():
	print("Setup MQTT RC-Switch bridge used by HomA framework.")
	print("%s [-h] [--help] [-d] [-r]" % sys.argv[0])
	print("-h, --help        Shows this help")
	print("-d                Enable debug output")
	print("-r                Remove HomA rcSwitch messages")
	return

def main():
	global debug
	remove = False

	# parse command line options
	try:
		opts, args = getopt.getopt(sys.argv[1:], "hdr", ["help"])
	except getopt.GetoptError:
		print("Error. Invalid argument.")
		usage()
		sys.exit(2)
	for opt, arg in opts:
		if opt in ("-h", "--help"):
			usage()
			sys.exit()
		elif opt == '-d':
			debug = True
			print("Debug output enabled.")
		elif opt in ("-r"):
			remove = True
			if debug: print("Remove switch found.")

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

	if remove:
		homa_remove(mqttc)      # remove HomA MQTT device and control settings
	else:
		homa_init(mqttc)        # setup HomA MQTT device and control settings

	# wait until all queued topics are published
	mqttc.loop_stop()
	mqttc.disconnect()
	return

if __name__ == "__main__":
	main()
