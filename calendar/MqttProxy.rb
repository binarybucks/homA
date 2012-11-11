require 'thread'
require 'mqtt'
require 'singleton'
require 'yaml'
#require 'tree' 

# Load Config
CONFIG = YAML.load_file("config.yml") unless defined? CONFIG

class MqttProxy
	include Singleton 

	def initialize()
		puts "(ThreadID #{Thread.current.object_id}) Initializing MQTT proxy"

		@server = CONFIG['mqtt']['server']
		@port = 1833
		@topicCallbacks = {}

		#@callbackTree = Tree::TreeNode.new("ROOT", "Root Content")

		positiveWildcardMatch?("/#", "/devices/arduino/status")
		positiveWildcardMatch?("/devices/#", "/devices/arduino/1/status")
		positiveWildcardMatch?("/false/arduino/status", "/devices/arduino/status")
		positiveWildcardMatch?("/false/arduino/#", "/devices/arduino/status")
		positiveWildcardMatch?("/devices/arduino/#", "/devices/arduino/status")
		positiveWildcardMatch?("/devices/#/status", "/devices/arduino/status")
		positiveWildcardMatch?("/false/#/status", "/devices/arduino/status")


		run()
	end 

	def registerCallbackOnMQTTTopic(block, topic)
		@topicCallbacks[topic] ||= [] 
		@topicCallbacks[topic].push(block)
		subscribe(topic)
	end

	def publish(topic, payload)
		puts "(ThreadID #{Thread.current.object_id}) MQTT publishing to \"#{topic}\": #{payload} "
		@mqtt.publish(topic, payload, true, 0)
	end

private 
		
	def subscribe(topic)
		puts "(ThreadID #{Thread.current.object_id}) MQTT subscribing to topic: #{topic} "
		@mqtt.subscribe(topic)
	end

	def onMessage(topicOfMessage, message)
		puts "(ThreadID #{Thread.current.object_id}) MQTT message received on #{topicOfMessage}: #{message} "

		@topicCallbacks.keys.each do|key|  
			if (key == topicOfMessage or positiveWildcardMatch?(key, topicOfMessage)) then
				@topicCallbacks[key].each{|callback| callback.call(topicOfMessage, message)}  
			end			
		end
	end

	def positiveWildcardMatch?(key, topicOfMessage)
		#puts "Matching: #{topicOfMessage} against #{key}"

		# Transform key (e.g. /foo/bar/#) to regex form by replacing mqtt wildcard # with regex wildcard *
		# Mqtt single level + wildcard is not yet supported
		searchString = key.gsub('#', '*')
		topicOfMessage.match(searchString) {|matchData| return true}
		return false
	end

	def run()
		puts "(ThreadID #{Thread.current.object_id}) Starting MQTT proxy for server #{@server}:#{@port} "
		@mqtt = MQTT::Client.connect(@server) 
 	
  	Thread.new do 
  		puts "In mqtt thread"
		  @mqtt.get() { |topic,message| EM.next_tick{onMessage(topic, message)}}
  	end
  end
end
