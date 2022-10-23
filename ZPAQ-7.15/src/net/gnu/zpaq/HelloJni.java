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
 */
package net.gnu.zpaq;

import android.app.Activity;
import android.widget.TextView;
import android.os.Bundle;
import java.io.*;
import android.util.*;


public class HelloJni extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Create a TextView and set its content.
         * the text is retrieved by calling a native
         * function.
         */
        TextView  tv = new TextView(this);
        try {
			String toString = new Zpaq(this, null).runZpaq(true, "a", "/sdcard/zpaq5.zpaq", "/sdcard/zpaq")[1].toString();
			tv.setText(toString);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
        setContentView(tv);
    }

    
}
