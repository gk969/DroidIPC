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


void NV21toRGB(jbyte *nv21, jint *rgb, t_picSize *picSize)
{
	int w,h,wp,hp;
	int r,g,b;

	int tarbase=picSize->top*picSize->srcWid+picSize->left;

	picSize->top&=0xFFFFFFFC;
	picSize->left&=0xFFFFFFFC;

	for(h=0; h<picSize->tarHei; h+=4)
	{
		int hBase=h*picSize->tarWid;		//plane height base addr
		int srcHbase=tarbase+h*picSize->srcWid;
		for(w=0; w<picSize->tarWid; w+=4)
		{
			int pBase=hBase+w;				//plane base addr
			int srcPbase=(srcHbase+w)/2*3;
			for(hp=0; hp<4; hp++)
			{
				int hPos=pBase+hp*4;		//pix height addr in plane
				int srcHYpos=srcPbase+hp*4;
				for(wp=0; wp<4; wp++)
				{
					int pPos=hPos+wp;	//pix addr in plane
					unsigned char srcY=nv21[srcHYpos+wp];
					r=g=b=srcY;

					*(rgb+pPos)=0xFF000000|(r<<16)|(g<<8)|b;
				}
			}
		}
	}
}

jboolean Java_com_droidipc_PlaybackViewCB_OnPreviewFrame(JNIEnv* env, jobject thiz, jbyteArray jnv21, jintArray jrgb, jintArray jPicSize)
{
	jbyte *nv21=(*env)->GetByteArrayElements(env, jnv21, NULL);
	jint *rgb=(*env)->GetIntArrayElements(env, jrgb, NULL);
	t_picSize *picSize=(t_picSize*)((*env)->GetByteArrayElements(env, jPicSize, NULL));

	if((nv21==NULL)||(rgb==NULL)||(picSize==NULL))
	{
		LOGI("GetbyteArrayElements Fail!");
		return JNI_FALSE;
	}

	//LOGI("surface width:%d height:%d", picSize->srcWid, picSize->srcHei);
	//LOGI("bitmap width:%d height:%d", picSize->tarWid, picSize->tarHei);
	LOGI("pos top:%d left:%d", picSize->top, picSize->left);

	if((((picSize->srcWid|picSize->srcHei)&0x03)!=0)||
	   ((picSize->top+picSize->tarHei)>picSize->srcHei)||
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



