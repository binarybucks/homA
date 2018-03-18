/**
 * @file
 * @brief Windsensor main application
 *
 * This is a HomA/MQTT Windsensor unit.
 *
 * Params are loaded, WiFi init, MQTT setup and GPIO keys are set.
 * Sensor pulses are counted using an interrupt.
 * Wind speed is calculated and published to broker within a timer.
 *
 * All configuration is done in "user_config.h".
 */
 
/*
Programming Best Practices
http://www.danielcasner.org/guidelines-for-writing-code-for-the-esp8266/
- Application code should have the ICACHE_FLASH_ATTR decorator unless it is executed very often.
- All interrupt handlers must not have the ICACHE_FLASH_ATTR decorator and any code which executes very often should not have the decorator.
*/

#include <ets_sys.h>
#include <osapi.h>
#include <gpio.h>
#include <user_interface.h>
#include <mem.h>
#include <time.h>
#include <sntp.h>

#include "user_config.h"
#include "user_common.h"
#include "user_upgrade.h"
#include "mqtt/mqtt.h"
#include "mqtt/utils.h"
#include "mqtt/debug.h"
#include "config.h"
#include "key.h"
#include "wifi.h"
#include "dst.h"

// global variables
LOCAL MQTT_Client mqttClient;
LOCAL bool mqtt_connected = false;
LOCAL bool send_start_time = false;
LOCAL struct keys_param keys;
LOCAL struct single_key_param *single_key[KEY_NUM];
LOCAL os_timer_t speed_timer;
LOCAL uint16_t server_version;
LOCAL uint16_t speed_count_0; // counts current speed sensor pulses
LOCAL uint16_t speed_count_1; // last count pulses
LOCAL uint16_t speed_count_2; // before last count pulses
LOCAL os_timer_t sntp_timer; // time for NTP service
#define MAIN_TASK_PRIO        USER_TASK_PRIO_0
#define MAIN_TASK_QUEUE_LEN   1
LOCAL os_event_t main_task_queue[MAIN_TASK_QUEUE_LEN];
// user main task signals
enum sig_speed_task {
	SIG_SEND = 0,
	SIG_UPGRADE
};
const char *rst_reason_text[] = {
	"normal startup by power on", // REASON_DEFAULT_RST = 0
	"hardware watch dog reset",   // REASON_WDT_RST = 1
	"exception reset",            // REASON_EXCEPTION_RST = 2
	"software watch dog reset",   // REASON_SOFT_WDT_RST = 3
	"sw restart, system_restart()", // REASON_SOFT_RESTART = 4
	"wake up from deep-sleep",    // REASON_DEEP_SLEEP_AWAKE = 5
	"external system reset"       // REASON_EXT_SYS_RST = 6
};

#ifdef __cplusplus
extern "C" {
#endif
	uint32 user_rf_cal_sector_set(void);
	void user_init(void);
#ifdef __cplusplus
}
#endif

LOCAL void SpeedLoop_Setup(void);


/**
 ******************************************************************
 * @brief  SDK just reversed 4 sectors, used for rf init data and paramters.
 * @author Holger Mueller
 * @date   2017-06-08
 * We add this function to force users to set rf cal sector, since
 * we don't know which sector is free in user's application.
 * Sector map for last several sectors: ABCCC
 * A : rf cal
 * B : rf init data
 * C : sdk parameters
 *
 * @return rf cal sector
 ******************************************************************
 */
uint32 ICACHE_FLASH_ATTR
user_rf_cal_sector_set(void)
{
	enum flash_size_map size_map = system_get_flash_size_map();
	uint32 rf_cal_sec = 0;
	
	INFO(CRLF);
	switch (size_map) {
	case FLASH_SIZE_4M_MAP_256_256:
		INFO("%s: FLASH_SIZE_4M_MAP_256_256" CRLF, __FUNCTION__);
		rf_cal_sec = 128 - 5;
		break;
	case FLASH_SIZE_8M_MAP_512_512:
		INFO("%s: FLASH_SIZE_8M_MAP_512_512" CRLF, __FUNCTION__);
		rf_cal_sec = 256 - 5;
		break;
	case FLASH_SIZE_16M_MAP_512_512:
	case FLASH_SIZE_16M_MAP_1024_1024:
		INFO("%s: FLASH_SIZE_16M_MAP_512_512" CRLF, __FUNCTION__);
		rf_cal_sec = 512 - 5;
		break;
	case FLASH_SIZE_32M_MAP_512_512:
	case FLASH_SIZE_32M_MAP_1024_1024:
		INFO("%s: FLASH_SIZE_32M_MAP_512_512" CRLF, __FUNCTION__);
		rf_cal_sec = 1024 - 5;
		break;
	case FLASH_SIZE_64M_MAP_1024_1024:
		INFO("%s: FLASH_SIZE_64M_MAP_1024_1024" CRLF, __FUNCTION__);
		rf_cal_sec = 2048 - 5;
		break;
	case FLASH_SIZE_128M_MAP_1024_1024:
		INFO("%s: FLASH_SIZE_128M_MAP_1024_1024" CRLF, __FUNCTION__);
		rf_cal_sec = 4096 - 5;
		break;
	default:
		INFO("%s: default 0?!" CRLF, __FUNCTION__);
		rf_cal_sec = 0;
		break;
	}

	return rf_cal_sec;
}

/**
 ******************************************************************
 * @brief  NTP timer callback.
 * @author Holger Mueller
 * @date   2017-06-11, 2017-07-06, 2017-11-02
 *
 * @param  arg - NULL, not used.
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
CheckSntpStamp_Cb(void *arg)
{
	uint32 current_stamp;
	char *time_str;

	current_stamp = applyDST(sntp_get_current_timestamp());
	if (current_stamp != 0) {
		os_timer_disarm(&sntp_timer);
		time_str = sntp_get_real_time(current_stamp);
		if (os_strlen(time_str) > 0)
			time_str[os_strlen(time_str)-1] = 0; // remove tailing \n
		INFO("sntp: %d, %s" CRLF, current_stamp, time_str);
		MQTT_Publish(&mqttClient,
			"/devices/" HOMA_SYSTEM_ID "/controls/Start time",
			time_str, os_strlen(time_str), 1, 1);
		send_start_time = true; // do not resend start time until we reboot
	}
}

/**
 ******************************************************************
 * @brief  MQTT callback broker connected.
 * @author Holger Mueller
 * @date   2017-06-08, 2017-07-06, 2017-07-11, 2017-11-02
 * Subscribes to /sys topics, publishes HomA /devices/ structure.
 *
 * @param  args - MQTT_Client structure pointer.
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
MqttConnected_Cb(uint32_t *args)
{
	char app_version[20];
	char *rst_reason;
	MQTT_Client *client = (MQTT_Client *) args;
	
	INFO("MQTT: Connected" CRLF);
	mqtt_connected = true;
	
	MQTT_Subscribe(client, "/sys/" HOMA_SYSTEM_ID "/#", 2);

	// setup HomA device topics
	//MQTT_Publish(*client, topic, data, data_length, qos, retain)
	MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/meta/room", HOMA_ROOM, os_strlen(HOMA_ROOM), 1, 1);
	MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/meta/name", HOMA_DEVICE, os_strlen(HOMA_DEVICE), 1, 1);
	
	MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/controls/Wind speed/meta/type", "text", 4, 1, 1);
	MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/controls/Wind speed/meta/unit", " km/h", 5, 1, 1);
	
	MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/controls/Wind speed/meta/order", "1", 1, 1, 1);
	MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/controls/Start time/meta/order", "2", 1, 1, 1);
	MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/controls/Reset reason/meta/order", "3", 1, 1, 1);
	MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/controls/Device id/meta/order", "4", 1, 1, 1);
	MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/controls/Version/meta/order", "5", 1, 1, 1);
	
	MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/controls/Device id",
		sysCfg.device_id, os_strlen(sysCfg.device_id), 1, 1);
	itoa(app_version, APP_VERSION);
	MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/controls/Version",
		app_version, os_strlen(app_version), 1, 1);
	rst_reason = (char *) rst_reason_text[system_get_rst_info()->reason];
	MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/controls/Reset reason",
		rst_reason, os_strlen(rst_reason), 1, 1);

	// do only resend start time if we reboot,
	// do not if we got a Wifi reconnect ...
	if (!send_start_time) {
		os_timer_disarm(&sntp_timer);
		os_timer_setfn(&sntp_timer, (os_timer_func_t *)CheckSntpStamp_Cb, NULL);
		os_timer_arm(&sntp_timer, 100, true);
	}

	// start speed timer
	SpeedLoop_Setup();
}

/**
 ******************************************************************
 * @brief  MQTT callback broker disconnected.
 * @author Holger Mueller
 * @date   2017-06-08
 *
 * @param  args - MQTT_Client structure pointer.
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
MqttDisconnected_Cb(uint32_t * args)
{
	MQTT_Client *client = (MQTT_Client *) args;
	INFO("MQTT: Disconnected" CRLF);
	mqtt_connected = false;
}

/**
 ******************************************************************
 * @brief  MQTT callback message/topic published.
 * @author Holger Mueller
 * @date   2017-06-06
 *
 * @param  args - MQTT_Client structure pointer.
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
MqttPublished_Cb(uint32_t * args)
{
	MQTT_Client *client = (MQTT_Client *) args;
	//INFO("MQTT: Published" CRLF);
}

/**
 ******************************************************************
 * @brief  MQTT callback message/topic received.
 * @author Holger Mueller
 * @date   2017-06-08, 2017-11-02
 *
 * @param  args - MQTT_Client structure pointer.
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
MqttData_Cb(uint32_t * args, const char *topic, uint32_t topic_len, const char *data, uint32_t data_len)
{
	char versionBuf[20];
	char *topicBuf = (char *)os_zalloc(topic_len + 1);
	char *dataBuf = (char *)os_zalloc(data_len + 1);

	MQTT_Client *client = (MQTT_Client *) args;

	os_memcpy(topicBuf, topic, topic_len);
	topicBuf[topic_len] = 0;

	os_memcpy(dataBuf, data, data_len);
	dataBuf[data_len] = 0;

	INFO("Receive topic: %s, data: %s" CRLF, topicBuf, dataBuf);

	if (strcmp(topicBuf, "/sys/" HOMA_SYSTEM_ID "/server_version") == 0) {
		server_version = atoi(dataBuf);
		INFO("Received server version %d" CRLF, server_version);
		if (server_version <= APP_VERSION) {
			INFO("%s: No upgrade. Server version=%d, local version=%d" CRLF, 
				__FUNCTION__, server_version, APP_VERSION);
			server_version = 0; // reset server version
		} else {
			// stop speed timer as long as we do the upgrade
			os_timer_disarm(&speed_timer);
			// tell user that upgrade is in progress
			os_sprintf(versionBuf, "upgrading to %d", server_version);
			MQTT_Publish(client, "/devices/" HOMA_SYSTEM_ID "/controls/Version",
				versionBuf, os_strlen(versionBuf), 1, 1);
			// do the main work in a Task, not here in the callback
			system_os_post(MAIN_TASK_PRIO, SIG_UPGRADE, 0);
		}
	}

	os_free(topicBuf);
	os_free(dataBuf);
}

/**
 ******************************************************************
 * @brief  Do all the stuff that need network after we got an ip.
 * @author Holger Mueller
 * @date   2017-06-08, 2017-10-31
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
WifiGotIp(void)
{
	MQTT_Connect(&mqttClient);

	// setup NTP
	sntp_setservername(0, (char*) "de.pool.ntp.org"); // set server 0 by domain name
	sntp_setservername(1, (char*) "europe.pool.ntp.org"); // set server 1 by domain name
	sntp_setservername(2, (char*) "time.nist.gov"); // set server 2 by domain name
	sntp_set_timezone(1); // set Berlin timezone (GMT+1)
	sntp_init();
}

#ifdef WPS
/**
 ******************************************************************
 * @brief  WiFi WPS status callback.
 * @author Holger Mueller
 * @date   2017-06-06
 *
 * @param  status - WPS status. See SDK documentation.
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
UserWpsStatus_Cb(int status)
{
	switch (status) {
	case WPS_CB_ST_SUCCESS:
		INFO("%s: WPS_CB_ST_SUCCESS" CRLF, __FUNCTION__);
		wifi_wps_disable();
		wifi_station_connect();
		break;
	case WPS_CB_ST_FAILED:
	case WPS_CB_ST_TIMEOUT:
		ERROR("%s: WPS_CB_ST_FAILED or WPS_CB_ST_TIMEOUT" CRLF, __FUNCTION__);
		wifi_wps_start();
		break;
	}
}

/**
 ******************************************************************
 * @brief  WiFi event handler if WPS is enabled.
 * @author Holger Mueller
 * @date   2017-06-07
 *
 * @param  *evt - WiFi system event pointer. See SDK documentation.
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
WifiWpsHandleEvent_Cb(System_Event_t *evt_p)
{
	//INFO("%s: %s" CRLF, __FUNCTION__, wifi_event[evt->event]);
	switch (evt_p->event) {
	case EVENT_STAMODE_DISCONNECTED:
		INFO("%s: disconnect from ssid %s, reason %d" CRLF, __FUNCTION__,
				evt_p->event_info.disconnected.ssid,
				evt_p->event_info.disconnected.reason);
		os_timer_disarm(&speed_timer); // disable speed timer, as we can not send data anymore
		MQTT_Disconnect(&mqttClient);
		break;
	case EVENT_STAMODE_GOT_IP:
		INFO("%s: WiFi connected." CRLF, __FUNCTION__);
		/*INFO("ip: " IPSTR ", mask: " IPSTR ", gw: " IPSTR CRLF,
				IP2STR(&evt_p->event_info.got_ip.ip),
				IP2STR(&evt_p->event_info.got_ip.mask),
				IP2STR(&evt_p->event_info.got_ip.gw));*/
		// start tasks that need network
		WifiGotIp();
		break;
	default:
		break;
	}
}

#else // WPS

/**
 ******************************************************************
 * @brief  WiFi event handler if WPS is disabled.
 * @author Holger Mueller
 * @date   2017-06-06
 *
 * @param  status - WiFi status. See wifi.c and 
 *                  wifi_station_get_connect_status()
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
WifiConnect_Cb(uint8_t status)
{
	if (status == STATION_GOT_IP) {
		WifiGotIp();
	} else {
		MQTT_Disconnect(&mqttClient);
	}
}
#endif // WPS

/**
 ******************************************************************
 * @brief  WPS key's short press function, needed to be installed.
 * @author Holger Mueller
 * @date   2017-06-07
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
WpsKeyShortPress_Cb(void)
{
	//INFO("%s" CRLF, __FUNCTION__);
}

/**
 ******************************************************************
 * @brief  WPS key's long press function, needed to be installed.
 *         Starts WPS push button function.
 * @author Holger Mueller
 * @date   2017-06-07
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
WpsKeyLongPress_Cb(void)
{
#ifdef WPS
	INFO("%s: starting WPS push button ..." CRLF, __FUNCTION__);
	wifi_wps_disable();
	wifi_wps_enable(WPS_TYPE_PBC);
	wifi_set_wps_cb(UserWpsStatus_Cb);
	wifi_wps_start();
#endif // WPS
}

/**
 ******************************************************************
 * @brief  Speed key's short press function, needed to be installed.
 *         Counts pulses from rotor sensor.
 *         Do keep this in RAM (no ICACHE_FLASH_ATTR), as it is
 *         called very often.
 * @author Holger Mueller
 * @date   2017-06-07, 2017-11-02
 ******************************************************************
 */
LOCAL void
SpeedKeyShortPress_Cb(void)
{
	// Keep the Interrupt Service Routine (ISR) / callback short.
	// Do not use “serial print” commands in an ISR. These can hang the system.
	//INFO("%s" CRLF, __FUNCTION__);
	speed_count_0++;
}

/**
 ******************************************************************
 * @brief  Main task for publishing data.
 * @author Holger Mueller
 * @date   2017-11-02, 2018-03-14
 *
 * @param  *event_p - message queue pointer set by system_os_post().
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
Main_Task(os_event_t *event_p)
{
	float windspeed;
	char speed_str[20];

	switch (event_p->sig) {
	case SIG_SEND:
		INFO("%s: Got signal 'SIG_SEND'." CRLF, __FUNCTION__);

		// Calculation of wind speed for the used "Schalenanemometer"
		// see https://de.wikipedia.org/wiki/Anemometer
		// and http://www.kleinwindanlagen.de/Forum/cf3/topic.php?t=2779
		// 1 km/h = 1000 m / 3600 s = 1 / 3.6 m/s; 1 m/s = 3.6 km/h
		// Schnelllaufzahl (SLZ) / tip speed ratio (TSR): 0.3 to 0.4
		// Rotations per second = TSR x Wind speed[m/s] / circumference[m]
		// Wind speed[km/h] = circumference[m] x speed_count / TSR / pulses per rotation * 3.6
		windspeed = CIRCUM * (float) speed_count_1 / SPEED_TB / TSR / PPR * 3.6;

		ftoa(speed_str, windspeed);
		INFO("%s: windspeed=%s" CRLF, __FUNCTION__, speed_str);
		if (mqtt_connected) {
			MQTT_Publish(&mqttClient, "/devices/" HOMA_SYSTEM_ID "/controls/Wind speed",
				speed_str, os_strlen(speed_str), 0, 1);
		}
		/*
		// uncomment this, if you want to debug the speed counter
		itoa(speed_str, speed_count_1);
		INFO("%s: speed_count=%s" CRLF, __FUNCTION__, speed_str);
		if (mqtt_connected) {
			MQTT_Publish(&mqttClient, "/devices/" HOMA_SYSTEM_ID "/controls/Wind count",
				speed_str, os_strlen(speed_str), 0, 1);
		}
		*/
		break;
	case SIG_UPGRADE:
		INFO("%s: Got signal 'SIG_UPGRADE'." CRLF, __FUNCTION__);
		if (!OtaUpgrade(server_version)) {
			// No upgrade will be done, restart speed timer
			SpeedLoop_Setup();
		}
		server_version = 0; // reset server version
		break;
	default:
		ERROR("%s: Unknown signal %d." CRLF, __FUNCTION__, event_p->sig);
		break;
	}
} // Main_Task

/**
 ******************************************************************
 * @brief  Timer callback for speed sensor timebase.
 *         Do keep this in RAM (no ICACHE_FLASH_ATTR), as it is
 *         called very often.
 * @author Holger Mueller
 * @date   2017-06-08, 2017-11-03
 *
 * @param  *arg - callback function parameter set by os_timer_setfn(),
 *                not used.
 ******************************************************************
 */
LOCAL void
SpeedLoop_Cb(void *arg)
{
	// first thing to do: save and reset speed_counter
	ETS_GPIO_INTR_DISABLE();	// lock interrupts until we copied the counter
	speed_count_2 = speed_count_1;
	speed_count_1 = speed_count_0;
	speed_count_0 = 0;	// reset global speed counter
	ETS_GPIO_INTR_ENABLE();

	// produce less traffic, send only if wind counter changed
	if (speed_count_1 != speed_count_2) {
		// do the main work in a Task, not here in the callback
		system_os_post(MAIN_TASK_PRIO, SIG_SEND, 0);
	}
}

/**
 ******************************************************************
 * @brief  Setup speed loop timer
 * @author Holger Mueller
 * @date   2017-11-02
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
SpeedLoop_Setup(void)
{
	// setup speed counter and timer, but do not arm timer
	speed_count_0 = 0;
	speed_count_1 = 0xFFFE; // set an impossible value
	os_timer_disarm(&speed_timer);
	os_timer_setfn(&speed_timer, SpeedLoop_Cb, NULL);
	os_timer_arm(&speed_timer, SPEED_TB * 1000, true);	// trigger timer every SPEED_TB seconds
}

/**
 ******************************************************************
 * @brief  Main user init function.
 * @author Holger Mueller
 * @date   2017-06-08, 2017-07-05, 2017-11-02
 ******************************************************************
 */
void ICACHE_FLASH_ATTR
user_init(void)
{
	// if you do not set the uart, ESP8266 will start with 74880 baud :-(
	//uart_div_modify(0, UART_CLK_FREQ / 115200);
	INFO(CRLF CRLF "SDK version: %s" CRLF, system_get_sdk_version());
	INFO("Windsensor version %d" CRLF, APP_VERSION);
	INFO("Reset reason: %s" CRLF, rst_reason_text[system_get_rst_info()->reason]);

	mqtt_connected = false;
	send_start_time = false;
	CFG_Load();

#ifdef WPS
	INFO("WiFi WPS setup ..." CRLF);
    wifi_set_event_handler_cb(WifiWpsHandleEvent_Cb);
	wifi_set_opmode(STATION_MODE);
	wifi_station_set_hostname(sysCfg.device_id);
#else // WPS
	WIFI_Connect(sysCfg.sta_ssid, sysCfg.sta_pwd, WifiConnect_Cb);
	wifi_station_set_hostname(sysCfg.device_id);
#endif // WPS

	// setup windsensor WPS button pin
	single_key[0] = key_init_single(KEY_0_IO_NUM, KEY_0_IO_MUX,
	  KEY_0_IO_FUNC, WpsKeyLongPress_Cb, WpsKeyShortPress_Cb, FALSE);
	// setup windsensor speed sensor pin
	single_key[1] = key_init_single(KEY_1_IO_NUM, KEY_1_IO_MUX,
	  KEY_1_IO_FUNC, NULL, SpeedKeyShortPress_Cb, TRUE);
	keys.key_num = KEY_NUM;
	keys.single_key = single_key;
	key_init(&keys);
	
	// setup Main_Task (for sending data)
	system_os_task(Main_Task, MAIN_TASK_PRIO, main_task_queue, MAIN_TASK_QUEUE_LEN);

	// setup MQTT
	//MQTT_InitConnection(&mqttClient, sysCfg.mqtt_host, sysCfg.mqtt_port, sysCfg.security);
	MQTT_InitConnection(&mqttClient, MQTT_HOST, MQTT_PORT, MQTT_SECURITY);
	//MQTT_InitClient(&mqttClient, sysCfg.device_id, sysCfg.mqtt_user, sysCfg.mqtt_pass, sysCfg.mqtt_keepalive, 1);
	MQTT_InitClient(&mqttClient, sysCfg.device_id, MQTT_USER, MQTT_PASS, MQTT_KEEPALIVE, 1);
	MQTT_InitLWT(&mqttClient, "/lwt", "offline", 0, 0);
	MQTT_OnConnected(&mqttClient, MqttConnected_Cb);
	MQTT_OnDisconnected(&mqttClient, MqttDisconnected_Cb);
	MQTT_OnPublished(&mqttClient, MqttPublished_Cb);
	MQTT_OnData(&mqttClient, MqttData_Cb);

	INFO("System started." CRLF CRLF);
}
