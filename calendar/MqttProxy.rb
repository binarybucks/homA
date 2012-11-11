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
		puts "(ThreadID #{Thread.current.object_id}) MQTT publishing to \"#{topic}\": #{payload} "
		@mqtt.publish(topic, payload, true, 0)
	end

	def run()
		puts "(ThreadID #{Thread.current.object_id}) Starting MQTT proxy for server #{@server}:#{@port} "
		@mqtt = MQTT::Client.connect(@server) 
  end
end
