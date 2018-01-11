/**
 * @file
 * @brief Common user functions and definitions
 * $Id: user_common.h 16 2015-10-29 21:49:45Z hmueller $
 */
#ifndef __USER_COMMON_H__
#define __USER_COMMON_H__

#include <osapi.h>
#include "user_config.h"

/*
#define INFO os_printf
#define ERROR os_printf
#ifdef ESP_DEBUG
#define DEBUG os_printf
#else
#define DEBUG
#endif // ESP_DEBUG
*/

#ifdef __cplusplus
extern "C" {
#endif

char *ftoa(char *s, float f);
char *itoa(char *s, uint16_t i);

#ifdef __cplusplus
}
#endif

#endif /* __USER_COMMON_H__ */
