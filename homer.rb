#!/usr/bin/env ruby
require 'thread'
require 'singleton'
require 'json'
require 'serialport'
require 'mqtt'
require 'eventmachine'
require 'yaml'
require 'open-uri'
require 'oauth2'


# Proxies
require_relative 'helper/SerialProxy.rb'
require_relative 'helper/MqttProxy.rb'
require_relative 'helper/CalendarProxy.rb'

# Devices
require_relative 'devices/GenericDevice.rb'
require_relative 'devices/Ambilight.rb'
require_relative 'devices/Ab440RemotePowerplug.rb'

# Load Config
CONFIG = YAML.load_file("config/config.yml") unless defined? CONFIG


# Initialize proxies classes
$serialProxy = SerialProxy.new()
$mqttProxy = MqttProxy.new()
$calendarProxy = CalendarProxy.new()

# Initialize devices
Ab440RemotePowerplug.new("1");
Ab440RemotePowerplug.new("2");
Ambilight.new("3");

EventMachine.run {
	$calendarProxy.pollPeriodically()
	Signal.trap("INT") {  EventMachine.stop }
}