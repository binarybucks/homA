require 'eventmachine'
require 'singleton'
require 'yaml'
require 'json'

require_relative '../helper/MqttProxy.rb'
require_relative '../helper/SerialProxy.rb'

# Load Config
CONFIG = YAML.load_file("config/config.yml") unless defined? CONFIG


class Arduino
	include Singleton 

	def initialize()
		registerCallbackOnMQTTTopic(:remotePowerplug1StatusChanged, "/devices/1/status")
		registerCallbackOnMQTTTopic(:remotePowerplug2StatusChanged, "/devices/2/status")

		registerCallbackOnMQTTTopic(:ambilightStatusChanged, "/devices/3/status")
		registerCallbackOnMQTTTopic(:ambilightFadingChanged, "/devices/3/fading")
		registerCallbackOnMQTTTopic(:ambilightColorChanged, "/devices/3/color")
	end

	def run()
		sensorsPollPeriodically()
	end

private
	def registerCallbackOnMQTTTopic(callbackIdentifier, topic)
		MqttProxy.instance().registerCallbackOnMQTTTopic(lambda { |topic, message| method(callbackIdentifier).call(topic, message) }, topic)
	end


	# AMBILIGHT SPECIFIC CODE
	def ambilightStatusChanged(topic, message)
		remotePowerplugStatusChanged(3, message)
	end

	def ambilightColorChanged(topic, message)
		c = message.match /(..)(..)(..)/  
		SerialProxy.instance().write( "'c':{'r':#{c[1]},'g':#{c[2]},'b':#{c[3]}}" )
	end
	
	def ambilightFadingChanged(topic, message)
		puts "fadingChanged: #{message}"
		@fading = message.to_i
		fade()
	end

	def fade()
    Thread.new do # Spawn background thread for color fading
    	@currentr ||= 0
    	@currentg ||= 0
    	@currentb ||= 255
      while @fading==1 do
        if @currentr == 255 && @currentg == 0  && @currentb < 255
          setColorRgb(@currentr, @currentg, @currentb+1)
        elsif @currentb == 255 && @currentg == 0 && @currentr > 0
          setColorRgb(@currentr-1, @currentg, @currentb)
        elsif @currentr ==0 && @currentb == 255  && @currentg < 255
          setColorRgb(@currentr, @currentg+1, @currentb)
        elsif @currentr == 0 && @currentg == 255 && @currentb > 0
          setColorRgb(@currentr, @currentg, @currentb-1)
        elsif @currentg == 255 && @currentb == 0 && @currentr < 255
          setColorRgb(@currentr+1, @currentg, @currentb)
        elsif @currentr == 255 && @currentb == 0 && @currentg > 0
          setColorRgb(@currentr, @currentg-1, @currentb)
        end
        sleep 1
     	end
   	end
	end

	
	def setColorRgb(r, g, b)
		@currentr = r
		@currentg = g
		@currentb = b
		MqttProxy.instance().publish("/devices/#{@identifier}/color", "%02x" % @currentr + "%02x" % @currentg + "%02x" % @currentb)
	end

	# 433MHZ REMOTE POWERPLUG SPECIFIC CODE
	def remotePowerplug1StatusChanged(topic, message)
			remotePowerplugStatusChanged(1, message)
	end

	def remotePowerplug2StatusChanged(topic, message)
		remotePowerplugStatusChanged(2, message)
	end

	def remotePowerplugStatusChanged(plugIdentifier, message)
		puts "statusChanged: #{message}"
		SerialProxy.instance().write( "{w: {'t': 'dip10','f': '101010','l': '#{plugIdentifier}','s': #{message}  }}")
	end

	# SENSOR SPECIFIC CODE
	def sensorsPollPeriodically(interval = 4)
		@pollIntervall = interval

	 	timer = EventMachine::PeriodicTimer.new(interval) do
			SerialProxy.instance().read() do |result|
				if (result and result.length > 4) then
					begin
						result = JSON.parse(result.gsub("\n",""))
						MqttProxy.instance().publish("/sensors/arduino/temperature", result["s"]["t"])
					rescue Exception => e
						#puts "Received garbage serial output (result), ignoring it"
					end
				end
			end
	 	end
	end

end

# Start the whole thing
EventMachine.run {
	Arduino.instance().run()
	Signal.trap("INT") {  EventMachine.stop }
}
