#!/usr/bin/env python2
# -*- coding: utf-8
# Setup min_max_saver. This is a min/max saver used by HomA framework.
# Creates the following retained topics:
# /sys/<systemId>/min/<minSystemId>/<minControlId>, payload: <time>
# /sys/<systemId>/max/<maxSystemId>/<maxControlId>, payload: <time>
# 
# Holger Mueller
# 2017/10/24 initial revision

import sys
import paho.mqtt.client as mqtt
import mqtt_config		# defines host, port, user, pwd, ca_certs

# config here ...
debug = True
systemId = "123456-min-max-saver"
# config min/max saver here
mqtt_arr = [
	{'saver': 'max', 'system': '123456-windsensor', 'control': 'Wind speed', 'time': '24'},
	{'saver': 'min', 'system': '123456-energy', 'control': 'Current Power', 'time': '24'},
	{'saver': 'max', 'system': '123456-energy', 'control': 'Current Power', 'time': '24'}]


def homa_init(mqttc):
	"Publish HomA setup messages for min/max saver."
	print("Publishing HomA setup data (systemId %s) ..." % systemId)
	# setup controls
	for mqtt_dict in mqtt_arr:
		mqttc.publish("/sys/%s/%s/%s/%s" % (systemId, mqtt_dict['saver'], mqtt_dict['system'], mqtt_dict['control']), mqtt_dict['time'], retain=True)
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