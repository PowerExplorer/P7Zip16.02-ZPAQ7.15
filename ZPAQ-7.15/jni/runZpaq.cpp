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

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/example/hellojni/HelloJni.java
 */
 
#ifndef _RUNZPAQ_H_
#define _RUNZPAQ_H_

extern int main(int numArgs, const char *argv[]);

#endif  // _RUNZPAQ_H_

extern "C"
{
	JNIEXPORT jint JNICALL Java_net_gnu_zpaq_Zpaq_runZpaq
		(JNIEnv *env, jobject thisObj, jobjectArray inJNIArray) {

		jint ret = 0;
		try {
			// Get a class reference for java.lang.String
			//jclass classString = (env)->FindClass("java/lang/String");
			//jmethodID midValueOf = (env)->GetStaticMethodID(classString, "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
			//if (NULL == midValueOf) return NULL;

			jsize length = (env)->GetArrayLength(inJNIArray);
			if (length <= 1) {
				return 2;
			}
			const char *args[length + 2] = {};
			args[0] = "zpaq";
			args[length + 1] = 0;
			
			jstring jst[length + 1];
			jst[length] = 0;
			
			for (int i = 0; i < length; i++) {
				jst[i] = (jstring)(env)->GetObjectArrayElement(inJNIArray, i);
				//jstring jstemp = (jstring)(env)->CallStaticObjectMethod(classString, midValueOf, jst[i]);
				args[i+1] = env->GetStringUTFChars(jst[i], 0);
			}
			ret = main(length + 1, args);
			for (int i = 0; i < length; i++) {
				(env)->ReleaseStringUTFChars(jst[i], args[i + 1]);
			}
		} catch (...) {
			ret = 2;
		}
		return ret;
	}

}
