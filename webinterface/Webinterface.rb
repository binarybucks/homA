require 'singleton'
require 'em-websocket'
require 'yaml'
require 'thin'
require 'json'

require_relative '../helper/MqttProxy.rb'
CONFIG = YAML.load_file("config/config.yml") unless defined? CONFIG

class Webinterface
	include Singleton

	def initialize ()
		registerCallbackOnMQTTTopic(:broadcastMqttPublishToSockets, "/#")
		@openSockets = []
	end

	def run ()
		EventMachine::WebSocket.start(:host => CONFIG["webinterface"]["host"], :port => CONFIG["webinterface"]["port"]) do |ws|
	  	ws.onopen {onOpen(ws)}
	    ws.onclose {onClose(ws)}
	    ws.onmessage { |msg| onMessage(ws, msg)}
	  end
	end


private 

	def registerCallbackOnMQTTTopic(callbackIdentifier, topic)
		MqttProxy.instance().registerCallbackOnMQTTTopic(lambda { |topic, message| method(callbackIdentifier).call(topic, message) }, topic)
	end

	def onOpen(socket)
		@openSockets.push(socket)
	end

	def onClose(socket)
		@openSockets.each { |s| @openSockets.delete(s) if (s == socket) }
	end

	def onMessage(socket, message)
		json = JSON.parse(message)
		json.each { |key,value| MqttProxy.instance().publish(key, value) }
	end

	def broadcastMqttPublishToSockets(topic, message)
			puts "Webinterace broadcasting #{topic}:#{message} to all connected sockets"
 		 	EM.next_tick { @openSockets.each{|socket| socket.send( {topic => message}.to_json )} }
	end
end




EventMachine.run {
	Webinterface.instance().run()
}