/*
 * Copyright (C) 2009 The Android Open Source Project
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
 *
 */
#include <string.h>
#include <jni.h>
#include <android/log.h>
//#include <android/bitmap.h>

#define  LOG_TAG    "jni-libFfmpeg"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/example/hellojni/HelloJni.java
 */
jstring
Java_com_droidipc_MainActivityIPC_stringFromJNI( JNIEnv* env,
                                                  jobject thiz )
{
#if defined(__arm__)
  #if defined(__ARM_ARCH_7A__)
    #if defined(__ARM_NEON__)
      #define ABI "armeabi-v7a/NEON"
    #else
      #define ABI "armeabi-v7a"
    #endif
  #else
   #define ABI "armeabi"
  #endif
#elif defined(__i386__)
   #define ABI "x86"
#elif defined(__mips__)
   #define ABI "mips"
#else
   #define ABI "unknown"
#endif

    return (*env)->NewStringUTF(env, "test jni !  Compiled with ABI " ABI ".");
}


typedef struct
{
	int srcWid;
	int srcHei;
	int tarWid;
	int tarHei;
}t_picSize;


void NV21toRGB565(jbyte *nv21, jbyte *rgb565, jint width, jint height)
{

}

void Java_com_droidipc_PlaybackViewCB_OnPreviewFrame( JNIEnv* env, jobject thiz, jbyteArray jbuf, jintArray jPicSize)
{
	t_picSize *picSize=(t_picSize*)((*env)->GetByteArrayElements(env, jPicSize, NULL));
	jbyte *buf=(*env)->GetByteArrayElements(env, jbuf, NULL);

	if((picSize==NULL)||(buf==NULL))
	{
		LOGI("JNI:GetbyteArrayElements Fail!");
		return;
	}

	LOGI("surface width:%d height:%d", picSize->srcWid, picSize->srcHei);
	LOGI("bitmap width:%d height:%d", picSize->tarWid, picSize->tarHei);
	//LOGI("ArrayElement size: %d", (*env)->GetArrayLength(env, jbuf));

	(*env)->ReleaseByteArrayElements(env, jPicSize, (int*)picSize,0);
	(*env)->ReleaseByteArrayElements(env, jbuf, buf,0);
}



