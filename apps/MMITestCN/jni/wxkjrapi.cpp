/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define ALOG_TAG "wxkjrapijni"
#define ALOG_NDEBUG 0
#include <utils/Log.h>

#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdarg.h>
#include <errno.h>
#include <string.h>
/*
#include "comdef.h"

#include "snd.h"
#include "oem_rapi.h"
*/
#include "jni.h"

#include "../../../mediatek/external/nvram/libnvram/libnvram.h"
#include "Custom_NvRam_LID.h"
//#include "CFG_Trace_File.h"
//#include "CFG_Trace_Default.h"

#define DEBUG_TEST

#define INT_TO_LBYTE(a) ( (uint8_t) ( a & 0xFF) )
#define INT_TO_HBYTE(a) ( (uint8_t) ((a >> 8) & 0xFF) )

#define MAX_CMD_LEN 8

#define AP_CFG_CUSTOM_FILE_TRACE_LID 1

#define MMCBLKP		"/dev/pro_info"

static jbyteArray read_trace_parti(JNIEnv *env, jobject thiz) 
{
	int status = 0;
	jstring str;
	int fd = 0;
	jbyteArray ret;
	char buf[512];
	unsigned char *p = NULL;
	int index = 0;
	int checksum_len = 0;
	unsigned short checksum;
	int lk;

	//read patti
	if( (fd = open(MMCBLKP, O_RDONLY)) < 0 ) { 
		printf("open %s failed\n", MMCBLKP);
		return NULL; 
	}
	
	
	memset(buf,0,sizeof(buf));

	if( (status = read(fd,buf,512)) < 0){ 
		printf("read %s failed : %d\n", MMCBLKP, errno);
		close(fd);
		return NULL;
	}   
	close(fd);

	ret = env->NewByteArray((jsize)sizeof(buf));
	env->SetByteArrayRegion(ret, 0, (jsize)sizeof(buf), (jbyte*)buf);
	return ret;
}

static int pack_n_int(uint8_t *buf,int n, ...) {
 int i=0;
 va_list args;
 va_start(args,n);
    for(i=0;i<n;i++) {
        int current = va_arg(args, int);
        buf[i*2]  = INT_TO_LBYTE(current);
        buf[i*2+1]= INT_TO_HBYTE(current);
    }
 va_end(args);
 return n;
}


int writefifo(const char *name, uint8_t *buffer, int len) {

  int count=0;
  int fifo=0;
  int result=0;
  
  if ( (fifo = open(name, O_WRONLY)) == -1 ) {
 //     ALOGV("can't open fifo");
      result = -1;
  }else {
      count=0;
      do{
          result = write(fifo, &buffer[count], len - count);
          count+= result;
      }while(count < len && result > 0);
    
      (void) close(fifo);
  } 
  return result;
}

int readfifo(const char *name, uint8_t *buffer, int max) {

  int result=0;
  int nread=0;
  int fifo=0;

  if ( (fifo = open(name, O_RDONLY)) == -1 ) {
 //     ALOGV("can't open fifo");
      result = -1;
  }else {
      nread = 0;
      do {
          result = read(fifo, &buffer[nread], max-nread);
          nread+=result;
      }while( result > 0 );
      
 //     ALOGV("received from modem : %d bytes : %s\n", nread, buffer);
      
      (void) close(fifo);
  }
  
  return result;
}



static jint 
sendcmd(JNIEnv *env, jobject thiz, jstring name, jshort cmd, jshort para1, jshort para2, jshort para3) {
    
  const char *fifo_name;
  char fifo_name_read[100];
  char fifo_name_write[100];
  
  int fifo;
  uint8_t bytes[8]={0};
  uint8_t readbuf[128];
  jint result=0;
  
  fifo_name = env->GetStringUTFChars(name, NULL);
  if (name == NULL) {
      return -1; /* OutOfMemoryError already thrown */
  }

  strncpy(fifo_name_read,fifo_name,100);
  strncpy(fifo_name_write,fifo_name,100);
  strcat(fifo_name_read,"_read");
  strcat(fifo_name_write,"_write");
 
 // ALOGV("fifo > proxy  is %s", fifo_name_read);
 // ALOGV("fifo < proxy  is %s", fifo_name_write);
 
  pack_n_int(bytes,4, cmd, para1, para2, para3);
  memset(readbuf,0,sizeof(readbuf));
    
  if( writefifo(fifo_name_read, bytes, MAX_CMD_LEN) < MAX_CMD_LEN ) {
      result = -2;
  }else if ( (result = readfifo(fifo_name_write, readbuf,sizeof(readbuf))) < 0) {
      result = -3;
  }else if (strstr((char *)readbuf, "ERROR") != NULL ) {
      result = -4;
  }

  env->ReleaseStringUTFChars(name, fifo_name);

  return result;
}




static jstring
sendcmdForStrResult(JNIEnv *env, jobject thiz, jstring name, jshort cmd, jshort para1, jshort para2, jshort para3){
  const char *fifo_name;
  
  int fifo;
  uint8_t bytes[8]={0};
  uint8_t readbuf[128];
  jint result=0;
  jstring str;
  
  fifo_name = env->GetStringUTFChars(name, NULL);
  if (name == NULL) {
      return NULL;
  }
  
  pack_n_int(bytes,4, cmd, para1, para2, para3);
  memset(readbuf,0,sizeof(readbuf));
    
  if( writefifo(fifo_name, bytes, MAX_CMD_LEN) < MAX_CMD_LEN ) {
      result = -2;
  }else if ( (result = readfifo(fifo_name, readbuf,sizeof(readbuf))) < 0) {
      result = -3;
  }else if (strstr((char *)readbuf, "ERROR") != NULL ) {
      result = -4;
  }
  
  if ( strlen((char *)readbuf) > 0 ) {
      str = env-> NewStringUTF((char *)readbuf);
  }else{
      str = NULL;
  }

  env->ReleaseStringUTFChars(name, fifo_name);

  return str;
}

static jstring
sendcmdForStrResult2(JNIEnv *env, jobject thiz, jstring name, jshort cmd, jshort para1, jshort para2, jshort para3){
  const char *fifo_name;
  char fifo_name_read[100];
  char fifo_name_write[100];
  
  int fifo;
  uint8_t bytes[8]={0};
  uint8_t readbuf[128];
  jint result=0;
  jstring str;
  
  if (name == NULL) {
      return NULL;
  }

  fifo_name = env->GetStringUTFChars(name, NULL);
  
  strncpy(fifo_name_read,fifo_name,100);
  strncpy(fifo_name_write,fifo_name,100);
  strcat(fifo_name_read,"_read");
  strcat(fifo_name_write,"_write");
 
// ALOGV("fifo > proxy  is %s", fifo_name_read);
//  ALOGV("fifo < proxy  is %s", fifo_name_write);
 
  pack_n_int(bytes,4, cmd, para1, para2, para3);
  memset(readbuf,0,sizeof(readbuf));
    
  if( writefifo(fifo_name_read, bytes, MAX_CMD_LEN) < MAX_CMD_LEN ) {
      result = -2;
  }else if ( (result = readfifo(fifo_name_write, readbuf,sizeof(readbuf))) < 0) {
      result = -3;
  }else if (strstr((char *)readbuf, "ERROR") != NULL ) {
      result = -4;
  }
  
  if ( strlen((char *)readbuf) > 0 ) {
      str = env-> NewStringUTF((char *)readbuf);
  }else{
      str = NULL;
  }

  env->ReleaseStringUTFChars(name, fifo_name);

  return str;
}

static jint
doNvRead(JNIEnv *env, jobject thiz, jint item, jbyteArray buf)
{
   int nvram_fd = 0;
   int file_lid = AP_CFG_CUSTOM_FILE_TRACE_LID;
   int i = 0,rec_sizem,rec_size,rec_num,result;
   jbyte *jbuf = env->GetByteArrayElements(buf,0);

  // nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
   result = read(nvram_fd, jbuf, rec_size*rec_num);
  // NVM_CloseFileDesc(nvram_fd);
#ifdef DEBUG_TEST
   // memcpy(jbuf, (void *)&stTraceConfigDefault, sizeof(ap_nvram_trace_config_struct));
 //  ALOGD("nv buf end\n");
#endif
   return result;
}

static jint 
doNvWrite(JNIEnv *env, jobject thiz, jint item, jbyteArray buf)
{
   int nvram_fd = 0;
   int file_lid = AP_CFG_CUSTOM_FILE_TRACE_LID;
   int i = 0,rec_sizem,rec_size,rec_num,result;
   jbyte *jbuf = env->GetByteArrayElements(buf,0);

  // nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
   result = write(nvram_fd, jbuf, rec_size*rec_num);
 //  NVM_CloseFileDesc(nvram_fd);
   return result;
}

static const char *classPathName = "com/nb/mmitest/WXKJRapi";

static JNINativeMethod methods[] = {
  {"sendcmd", "(Ljava/lang/String;SSSS)I", (void*)sendcmd },
  {"sendcmdForStrResult", "(Ljava/lang/String;SSSS)Ljava/lang/String;", (void*)sendcmdForStrResult2 },
  {"doNvRead", "(I[B)I", (void*)doNvRead},
  {"doNvWrite", "(I[B)I", (void*)doNvWrite},
  {"read_trace_parti", "()[B", (void*)read_trace_parti },
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
   //     ALOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
    //    ALOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env)
{
  if (!registerNativeMethods(env, classPathName,
                 methods, sizeof(methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}


// ----------------------------------------------------------------------------

/*
 * This is called by the VM when the shared library is first loaded.
 */
 
typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;
    
   // ALOGI("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
  //      ALOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
  //      ALOGE("ERROR: registerNatives failed");
        goto bail;
    }
    
    result = JNI_VERSION_1_4;
    
bail:
    return result;
}
