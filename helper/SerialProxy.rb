class SerialProxy
	def initialize(device = "/dev/ttyACM0", baudRate = 9600, dataBits = 8, stopBits = 1, parity = SerialPort::NONE)

		@device = device
		@baudRate = baudRate
		@dataBits = dataBits
		@stopBits = stopBits
		@parity = parity

		connect()
	end 

	def write(s)
		 writeOperation = proc {
			puts "Writing to serialport " + s.to_s
			sleep(3); #Simulate very slow writing
			#@sp.write(string.to_s)
    }
    completionCallback = proc {|result|
    	puts "Done writing to serialport"
    }
    EventMachine.defer(writeOperation, completionCallback)
	end

	private
		def connect()
			puts "Starting serial proxy for device #{@device}"

			begin
				@sp = SerialPort.new(@device, @baudRate, @dataBits, @stopBits, @parity)
			rescue Exception
				#puts "Unable to open serial device " + @device + ". Exiting!"
				#exit
			end
		end


end