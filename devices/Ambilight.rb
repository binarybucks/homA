require_relative 'GenericDevice.rb'

class Ambilight < GenericDevice
	def initialize (identifier)
		@identifier = identifier
		registerCallbackOnMQTTTopic(:statusChanged, "/devices/#{@identifier}/status")
		registerCallbackOnMQTTTopic(:fadingChanged, "/devices/#{@identifier}/fading")
		registerCallbackOnMQTTTopic(:colorChanged, "/devices/#{@identifier}/color")
	end


	private 
	def colorChanged(message)
		puts "colorChanged: #{message}"
		c = message.match /(..)(..)(..)/  
		$serialProxy.write( "'c':{'r':#{c[1]},'g':#{c[2]},'b':#{c[3]}}" )
	end
	
	def fadingChanged(message)
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

		$mqttProxy.publish("/devices/#{@identifier}/color", "%02x" % @currentr + "%02x" % @currentg + "%02x" % @currentb)
	
	end

	def statusChanged(message)
		puts "statusChanged: #{message}"
		$serialProxy.write( "{w: {'t': 'dip10','f': '101010','l': '1','s': #{message}  }}")
	end



end



	