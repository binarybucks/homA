class SerialProxy
	def initialize()
		puts "(ThreadID #{Thread.current.object_id}) Initializing serial proxy"

		@device = CONFIG['serialport']['device']
		@baudRate = CONFIG['serialport']['baudRate']
		@dataBits = 8
		@stopBits = 1
		@parity = SerialPort::NONE


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
			begin
				@sp = SerialPort.new(@device, @baudRate, @dataBits, @stopBits, @parity)
			rescue Exception
				#puts "Unable to open serial device " + @device + ". Exiting!"
				#exit
			end
		end


end