/**
 * @file
 * @brief User OTA upgrade functions.
 * $Id$
 * Thanks to Martin
 * https://harizanov.com/2015/06/firmware-over-the-air-fota-for-esp8266-soc/
 */
#include "user_config.h"
#include "user_upgrade.h"
#include "osapi.h"
#include "user_interface.h"
#include "mem.h"
#include "espconn.h"
#include "upgrade.h"
#include "mqtt/debug.h"
#include "mqtt/utils.h"

// global variables
LOCAL struct espconn gethostname_conn;

/**
 ******************************************************************
 * @brief  System callback after system_upgrade_start() finished.
 * @author Holger Mueller
 * @date   2017-06-03, 2017-10-30
 *
 * @param  arg - Pointer to upgrade_server_info structure.
 ******************************************************************
 */
static void ICACHE_FLASH_ATTR 
ota_finished_callback(void *arg)
{
	struct upgrade_server_info *update = (struct upgrade_server_info*) arg;
	if (update->upgrade_flag == true) {
		os_printf("[OTA]success; rebooting!" CRLF);
		system_upgrade_reboot();
	} else {
		os_printf("[OTA]failed!" CRLF);
		system_restart();
	}

	os_free(update->pespconn);
	os_free(update->url);
	os_free(update);
}

/**
 ******************************************************************
 * @brief  Handle the OTA upgrade. Server version is checked before,
 *         hostname is resolved.
 * @author Holger Mueller
 * @date   2017-06-07
 *
 * @param  server_ip - IP address of update server.
 * @param  port - HTTP Port of update server.
 * @param  path - Path on update server to upgrade binary files.
 * @return true: upgrade started, false: failed, not startet.
 ******************************************************************
 */
LOCAL bool ICACHE_FLASH_ATTR 
OtaHandleUpgrade(ip_addr_t *server_ip, uint16_t port, const char *path)
{
	const char *file;

	switch (system_upgrade_userbin_check()) {
	case UPGRADE_FW_BIN1:
		file = "user2.4096.new.4.bin";
		break;
	case UPGRADE_FW_BIN2:
		file = "user1.4096.new.4.bin";
		break;
	default:
		os_printf("[OTA]Invalid userbin number! Exit." CRLF);
		return false;
	}

	if (server_ip->addr == 0) {
		os_printf("[OTA]Invalid server ip! Exit." CRLF);
		return false;
	}

	struct upgrade_server_info *update = 
		(struct upgrade_server_info *)os_zalloc(sizeof(struct upgrade_server_info));
	update->pespconn = (struct espconn *)os_zalloc(sizeof(struct espconn));
	os_memcpy(update->ip, server_ip, 4);
	update->port = port;

	os_printf("[OTA]Server URL http://" IPSTR ":%d%s%s" CRLF,
		IP2STR(update->ip), update->port, path, file);

	update->check_cb = ota_finished_callback;
	update->check_times = 10000;
	update->url = (uint8 *) os_zalloc(512);
	os_sprintf((char *)update->url,
		   "GET %s%s HTTP/1.1\r\n"
		   "Host: " IPSTR ":%d\r\n"
		   "Connection: close\r\n"
		   "\r\n",
		   path, file, IP2STR(update->ip), update->port);

	if (system_upgrade_start(update)) {
		os_printf("[OTA]Upgrading..." CRLF);
	} else {
		os_printf("[OTA]Could not start upgrade." CRLF);

		os_free(update->pespconn);
		os_free(update->url);
		os_free(update);
		return false;
	}
	return true;
}

/**
 ******************************************************************
 * @brief  Update server dns found callback. If ok, do the OTA upgrade.
 * @author Holger Mueller
 * @date   2017-06-07
 *
 * @param  name - pointer to the name that was looked up.
 * @param  ipaddr - pointer to an ip_addr_t containing the IP address of
 *                  the hostname, or NULL if the name could not be found
 *                  (or on any other error).
 * @param  arg - a user-specified callback argument passed to
 *               dns_gethostbyname.
 ******************************************************************
 */
LOCAL void ICACHE_FLASH_ATTR
OtaUpgradeDnsFound_Cb(const char *name, ip_addr_t *ipaddr, void *arg)
{
	//struct espconn *pespconn = (struct espconn *)arg;

	if (ipaddr == NULL) {
		INFO("%s: ipaddr not found" CRLF, __FUNCTION__);
		// TODO: init DNS rerequest here!
		/*
        if (++device_recon_count == 5) {
            device_status = DEVICE_CONNECT_SERVER_FAIL;
            user_esp_platform_reset_mode();
        }
		*/
		return;
    }

	INFO("%s: found ip " IPSTR CRLF, __FUNCTION__, IP2STR(ipaddr));
	OtaHandleUpgrade(ipaddr, OTA_PORT, OTA_PATH);
}

/**
 ******************************************************************
 * @brief  Do an OTA upgrade. Check server version, resolve hostname.
 * @author Holger Mueller
 * @date   2017-06-07
 *
 * @param  server_version - OTA version on server.
 * @return true: upgrade started, false: failed, not startet.
 ******************************************************************
 */
bool ICACHE_FLASH_ATTR 
OtaUpgrade(uint16_t server_version)
{
	ip_addr_t update_server_ip;
	bool ret = false;

	if (server_version <= APP_VERSION) {
		INFO("%s: No upgrade. Server version=%d, local version=%d" CRLF, 
			__FUNCTION__, server_version, APP_VERSION);
		return false;
	}
	INFO("%s: Upgrade available version=%d" CRLF, __FUNCTION__, server_version);

	if (UTILS_StrToIP(OTA_HOST, &update_server_ip)) {
		INFO("%s: Update server ip " IPSTR CRLF, __FUNCTION__, IP2STR(&update_server_ip));
		ret = OtaHandleUpgrade(&update_server_ip, OTA_PORT, OTA_PATH);
	} else {
		INFO("%s: Update server hostname %s. Waiting for DNS query ..." CRLF, __FUNCTION__, OTA_HOST);
		update_server_ip.addr = 0;
		// The third parameter can be used to store the IP address which got by DNS, so that if users call espconn_gethostbyname again, it won't run DNS again, but just use the IP address it already got.
		espconn_gethostbyname(&gethostname_conn, OTA_HOST, &update_server_ip, OtaUpgradeDnsFound_Cb);
		ret = true;
	}
	return ret;
}
