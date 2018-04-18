/*
 * Copyright (C) 2006 The Android Open Source Project
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

package jp.sblo.pandora.jota.text.style;

import jp.sblo.pandora.jota.text.TextUtils;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

/**
 * Changes the typeface family of the text to which the span is attached.
 */
public class TypefaceSpan extends MetricAffectingSpan implements ParcelableSpan {
    private final String mFamily;

    /**
     * @param family The font family for this typeface.  Examples include
     * "monospace", "serif", and "sans-serif".
     */
    public TypefaceSpan(String family) {
        mFamily = family;
    }

    public TypefaceSpan(Parcel src) {
        mFamily = src.readString();
    }

    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        writeToParcelInternal(dest, flags);
    }

    /**
     * Returns the font family name.
     */
    public String getFamily() {
        return mFamily;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        apply(ds, mFamily);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        apply(paint, mFamily);
    }

    private static void apply(Paint paint, String family) {
        int oldStyle;

        Typeface old = paint.getTypeface();
        if (old == null) {
            oldStyle = 0;
        } else {
            oldStyle = old.getStyle();
        }

        Typeface tf = Typeface.create(family, oldStyle);
        int fake = oldStyle & ~tf.getStyle();

        if ((fake & Typeface.BOLD) != 0) {
            paint.setFakeBoldText(true);
        }

        if ((fake & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }

        paint.setTypeface(tf);
    }
    public int getSpanTypeIdInternal() {
        return TextUtils.TYPEFACE_SPAN;
    }
    public void writeToParcelInternal(Parcel dest, int flags) {
        dest.writeString(mFamily);
    }
}
