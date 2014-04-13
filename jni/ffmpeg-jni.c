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
#include <malloc.h>
#include <android/log.h>
//#include <android/bitmap.h>

#include "typeDef.h"


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
	int top;
	int left;
	int tarWid;
	int tarHei;
}t_picSize;


void NV21toRGB(u8 *nv21,u32 *rgb, t_picSize *picSize)
{
	int w,h;

	for(h=0; h<picSize->tarHei; h++)
	{
		int hBase=h*picSize->tarWid;
		int srcHbase=(picSize->top+h)*picSize->srcWid;
		for(w=0; w<picSize->tarWid; w++)
		{
			int Addr=hBase+w;
			int srcAddr=srcHbase+w;
			u32 r,g,b;

			u8 srcY=nv21[srcAddr];
			r=g=b=srcY;

			rgb[Addr]=0xFF000000|(r<<16)|(g<<8)|b;
		}
	}
}

jboolean Java_com_droidipc_PlaybackViewCB_OnPreviewFrame(JNIEnv* env, jobject thiz, jbyteArray jnv21, jintArray jrgb, jintArray jPicSize)
{
	u8 *nv21=(*env)->GetByteArrayElements(env, jnv21, NULL);
	u32 *rgb=(*env)->GetIntArrayElements(env, jrgb, NULL);
	t_picSize *picSize=(t_picSize*)((*env)->GetByteArrayElements(env, jPicSize, NULL));

	if((nv21==NULL)||(rgb==NULL)||(picSize==NULL))
	{
		LOGI("GetbyteArrayElements Fail!");
		return JNI_FALSE;
	}

	//LOGI("surface width:%d height:%d", picSize->srcWid, picSize->srcHei);
	//LOGI("bitmap width:%d height:%d", picSize->tarWid, picSize->tarHei);
	//LOGI("pos top:%d left:%d", picSize->top, picSize->left);

	if(((picSize->top+picSize->tarHei)>picSize->srcHei)||
	   ((picSize->left+picSize->tarWid)>picSize->srcWid))
	{
		LOGI("Size or Pos Error!");
		return JNI_FALSE;
	}

/**/

	NV21toRGB(nv21, rgb, picSize);

	/*
	{
		int i;
		int pix=0;
		int size=(*env)->GetArrayLength(env, jrgb);
		for(i=0; i<size; i++)
		{
			if(rgb[i]!=pix)
			{
				pix=rgb[i];
				LOGI("diff @rgb[%d]=%08X", i, pix);
			}
		}
	}
	*/

	//LOGI("ArrayElement size: %d", (*env)->GetArrayLength(env, jbuf));

	(*env)->ReleaseByteArrayElements(env, jnv21, nv21, 0);
	(*env)->ReleaseIntArrayElements(env, jrgb, rgb, 0);
	(*env)->ReleaseIntArrayElements(env, jPicSize, (int*)picSize, 0);
	return JNI_TRUE;
}



