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
#include <sys/stat.h>
#include <sys/types.h>
#include <android/log.h>
#include <fcntl.h>
#include <stdio.h>

#ifdef __cplusplus
extern "C" {
#endif

int fdi = 0;
int fdo = 0;

jstring
Java_com_free_zpaq_Zpaq_stringFromJNI( JNIEnv* env,
                                                  jobject thiz, jstring jOutfile, jstring jInfile )
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

	const char* outfile = env->GetStringUTFChars(jOutfile, 0);
	const char* infile = env->GetStringUTFChars(jInfile, 0);

	/*
	 * Step 1: Make a named pipe
	 * Step 2: Open the pipe in Write only mode. Java code will open it in Read only mode.
	 * Step 3: Make STDOUT i.e. 1, a duplicate of opened pipe file descriptor.
	 * Step 4: Any writes from now on to STDOUT will be redirected to the the pipe and can be read by Java code.
	 */
	int out = mkfifo(outfile, 0664);
	/*int */fdo = open(outfile, O_WRONLY);

	int in = mkfifo(infile, 0664); // Make named input file here for synchronization
	//fflush(stdout);
	dup2(fdo, 1);
	setbuf(stdout, NULL);
	//fprintf(stdout, "This string will be written to %s test", outfile);
	//fprintf(stdout, "This string will be written to %s", outfile);
	//fprintf(stdout, "\n");
	fflush(stdout);
	// close(fdo);

	/*
	 * Step 1: Make a named pipe
	 * Step 2: Open the pipe in Read only mode. Java code will open it in Write only mode.
	 * Step 3: Make STDIN i.e. 0, a duplicate of opened pipe file descriptor.
	 * Step 4: Any reads from STDIN, will be actually read from the pipe and JAVA code will perform write operations.
	 */

	/*int */fdi = open(infile, O_RDONLY);
	dup2(fdi, 0);
	char buf[256] = "";
	fscanf(stdin, "%*s %99[^\n]", buf); // Use this format to read white spaces.
	// close(fdi);
	__android_log_write(ANDROID_LOG_ERROR, "Redirection1", buf);

	env->ReleaseStringUTFChars(jOutfile, outfile);
	env->ReleaseStringUTFChars(jInfile, infile);

    return env->NewStringUTF(buf);
}

void
Java_com_free_zpaq_Zpaq_closeStreamJNI( JNIEnv* env, jobject thiz ) {
	close(fdi);
	close(fdo);
}

#ifdef __cplusplus
}
#endif
