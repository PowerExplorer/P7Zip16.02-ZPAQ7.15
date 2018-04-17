# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cpp .cc
LOCAL_MODULE    := zpaq
LOCAL_SRC_FILES := libzpaq.cpp zpaq.cpp redirection-jni_zpaq.cpp runZpaq.cpp 

LOCAL_CPPFLAGS += -Dunix -pthread \
 -O3 -static \
 -s \
 -DANDROID_NDK -fexceptions \
 -D_REENTRANT -DENV_UNIX \
 -DUNICODE -D_UNICODE -DUNIX_USE_WIN_FILE \
 -DBREAK_HANDLER -D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE 

LOCAL_CFLAGS += -fPIE
#LOCAL_LDFLAGS += -fPIE -pie

ifeq ($(TARGET_ARCH_ABI),x86)
    LOCAL_CFLAGS += -ffast-math -mtune=atom -mssse3 -mfpmath=sse
endif

LOCAL_CPPFLAGS += $(LOCAL_CFLAGS)

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
#include $(BUILD_EXECUTABLE)

