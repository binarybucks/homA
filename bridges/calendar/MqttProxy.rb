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

	end 

	def publish(topic, payload)
		begin
				puts "(ThreadID #{Thread.current.object_id}) MQTT publishing to \"#{topic}\": #{payload} "
				@mqtt.publish(topic, payload, true, 0)
		rescue 
			puts "MQTT publish failed. Trying to reconnect to the broker"
			run()
		end
	end

	def run()
		puts "(ThreadID #{Thread.current.object_id}) Starting MQTT proxy for broker at #{@server}:#{@port} "
		begin
			@mqtt = MQTT::Client.connect(@server) 
  	rescue Exception => e
  		puts "Unable to connect to the broker"
  	end
  end
end
