#!/usr/bin/env ruby
require 'thread'
require 'singleton'
require 'json'
require 'serialport'
require 'mqtt'
require 'eventmachine'
require 'yaml'

# Proxies
require_relative 'helper/SerialProxy.rb'
require_relative 'helper/MqttProxy.rb'
require_relative 'helper/CalendarProxy.rb'

# Devices
require_relative 'devices/GenericDevice.rb'
require_relative 'devices/Ambilight.rb'
require_relative 'devices/Ab440RemotePowerplug.rb'


# Initialize proxies classes
#$serialProxy = SerialProxy.new()
#$mqttProxy = MqttProxy.new("192.168.8.2")
# Initialize devices
#Ab440RemotePowerplug.new("1");
#Ab440RemotePowerplug.new("2");
#Ambilight.new("3");

EventMachine.run {
	$calendarProxy = CalendarProxy.new("alr.st_t4do0ippogfurs00brmpgfhre0@group.calendar.google.com")

}