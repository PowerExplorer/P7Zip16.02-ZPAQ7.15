/*
 * Copyright 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.classysharkandroid.dex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;

//import com.google.classysharkandroid.utils.IOUtils;

import dalvik.system.DexClassLoader;
import net.gnu.util.*;

public class DexLoaderBuilder {

    private static final int BUF_SIZE = 64 * 1024;

    private DexLoaderBuilder() {
    }

    public static DexClassLoader fromFile(Context context, final File dexFile) throws Exception {

        //FileInputStream fileInputStream = new FileInputStream(dexFile);

        //byte[] bFile = IOUtils.toByteArray(fileInputStream);
		byte[] bFile = FileUtil.readFileToMemory(dexFile);
		
        return fromBytes(context, bFile);
    }

    public static DexClassLoader fromBytes(Context context, final byte[] dexBytes) throws Exception {

        if (null == context) {
            throw new RuntimeException("No context provided");
        }

        final String dexFileName = "internal.dex";

        final File dexInternalStoragePath = new File(context.getDir("dex", Context.MODE_PRIVATE), dexFileName);

        if (!dexInternalStoragePath.exists()) {

            prepareDex(dexBytes, dexInternalStoragePath);
        }

        final File optimizedDexOutputPath = context.getCodeCacheDir();

        DexClassLoader loader = new DexClassLoader(dexInternalStoragePath.getAbsolutePath(),
                optimizedDexOutputPath.getAbsolutePath(), null, context.getClassLoader().getParent());

        dexInternalStoragePath.delete();
        return loader;
    }

    private static boolean prepareDex(byte[] bytes, File dexInternalStoragePath) {
        BufferedInputStream bis = null;
        OutputStream dexWriter = null;
        try {
            bis = new BufferedInputStream(new ByteArrayInputStream(bytes));
            dexWriter = new BufferedOutputStream(new FileOutputStream(dexInternalStoragePath));
            final byte[] buf = new byte[BUF_SIZE];
            int len;
            while ((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                dexWriter.write(buf, 0, len);
            }
            return true;
        } catch (IOException e) {
            return false;
        } finally {
			FileUtil.flushClose(dexWriter);
			FileUtil.close(bis);
		}
    }
}
