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
	u32 w,h;
	u32 uvOfs=picSize->srcWid*picSize->srcHei;

	picSize->top&=0xFFFFFFFE;
	picSize->left&=0xFFFFFFFE;

	for(h=0; h<picSize->tarHei; h++)
	{
		u32 hBase=h*picSize->tarWid;
		u32 srcYbase=(picSize->top+h)*picSize->srcWid+picSize->left;
		u32 srcUVbase=(picSize->top+h)/2*picSize->srcWid+picSize->left+uvOfs;
		for(w=0; w<picSize->tarWid; w++)
		{
			u32 Addr=hBase+w;
			u32 srcYaddr=srcYbase+w;
			u32 srcUVaddr=srcUVbase+w&0xFFFFFFFE;
			u8 y,u,v;
			u8 r,g,b;
			y=nv21[srcYaddr];
			v=nv21[srcUVaddr];
			u=nv21[srcUVaddr+1];


			r=y+1.4075*(v-128);
			g=y-0.3455*(u-128)-0.7196*(v-128);
			b=y+1.779*(u-128);

/*
			r=y+1.13983*(v-128);
			g=y-0.39465*(u-128)-0.58060*(v-128);
			b=y+2.03211*(u-128);
*/



			//r=g=b=(w+h);

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

	//LOGI("ArrayElement size: %d", (*env)->GetArrayLength(env, jbuf));

	(*env)->ReleaseByteArrayElements(env, jnv21, nv21, 0);
	(*env)->ReleaseIntArrayElements(env, jrgb, rgb, 0);
	(*env)->ReleaseIntArrayElements(env, jPicSize, (int*)picSize, 0);
	return JNI_TRUE;
}



