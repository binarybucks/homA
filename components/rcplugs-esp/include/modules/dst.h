#ifndef __DST_H__
#define __DST_H__

#include <c_types.h>
#include <time.h>

#ifdef __cplusplus
extern "C" {
#endif

bool isDST(time_t timestamp);
time_t applyDST(time_t timestamp);

#ifdef __cplusplus
}
#endif

#endif // __DST_H__
