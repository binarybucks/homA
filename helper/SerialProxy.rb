require 'serialport'
require 'singleton'
# Load Config
CONFIG = YAML.load_file("config/config.yml") unless defined? CONFIG

class SerialProxy
	include Singleton
	def initialize()
		puts "(ThreadID #{Thread.current.object_id}) Initializing serial proxy"

		@device = CONFIG['serialport']['device']
		@baudRate = CONFIG['serialport']['baudRate']
		@dataBits = 8
		@stopBits = 1
		@parity = SerialPort::NONE


		run()
	end 

	def write(s)
		 writeOperation = proc {
			puts "Writing to serialport " + s.to_s
			@writeSp.write(s.to_s)
    }
    completionCallback = proc {|result|
    	puts "Done writing to serialport"
    }
    EventMachine.defer(writeOperation, completionCallback)
	end

	def read()

		 readOperation = proc {
		 	r = @readSp.gets()
     }
     completionCallback = proc {|result|
     	#puts "read #{result}"
     	yield result unless result.nil?
     }
     EventMachine.defer(readOperation, completionCallback)

	end

	private
		def run()
			begin
				@readSp = SerialPort.new(@device, @baudRate, @dataBits, @stopBits, @parity)
				@readSp.read_timeout = 4 # A random value seems to be required here to prevent the serialport gem from going haywire 
				@writeSp = SerialPort.new(@device, @baudRate, @dataBits, @stopBits, @parity)

			rescue Exception
				puts "Unable to open serial device " + @device + ". Exiting!"
				exit
			end
		end


end