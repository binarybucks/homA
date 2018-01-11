/**
 * @file
 * @brief Common user functions
 */
#include <osapi.h>
#include <stdlib.h>
#include "user_common.h"


/**
 ******************************************************************
 * @brief  Float to ASCII with precision 1
 * @author Holger Mueller
 * @date   2015-10-08
 *
 * @param  *s - Buffer to convert to.
 * @param  f - Float to convert.
 * @return Returns pointer to s string.
 ******************************************************************
 */
char * ICACHE_FLASH_ATTR
ftoa(char *s, float f) {
	os_sprintf(s, "%d.%d",
		    (int) f, abs((int) ((f - (int) f) * 10)));
	return s;
}

/**
 ******************************************************************
 * @brief  Integer to ASCII
 * @author Holger Mueller
 * @date   2017-06-07
 *
 * @param  *s - Buffer to convert to.
 * @param  i - Integer to convert.
 * @return Returns pointer to s string.
 ******************************************************************
 */
char * ICACHE_FLASH_ATTR
itoa(char *s, uint16_t i) {
	os_sprintf(s, "%d", i);
	return s;
}
