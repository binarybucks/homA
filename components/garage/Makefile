# Makefile for ESP8266 projects
#
# Thanks to:
# - zarya
# - Jeroen Domburg (Sprite_tm)
# - Christian Klippel (mamalala)
# - Tommie Gannert (tommie)
#
# Changelog:
# - 2014-10-06: Changed the variables to include the header file directory
# - 2014-10-06: Added global var for the Xtensa tool root
# - 2014-11-23: Updated for SDK 0.9.3
# - 2014-12-25: Replaced esptool by esptool.py
# - 2015-09-17: adopted to RHTS project (Holger)
# - 2017-05-27: adopted to Windsensor project (Holger)
# - 2017-06-07: rewritten to support OTA and various flash sizes
# - 2018-03-14: adopted to Garage project (Holger)

VERBOSE		= 0

# name for the target project
TARGET		= Garage

# Output directors to store intermediate compiled files
# relative to the project directory
BUILD_BASE	= build
FW_BASE		= firmware

# base directory for the compiler
COMPILE		= gcc
XTENSA_TOOLS_ROOT ?= /opt/Espressif/xtensa-lx106-elf/bin

# base directory of the ESP8266 SDK package, absolute
#SDK_BASE	?= /opt/Espressif/ESP8266_SDK
SDK_BASE	?= /opt/Espressif/ESP8266_NONOS_SDK

# esptool.py path and port
ESPTOOL		?= $(XTENSA_TOOLS_ROOT)/esptool.py
ESPPORT		?= /dev/tty.SLAB_USBtoUART

# gen_appbin.py Espressif's generate bin script
GEN_APPBIN	?= python $(SDK_BASE)/tools/gen_appbin.py

# Server and location on update server for new firmware
UPDATE_SERVER	:= $(shell grep "^\#define OTA_HOST" include/user_config.h | sed 's/.*"\(.*\)".*/\1/')
UPDATE_PATH		:= /var/www/html/esp8266fw/garage

# MQTT settings to publish new firmware to target
MQTT_PUB		:= mosquitto_pub
MQTT_HOST		:= $(shell grep '^\#define MQTT_HOST' include/mqtt_config.h | sed 's/.*"\(.*\)".*/\1/')
MQTT_USER		:= $(shell grep '^\#define MQTT_USER' include/user_secret.h | sed 's/.*"\(.*\)".*/\1/')
MQTT_PASS		:= $(shell grep '^\#define MQTT_PASS' include/user_secret.h | sed 's/.*"\(.*\)".*/\1/')
 
# app version: 0 = non-OTA, 1 = OTA user1 (auto builds user2), 2 = OTA user2
APP			?= 1
# OTA boot version: old=boot_v1.1 or new=boot_v1.2+
BOOT		?= new
# SPI_MODE: 0=QIO, 1=QOUT, 2=DIO, 3=DOUT
SPI_MODE	?= 2
# SPI_SPEED frequency divider: 0=40MHz, 1=26.7MHz, 2=20MHz, 15=80MHz
SPI_SPEED	?= 0

# flash size and map:
# 0= 512KB( 256KB+ 256KB)
# 2=1024KB( 512KB+ 512KB)
# 3=2048KB( 512KB+ 512KB)
# 4=4096KB( 512KB+ 512KB)
# 5=2048KB(1024KB+1024KB)
# 6=4096KB(1024KB+1024KB)
# 7=4096KB(2048KB+2048KB) not supported, just for compatible with nodeMCU board
# 8=8192KB(1024KB+1024KB)
# 9=16384KB(1024KB+1024KB)
SIZE_MAP	?= 4
FLASH_SIZE	?= 4096

# which modules (subdirectories) of the project to include in compiling
MODULES		= modules mqtt user wiringESP
EXTRA_INCDIR	= include include/modules include/mqtt wiringESP

# libraries used in this project, mainly provided by the SDK
LIBS		= c gcc hal pp phy net80211 lwip wpa wps crypto upgrade main

# compiler flags using during compilation of source files
CFLAGS		= -Os -g -O2 -Wpointer-arith -Wundef -Werror -Wmissing-prototypes -Wl,-EL -fno-inline-functions -nostdlib -mlongcalls -mtext-section-literals  -D__ets__ -DICACHE_FLASH

# linker flags used to generate the main object file
LDFLAGS		= -nostdlib -Wl,--no-check-sections -u call_user_start -Wl,-static

# we create two different files for uploading into the flash
# these are the names and options to generate them
FW_ADDR_1	= 0x00000
FW_ADDR_2	= 0x10000
FW_ADDR_OTA	= 0x01000
FW_ADDR_OTA2	= 0x81000
ifeq ($(APP), 2)
    FW_ADDR_OTA	= $(FW_ADDR_OTA2)
endif
# 512KB Flash
ifeq ($(FLASH_SIZE), 512)
  FW_ADDR_ESP_INIT	= 0x7C000
  FW_ADDR_ESP_BLANK	= 0x7E000
  # blank should be flashed to 0xfe000 also ...
endif
# 4096KB Flash
ifeq ($(FLASH_SIZE), 4096)
  FW_ADDR_ESP_INIT	= 0x3FC000
  FW_ADDR_ESP_BLANK	= 0x3FE000
endif

FW_FILE_1	:= $(addprefix $(FW_BASE)/,$(FW_ADDR_1).bin)
FW_FILE_2	:= $(addprefix $(FW_BASE)/,$(FW_ADDR_2).bin)
FW_FILE_ESP_INIT	:= $(SDK_BASE)/bin/esp_init_data_default.bin
FW_FILE_ESP_BLANK	:= $(SDK_BASE)/bin/blank.bin
FW_FILE_ESP_BOOT	:= $(SDK_BASE)/bin/boot_v1.7.bin
FW_FILE_OTA	:= $(FW_BASE)/user$(APP).$(FLASH_SIZE).$(BOOT).$(SIZE_MAP).bin
FW_FILE_OTA2	:= $(FW_BASE)/user2.$(FLASH_SIZE).$(BOOT).$(SIZE_MAP).bin


####
#### no user configurable options below here
####
app_version	:= $(shell grep "^\#define APP_VERSION" include/user_config.h | sed "s/\#define APP_VERSION "//)
system_id	:= $(shell grep "^\#define HOMA_SYSTEM_ID" include/user_config.h | sed 's/.*"\(.*\)"/\1/')

# set tools to use as compiler, librarian and linker
CC			:= $(XTENSA_TOOLS_ROOT)/xtensa-lx106-elf-gcc
AR			:= $(XTENSA_TOOLS_ROOT)/xtensa-lx106-elf-ar
LD			:= $(XTENSA_TOOLS_ROOT)/xtensa-lx106-elf-gcc
OBJCOPY		:= $(XTENSA_TOOLS_ROOT)/xtensa-lx106-elf-objcopy

# various paths from the SDK used in this project
SDK_LIBDIR	:= lib
SDK_LDDIR	:= ld
SDK_INCDIR	:= include

SRC_DIR		:= $(MODULES)
BUILD_DIR	:= $(addprefix $(BUILD_BASE)/,$(MODULES))

SDK_LIBDIR	:= $(addprefix $(SDK_BASE)/,$(SDK_LIBDIR))
SDK_INCDIR	:= $(addprefix -I$(SDK_BASE)/,$(SDK_INCDIR))

SRC			:= $(foreach sdir,$(SRC_DIR),$(wildcard $(sdir)/*.c))
OBJ			:= $(patsubst %.c,$(BUILD_BASE)/%.o,$(SRC))
LIBS		:= $(addprefix -l,$(LIBS))
APP_AR		:= $(addprefix $(BUILD_BASE)/,$(TARGET)_app.a)
TARGET_OUT	:= $(addprefix $(BUILD_BASE)/,$(TARGET).out)

INCDIR		:= $(addprefix -I,$(SRC_DIR))
EXTRA_INCDIR	:= $(addprefix -I,$(EXTRA_INCDIR))
MODULE_INCDIR	:= $(addsuffix /include,$(INCDIR))

# linker script used by the linker
LD_SCRIPT	= eagle.app.v6.ld
ifneq ($(APP),0)
    ifneq ($(findstring $(SIZE_MAP),  6  8  9),)
        LD_SCRIPT = eagle.app.v6.$(BOOT).2048.ld
        # FW_FILE_ESP_BOOT at least v1.4 and +
    else ifeq ($(SIZE_MAP), 5)
        LD_SCRIPT = eagle.app.v6.$(BOOT).2048.ld
        # FW_FILE_ESP_BOOT at least v1.4 and +
    else ifeq ($(SIZE_MAP), 4)
        LD_SCRIPT = eagle.app.v6.$(BOOT).1024.app$(APP).ld
        # FW_FILE_ESP_BOOT at least v1.2 and +
    else ifeq ($(SIZE_MAP), 3)
        LD_SCRIPT = eagle.app.v6.$(BOOT).1024.app$(APP).ld
    else ifeq ($(SIZE_MAP), 2)
        LD_SCRIPT = eagle.app.v6.$(BOOT).1024.app$(APP).ld
    else ifeq ($(SIZE_MAP), 0)
        LD_SCRIPT = eagle.app.v6.$(BOOT).512.app$(APP).ld
    endif
endif
LD_SCRIPT	:= $(addprefix -T$(SDK_BASE)/$(SDK_LDDIR)/,$(LD_SCRIPT))

V ?= $(VERBOSE)
ifeq ("$(V)","1")
  Q :=
  vecho := @true
else
  Q := @
  vecho := @echo
endif

vpath %.c $(SRC_DIR)

define compile-objects
$1/%.o: %.c
	$(vecho) "CC $$<"
	$(Q) $(CC) $(INCDIR) $(MODULE_INCDIR) $(EXTRA_INCDIR) $(SDK_INCDIR) $(CFLAGS) -c $$< -o $$@
endef

.PHONY: all checkdirs flash clean connect publish

ifeq ($(APP), 0)
all: checkdirs $(TARGET_OUT) $(FW_FILE_1) $(FW_FILE_2)
else
all: checkdirs $(TARGET_OUT) $(FW_FILE_OTA)
endif

$(FW_BASE)/%.bin: $(TARGET_OUT) | $(FW_BASE)
	$(vecho) "FW $(FW_BASE)/"
    ifeq ($(APP), 0)
#	  $(Q) $(ESPTOOL) elf2image -o $(FW_BASE)/ -ff 40m -fs 4m $(TARGET_OUT)
	  $(Q) $(ESPTOOL) elf2image -o $(FW_BASE)/ -ff 40m -fs 4MB $(TARGET_OUT)
    else ifeq ($(APP), 9999)
#	  # does not work, app cores after boot loader
	  $(Q) $(ESPTOOL) elf2image -o $(FW_FILE_OTA) -ff 40m -fs 4MB --version=2 $(TARGET_OUT)
    else
	  $(Q) $(OBJCOPY) --only-section .text -O binary $< eagle.app.v6.text.bin
	  $(Q) $(OBJCOPY) --only-section .data -O binary $< eagle.app.v6.data.bin
	  $(Q) $(OBJCOPY) --only-section .rodata -O binary $< eagle.app.v6.rodata.bin
	  $(Q) $(OBJCOPY) --only-section .irom0.text -O binary $< eagle.app.v6.irom0text.bin
      ifeq ($(BOOT), old)
	    $(Q) env COMPILE=$(COMPILE) $(GEN_APPBIN) $< 1 $(SPI_MODE) $(SPI_SPEED) $(SIZE_MAP) $(APP)
      else
	    $(Q) env COMPILE=$(COMPILE) $(GEN_APPBIN) $< 2 $(SPI_MODE) $(SPI_SPEED) $(SIZE_MAP) $(APP)
      endif
	  $(Q) mv eagle.app.flash.bin $(FW_FILE_OTA)
	  $(Q) rm eagle.app.v6.*
	  $(vecho) "FW $(FW_FILE_OTA) generated. Flash size $(FLASH_SIZE)KB."
	  $(vecho) "$(notdir $(FW_FILE_ESP_BOOT))---------->0x00000"
	  $(vecho) "$(notdir $(FW_FILE_OTA))--->$(FW_ADDR_OTA)"
	  scp $(FW_FILE_OTA) $(UPDATE_SERVER):$(UPDATE_PATH)
      ifeq ($(APP), 1)
	    @rm $(TARGET_OUT)
	    $(MAKE) APP=2
      else ifeq ($(APP), 2)
#	    # user1...bin is now older than $(TARGET_OUT), fix that otherwise make will always redo this rule
	    $(Q) touch $(FW_BASE)/*.bin
      endif
    endif

$(TARGET_OUT): $(APP_AR)
	$(vecho) "LD $@"
	$(Q) $(LD) -L$(SDK_LIBDIR) $(LD_SCRIPT) $(LDFLAGS) -Wl,--start-group $(LIBS) $(APP_AR) -Wl,--end-group -o $@

$(APP_AR): $(OBJ)
	$(vecho) "AR $@"
	$(Q) $(AR) cru $@ $^

checkdirs: $(BUILD_DIR) $(FW_BASE)

$(BUILD_DIR):
	$(Q) mkdir -p $@

$(FW_BASE):
	$(Q) mkdir -p $@


ifeq ($(APP), 0)
flash: $(FW_FILE_1) $(FW_FILE_2)
	# write esp_init_data_default and clear default system parameter values
	$(ESPTOOL) --port $(ESPPORT) write_flash -fm dio $(FW_ADDR_1) $(FW_FILE_1) $(FW_ADDR_2) $(FW_FILE_2) $(FW_ADDR_ESP_INIT) $(FW_FILE_ESP_INIT) $(FW_ADDR_ESP_BLANK) $(FW_FILE_ESP_BLANK)
	# clear user parameter also
	#$(ESPTOOL) --port $(ESPPORT) write_flash -fm dio $(FW_ADDR_1) $(FW_FILE_1) $(FW_ADDR_2) $(FW_FILE_2) 0x3D000 $(FW_FILE_ESP_BLANK)
	# only write compiled code
	#$(ESPTOOL) --port $(ESPPORT) write_flash -fm dio $(FW_ADDR_1) $(FW_FILE_1) $(FW_ADDR_2) $(FW_FILE_2)
else ifeq ($(APP), 1)
flash: $(FW_FILE_OTA)
	$(ESPTOOL) --port $(ESPPORT) write_flash -fm dio 0x00000 $(FW_FILE_ESP_BOOT) $(FW_ADDR_OTA) $(FW_FILE_OTA) $(FW_ADDR_OTA2) $(FW_FILE_OTA2) $(FW_ADDR_ESP_INIT) $(FW_FILE_ESP_INIT)
endif

connect:
	picocom -b 74880 --omap crcrlf $(ESPPORT)

publish:
	$(MQTT_PUB) -h $(MQTT_HOST) -u "$(MQTT_USER)" -P "$(MQTT_PASS)" -q 2 -t "/sys/$(system_id)/server_version" -m "$(app_version)"

clean:
	$(Q) rm -rf $(FW_BASE) $(BUILD_BASE)

$(foreach bdir,$(BUILD_DIR),$(eval $(call compile-objects,$(bdir))))
