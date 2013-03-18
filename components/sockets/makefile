.PHONY: all build clean upload size upload_using_programmer burn_bootloader

all: build

build: clean build_msg ./build/sockets.hex size

build_msg:
	@echo "Building..."

size: ./build/sockets.hex
	"avr-size" -A "./build/sockets.hex"
	"avr-size"  "./build/sockets.elf"

clean:
	@echo "Cleaning..."
	"rm" -rf "./build/sockets.hex" "./build/sockets.elf" "./build/sockets.eep" "./build/Dhcp.cpp.o" "./build/Dns.cpp.o" "./build/Etherlight.cpp.o" "./build/EthernetClient.cpp.o" "./build/EthernetUdp.cpp.o" "./build/socket.cpp.o" "./build/w5100.cpp.o" "./build/sockets.ino.cpp.o"

./build/sockets.hex: ./build/sockets.elf ./build/sockets.eep
	@echo "Creating ./build/sockets.hex..."
	"avr-objcopy" -O ihex -R .eeprom "./build/sockets.elf" "./build/sockets.hex"

./build/sockets.eep: ./build/sockets.elf
	@echo "Creating ./build/sockets.eep..."
	"avr-objcopy" -O ihex -j .eeprom --set-section-flags=.eeprom=alloc,load --no-change-warnings --change-section-lma .eeprom=0 "./build/sockets.elf" "./build/sockets.eep"

./build/sockets.elf: ./build/core.a ./build/Dhcp.cpp.o ./build/Dns.cpp.o ./build/Etherlight.cpp.o ./build/EthernetClient.cpp.o ./build/EthernetUdp.cpp.o ./build/socket.cpp.o ./build/w5100.cpp.o ./build/sockets.ino.cpp.o
	@echo "Creating ./build/sockets.elf..."
	"avr-gcc" -Os -Wl,--gc-sections -mmcu=atmega328p -o "./build/sockets.elf"  "./build/Dhcp.cpp.o" "./build/Dns.cpp.o" "./build/Etherlight.cpp.o" "./build/EthernetClient.cpp.o" "./build/EthernetUdp.cpp.o" "./build/socket.cpp.o" "./build/w5100.cpp.o" "./build/sockets.ino.cpp.o" "./build/core.a" "-L./build" -lm

./build/core.a: ./build/CDC.cpp.o ./build/HardwareSerial.cpp.o ./build/HID.cpp.o ./build/IPAddress.cpp.o ./build/main.cpp.o ./build/new.cpp.o ./build/Print.cpp.o ./build/Stream.cpp.o ./build/Tone.cpp.o ./build/USBCore.cpp.o ./build/WInterrupts.c.o ./build/wiring.c.o ./build/wiring_analog.c.o ./build/wiring_digital.c.o ./build/wiring_pulse.c.o ./build/wiring_shift.c.o ./build/WMath.cpp.o ./build/WString.cpp.o ./build/SPI.cpp.o ./build/RCSwitch.cpp.o ./build/Dhcp.cpp.o ./build/Dns.cpp.o ./build/Ethernet.cpp.o ./build/EthernetClient.cpp.o ./build/EthernetServer.cpp.o ./build/EthernetUdp.cpp.o ./build/socket.cpp.o ./build/w5100.cpp.o ./build/PubSubClient.cpp.o
	@echo "Creating ./build/core.a..."
	"avr-ar" rcs "./build/core.a"  "./build/CDC.cpp.o" "./build/HardwareSerial.cpp.o" "./build/HID.cpp.o" "./build/IPAddress.cpp.o" "./build/main.cpp.o" "./build/new.cpp.o" "./build/Print.cpp.o" "./build/Stream.cpp.o" "./build/Tone.cpp.o" "./build/USBCore.cpp.o" "./build/WInterrupts.c.o" "./build/wiring.c.o" "./build/wiring_analog.c.o" "./build/wiring_digital.c.o" "./build/wiring_pulse.c.o" "./build/wiring_shift.c.o" "./build/WMath.cpp.o" "./build/WString.cpp.o" "./build/SPI.cpp.o" "./build/RCSwitch.cpp.o" "./build/Dhcp.cpp.o" "./build/Dns.cpp.o" "./build/Ethernet.cpp.o" "./build/EthernetClient.cpp.o" "./build/EthernetServer.cpp.o" "./build/EthernetUdp.cpp.o" "./build/socket.cpp.o" "./build/w5100.cpp.o" "./build/PubSubClient.cpp.o"

./build/Dhcp.cpp.o: ./libraries/etherlight/Dhcp.cpp
	@echo "Creating ./build/Dhcp.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/Dns.cpp.o: ./libraries/etherlight/Dns.cpp
	@echo "Creating ./build/Dns.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/Etherlight.cpp.o: ./libraries/etherlight/Etherlight.cpp
	@echo "Creating ./build/Etherlight.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/EthernetClient.cpp.o: ./libraries/etherlight/EthernetClient.cpp
	@echo "Creating ./build/EthernetClient.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/EthernetUdp.cpp.o: ./libraries/etherlight/EthernetUdp.cpp
	@echo "Creating ./build/EthernetUdp.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/socket.cpp.o: ./libraries/etherlight/utility/socket.cpp
	@echo "Creating ./build/socket.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/w5100.cpp.o: ./libraries/etherlight/utility/w5100.cpp
	@echo "Creating ./build/w5100.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/sockets.ino.cpp.o: ./build/sockets.ino.cpp
	@echo "Creating ./build/sockets.ino.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/CDC.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/CDC.cpp
	@echo "Creating ./build/CDC.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/HardwareSerial.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/HardwareSerial.cpp
	@echo "Creating ./build/HardwareSerial.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/HID.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/HID.cpp
	@echo "Creating ./build/HID.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/IPAddress.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/IPAddress.cpp
	@echo "Creating ./build/IPAddress.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/main.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/main.cpp
	@echo "Creating ./build/main.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/new.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/new.cpp
	@echo "Creating ./build/new.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/Print.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/Print.cpp
	@echo "Creating ./build/Print.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/Stream.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/Stream.cpp
	@echo "Creating ./build/Stream.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/Tone.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/Tone.cpp
	@echo "Creating ./build/Tone.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/USBCore.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/USBCore.cpp
	@echo "Creating ./build/USBCore.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/WInterrupts.c.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/WInterrupts.c
	@echo "Creating ./build/WInterrupts.c.o..."
	"avr-gcc" -c -g -Os -w -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/wiring.c.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/wiring.c
	@echo "Creating ./build/wiring.c.o..."
	"avr-gcc" -c -g -Os -w -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/wiring_analog.c.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/wiring_analog.c
	@echo "Creating ./build/wiring_analog.c.o..."
	"avr-gcc" -c -g -Os -w -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/wiring_digital.c.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/wiring_digital.c
	@echo "Creating ./build/wiring_digital.c.o..."
	"avr-gcc" -c -g -Os -w -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/wiring_pulse.c.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/wiring_pulse.c
	@echo "Creating ./build/wiring_pulse.c.o..."
	"avr-gcc" -c -g -Os -w -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/wiring_shift.c.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/wiring_shift.c
	@echo "Creating ./build/wiring_shift.c.o..."
	"avr-gcc" -c -g -Os -w -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/WMath.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/WMath.cpp
	@echo "Creating ./build/WMath.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/WString.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino/WString.cpp
	@echo "Creating ./build/WString.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/SPI.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI/SPI.cpp
	@echo "Creating ./build/SPI.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/RCSwitch.cpp.o: /Users/alexander/Documents/Arduino/libraries/RCSwitch/RCSwitch.cpp
	@echo "Creating ./build/RCSwitch.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/Dhcp.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/Dhcp.cpp
	@echo "Creating ./build/Dhcp.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/Dns.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/Dns.cpp
	@echo "Creating ./build/Dns.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/Ethernet.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/Ethernet.cpp
	@echo "Creating ./build/Ethernet.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/EthernetClient.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/EthernetClient.cpp
	@echo "Creating ./build/EthernetClient.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/EthernetServer.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/EthernetServer.cpp
	@echo "Creating ./build/EthernetServer.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/EthernetUdp.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/EthernetUdp.cpp
	@echo "Creating ./build/EthernetUdp.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/socket.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility/socket.cpp
	@echo "Creating ./build/socket.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/w5100.cpp.o: /Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility/w5100.cpp
	@echo "Creating ./build/w5100.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

./build/PubSubClient.cpp.o: /Users/alexander/Documents/Arduino/libraries/PubSubClient/PubSubClient.cpp
	@echo "Creating ./build/PubSubClient.cpp.o..."
	"avr-g++" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -MMD -mmcu=atmega328p -DF_CPU=16000000L -DARDUINO=102  "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/cores/arduino" "-I/Applications/Arduino.app/Contents/Resources/JAVA/hardware/arduino/variants/standard" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/SPI" "-I/Users/alexander/Documents/Arduino/libraries/RCSwitch" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet" "-I/Users/alexander/Documents/Arduino/libraries/PubSubClient" "-I./libraries/etherlight" "-I./libraries/etherlight/utility" "-I/Applications/Arduino.app/Contents/Resources/JAVA/libraries/Ethernet/utility" "-I."  "$<" -o "$@"

