class SerialProxy
	include Singleton


	def initialize(device = "/dev/ttyACM0", baudRate = 9600, dataBits = 8, stopBits = 1, parity = SerialPort::NONE)
		@device = device
		@baudRate = baudRate
		@dataBits = dataBits
		@stopBits = stopBits
		@parity = parity

		open()
	end 

	def open
		begin
			@sp = SerialPort.new(@device, @baudRate, @dataBits, @stopBits, @parity)
		rescue Exception
			#puts "Unable to open serial device " + @device + ". Exiting!"
			#exit
		end
	end

	def write(s)
		Mutex.new.synchronize {
			puts "Writing to serialport " + s.to_s
			#@sp.write(string.to_s)
		}
	end
end