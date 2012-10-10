require_relative 'GenericDevice.rb'

class Ab440RemotePowerplug < GenericDevice
	def initialize (identifier)
		registerCallbackOnMQTTTopic(:statusChanged, "/devices/#{identifier}/status")
	end

	def statusChanged(message)
		puts "statusChanged: #{message}"
		$serialProxy.write( "{w: {'t': 'dip10','f': '101010','l': '1','s': #{message}  }}")
	end
end