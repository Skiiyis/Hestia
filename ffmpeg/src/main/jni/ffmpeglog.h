#include <android/log.h>

#ifndef FFMEPG_SUPPORT_ANDROID_LOG
#define FFMEPG_SUPPORT_ANDROID_LOG

int ISDEBUG = 1;

#define LOGE(debug, format, ...) if(debug){__android_log_print(ANDROID_LOG_ERROR, "ffmpeg", format, ##__VA_ARGS__);}
#define LOGI(debug, format, ...) if(debug){__android_log_print(ANDROID_LOG_INFO, "ffmpeg", format, ##__VA_ARGS__);}

#endif