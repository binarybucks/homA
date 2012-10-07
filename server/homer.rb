#!/usr/bin/env ruby
require 'json'
require 'serialport'
require 'faye/websocket'
require 'thread'
require 'singleton'

# Auxilary classes
require_relative 'sender.rb'
require_relative 'receiver.rb'
require_relative 'serialProxy.rb'

# Device classes
require_relative 'devices/GenericDevice.rb'
require_relative 'devices/AB440RemotePowerplug.rb'
require_relative 'devices/Ambilight.rb'

# Initialize auxilary classes
$serialProxy = SerialProxy.instance
$receiver = Receiver.instance
$sender = Sender.instance

# Create devices
$devices = {}
AB440RemotePowerplug.new('switch1', 10101, 2) 
AB440RemotePowerplug.new('switch2', 10101, 2)
Ambilight.new('ambilight', 10101, 2)
					 
# Create WebSocket for interface
App = lambda do |env|
	if Faye::WebSocket.websocket?(env)
		s = Faye::WebSocket.new(env)
		s.onmessage = lambda { |event| $receiver.receive(event.data)}
		s.onopen = lambda {|event| $sender.addSocket(s)}
		s.onclose = lambda { |event| $sender.removeSocket(s);			s = nil}
		s.rack_response # Return async Rack response
	else
		[200, {'Content-Type' => 'text/plain'}, ['This server is currently only serving WebSockets']]
	end
end

