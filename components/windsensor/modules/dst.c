/**
 * @file
 * @brief Daylight saving time functions
 * Based on Richard A Burtons timezone.c
 * https://github.com/raburton/esp8266/tree/master/ntp
 * Original Copyright 2015 Richard A Burton
 * richardaburton@gmail.com
 * The MIT License (MIT)
 * See license.txt for license terms.
 * Adapted and modified by Holger Mueller
 */

#include <c_types.h>
#include <time.h>
#include "dst.h"

/**
 ******************************************************************
 * @brief  Check if we are in daylight saving time.
 * @author Richard A Burton, Holger Mueller
 * @date   2015, 2017-07-06
 *
 * @param  timestamp - timestamp to check.
 * @return true if we are in DST, else false.
 ******************************************************************
 */
bool ICACHE_FLASH_ATTR
isDST(time_t timestamp)
{
	struct tm *time;
	bool dst = false;

	if (timestamp == 0)
		// no time set, always false
		return false;

	time = gmtime(&timestamp);

	// work out if dst is active
	// this is for Europe Daylight Saving Time
	// modify according to your local rules
    if (time->tm_mon < 2 || time->tm_mon > 9) {
		// these months are completely out of DST
		dst = false;
	} else if (time->tm_mon > 2 && time->tm_mon < 9) {
		// these months are completely in DST
		dst = true;
	} else {
		// else we must be in one of the change months
		// work out when the last sunday was (could be today)
		int previousSunday = time->tm_mday - time->tm_wday;
		if (time->tm_mon == 2) { // march
			// was last sunday (which could be today) the last sunday in march
			if (previousSunday >= 25) {
				// are we actually on the last sunday today
				if (time->tm_wday == 0) {
					// if so are we at/past 2am gmt
					int s = (time->tm_hour * 3600) + (time->tm_min * 60) + time->tm_sec;
					if (s >= 7200)
						dst = true;
				} else {
					dst = true;
				}
			}
		} else if (time->tm_mon == 9) {
			// was last sunday (which could be today) the last sunday in october
			if (previousSunday >= 25) {
				// we have reached/passed it, so is it today?
				if (time->tm_wday == 0) {
					// change day, so are we before 1am gmt (2am localtime)
					int s = (time->tm_hour * 3600) + (time->tm_min * 60) + time->tm_sec;
					if (s < 3600)
						dst = true;
				}
			} else {
				// not reached the last sunday yet
				dst = true;
			}
		}
	}

	return dst;
}

/**
 ******************************************************************
 * @brief  Apply daylight saving time (if so).
 * @author Holger Mueller
 * @date   2017-07-06
 *
 * @param  timestamp - timestamp to check and apply.
 * @return Corrected timestamp.
 ******************************************************************
 */
time_t ICACHE_FLASH_ATTR
applyDST(time_t timestamp)
{
	if (isDST(timestamp)) {
		// add the dst hour
		timestamp += 3600;
	}
	return timestamp;
}
