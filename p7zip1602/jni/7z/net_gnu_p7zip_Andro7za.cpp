#include <string.h>
#include "7za_main.h"
#include "net_gnu_p7zip_Andro7za.h"

//#define ARGC 8
//static const char *test_args[ARGC + 1] =
//		{ "7za", 
//		"x", 
//		"/mnt/sdcard/7za123456789.7z",
//		"-o/mnt/sdcard/extractarchiveandroid",
//		"-aos", 
//		"", 
//		"", 
//		"",
//		0 };
//		
//#define ARGC2 19
//static const char *test_args2[ARGC2 + 1] =
//		{ "7za", 
//		"x", 
//		"/mnt/sdcard/7za123456789.7z",
//		"-o/mnt/sdcard/extractarchiveandroid",
//		"-aos", 
//		"", 
//		"", 
//		"", //1
//		"", //2
//		"", //3
//		"", //4
//		"", //5
//		"", //6
//		"", //7
//		"", //8
//		"", //9
//		"", //10
//		"", //11
//		"", //12
//		0 };
//		
//		
JNIEXPORT jint JNICALL Java_net_gnu_p7zip_Andro7za_a7zaCommandAll
		(JNIEnv *env, jobject thisObj, jobjectArray inJNIArray) {

		jint ret = 0;
		try {
			// Get a class reference for java.lang.String
			//jclass classString = (env)->FindClass("java/lang/String");
			//jmethodID midValueOf = (env)->GetStaticMethodID(classString, "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
			//if (NULL == midValueOf) return NULL;

			jsize length = (env)->GetArrayLength(inJNIArray);
			
			const char *args[length + 2] = {};
			args[0] = "7z";
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

//
//JNIEXPORT jint JNICALL Java_net_gnu_p7zip_Andro7za_a7zaCommand(
//		JNIEnv *env, jobject obj, 
//		jstring _command, 
//		jstring _pathArchive,
//		jstring _type,
//		jstring _password,
//		jstring _outputDirOrCompressionLevel,
//		jstring _exclude,
//		jstring _fList4CompressOrExtract) {
//
//	const char *command = env->GetStringUTFChars(_command, 0);
//	const char *pathArchive = env->GetStringUTFChars(_pathArchive, 0);
//	const char *type = env->GetStringUTFChars(_type, 0);
//	const char *password = env->GetStringUTFChars(_password, 0);
//	const char *outputDirOrCompressionLevel = env->GetStringUTFChars(_outputDirOrCompressionLevel, 0);
//	const char *exclude = env->GetStringUTFChars(_exclude, 0);
//	const char *fList4CompressOrExtract = env->GetStringUTFChars(_fList4CompressOrExtract, 0);
//		
//	test_args[1] = command;
//	test_args[2] = pathArchive;
//	test_args[3] = type;
//	test_args[4] = password;
//	test_args[5] = outputDirOrCompressionLevel;
//	test_args[6] = exclude;
//	test_args[7] = fList4CompressOrExtract;
//	
//	jint ret;
//	try {
//		ret = main(ARGC, test_args);
//	} catch (...) {
//		ret = 2;
//	}
//
//	// Release strings
//	env->ReleaseStringUTFChars(_command, command);
//    env->ReleaseStringUTFChars(_pathArchive, pathArchive);
//    env->ReleaseStringUTFChars(_type, type);
//	env->ReleaseStringUTFChars(_password, password);
//    env->ReleaseStringUTFChars(_outputDirOrCompressionLevel, outputDirOrCompressionLevel);
//    env->ReleaseStringUTFChars(_exclude, exclude);
//    env->ReleaseStringUTFChars(_fList4CompressOrExtract, fList4CompressOrExtract);
//	
//	return ret;
//}
//
//JNIEXPORT jint JNICALL Java_net_gnu_p7zip_Andro7za_a7zaCommand2(
//		JNIEnv *env, jobject obj, 
//		jstring _command, 
//		jstring _pathArchive,
//		jstring _type,
//		jstring _password,
//		jstring _outputDirOrCompressionLevel,
//		jstring _1,
//		jstring _2,
//		jstring _3,
//		jstring _4,
//		jstring _5,
//		jstring _6,
//		jstring _7,
//		jstring _8,
//		jstring _9,
//		jstring _10,
//		jstring _11,
//		jstring _12,
//		jstring _fList4CompressOrExtract
//		) {
//
//	const char *command = env->GetStringUTFChars(_command, 0);
//	const char *pathArchive = env->GetStringUTFChars(_pathArchive, 0);
//	const char *type = env->GetStringUTFChars(_type, 0);
//	const char *password = env->GetStringUTFChars(_password, 0);
//	const char *outputDirOrCompressionLevel = env->GetStringUTFChars(_outputDirOrCompressionLevel, 0);
//	const char *fList4CompressOrExtract = env->GetStringUTFChars(_fList4CompressOrExtract, 0);
//	
//	const char *a1 = env->GetStringUTFChars(_1, 0);
//	const char *a2 = env->GetStringUTFChars(_2, 0);
//	const char *a3 = env->GetStringUTFChars(_3, 0);
//	const char *a4 = env->GetStringUTFChars(_4, 0);
//	const char *a5 = env->GetStringUTFChars(_5, 0);
//	const char *a6 = env->GetStringUTFChars(_6, 0);
//	const char *a7 = env->GetStringUTFChars(_7, 0);
//	const char *a8 = env->GetStringUTFChars(_8, 0);
//	const char *a9 = env->GetStringUTFChars(_9, 0);
//	const char *a10 = env->GetStringUTFChars(_10, 0);
//	const char *a11 = env->GetStringUTFChars(_11, 0);
//	const char *a12 = env->GetStringUTFChars(_12, 0);
//	
//	test_args2[1] = command;
//	test_args2[2] = pathArchive;
//	test_args2[3] = type;
//	test_args2[4] = password;
//	test_args2[5] = outputDirOrCompressionLevel;
//	test_args2[6] = fList4CompressOrExtract;
//	
//	test_args2[7] = a1;
//	test_args2[8] = a2;
//	test_args2[9] = a3;
//	test_args2[10] = a4;
//	test_args2[11] = a5;
//	test_args2[12] = a6;
//	test_args2[13] = a7;
//	test_args2[14] = a8;
//	test_args2[15] = a9;
//	test_args2[16] = a10;
//	test_args2[17] = a11;
//	test_args2[18] = a12;
//	
//	jint ret;
//	try {
//		ret = main(ARGC2, test_args2);
//	} catch (...) {
//		ret = 2;
//	}
//
//	// Release strings
//	env->ReleaseStringUTFChars(_command, command);
//    env->ReleaseStringUTFChars(_pathArchive, pathArchive);
//    env->ReleaseStringUTFChars(_type, type);
//	env->ReleaseStringUTFChars(_password, password);
//    env->ReleaseStringUTFChars(_outputDirOrCompressionLevel, outputDirOrCompressionLevel);
//    env->ReleaseStringUTFChars(_fList4CompressOrExtract, fList4CompressOrExtract);
//	
//	env->ReleaseStringUTFChars(_1, a1);
//    env->ReleaseStringUTFChars(_2, a2);
//    env->ReleaseStringUTFChars(_3, a3);
//	env->ReleaseStringUTFChars(_4, a4);
//    env->ReleaseStringUTFChars(_5, a5);
//    env->ReleaseStringUTFChars(_6, a6);
//    env->ReleaseStringUTFChars(_7, a7);
//	env->ReleaseStringUTFChars(_8, a8);
//    env->ReleaseStringUTFChars(_9, a9);
//	env->ReleaseStringUTFChars(_10, a10);
//    env->ReleaseStringUTFChars(_11, a11);
//    env->ReleaseStringUTFChars(_12, a12);
//    
//	return ret;
//}
