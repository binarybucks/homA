class GenericDevice
	def initialize()
	end

	def registerCallbackOnMQTTTopic(callbackIdentifier, topic)
		$mqttProxy.registerCallbackOnMQTTTopic(lambda { |message| method(callbackIdentifier).call(message) }, topic)
	end
end