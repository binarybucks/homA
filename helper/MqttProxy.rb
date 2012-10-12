
class MqttProxy
	def initialize()
		puts "(ThreadID #{Thread.current.object_id}) Initializing MQTT proxy"

		@server = CONFIG['mqtt']['server']
		@port = 1833
		@topicCallbacks = {}
		connect()
	end 

	def registerCallbackOnMQTTTopic(block, topic)
		@topicCallbacks[topic] ||= [] 
		@topicCallbacks[topic].push(block)
		subscribe(topic)
	end

	def publish(topic, payload)
		puts "(ThreadID #{Thread.current.object_id}) MQTT publishing to \"#{topic}\": #{payload} "
		@mqtt.publish(topic, payload, retain=false)
	end

private 
		
	def subscribe(topic)
		puts "(ThreadID #{Thread.current.object_id}) MQTT subscribing to topic: #{topic} "
		@mqtt.subscribe(topic)
	end

	def onMessage(topic, message)
		puts "(ThreadID #{Thread.current.object_id}) MQTT message received on #{topic}: #{message} "
		@topicCallbacks[topic].each { |callback|  callback.call(message) } if (@topicCallbacks.has_key?(topic))
	end

	def connect()
		#puts "(ThreadID #{Thread.current.object_id}) Starting MQTT proxy for server #{@server}:#{@port} "
		@mqtt = MQTT::Client.connect(@server) 
 	
  	Thread.new do 
		  @mqtt.get() { |topic,message| EM.next_tick{onMessage(topic, message)}}
  	end
  end
end