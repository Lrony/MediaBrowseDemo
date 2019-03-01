package com.lrony.mediabrowsedemo.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.lrony.mediabrowsedemo.R;

import java.util.Formatter;
import java.util.Locale;

public class MusicUtils {
    private static final String TAG = "MusicUtils";

    private static final int ONE_HOUR = 3600;

    public static Bitmap resizeBitmap(Bitmap bitmap, Bitmap ref) {
        int w = ref.getWidth();
        int h = ref.getHeight();
        return Bitmap.createScaledBitmap(bitmap, w, h, false);
    }

    @SuppressLint("ResourceType")
    public static Bitmap getDefaultAlbumArt(Context context) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeStream(
                context.getResources().openRawResource(R.drawable.albumart_mp_unknown), null,
                opts);
    }

    /*  Try to use String.format() as little as possible, because it creates a
     *  new Formatter every time you call it, which is very inefficient.
     *  Reusing an existing Formatter more than tripled the speed of
     *  makeTimeString().
     *  This Formatter/StringBuilder are also used by makeAlbumSongsLabel()
     */
    private static StringBuilder sFormatBuilder = new StringBuilder();
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    private static final Object[] sTimeArgs = new Object[5];

    public static String makeTimeString(Context context, long secs) {
        String durationformat = context.getString(
                secs < ONE_HOUR ? R.string.usb_audio_durationformatshort :
                        R.string.usb_audio_durationformatlong);

        /* Provide multiple arguments so the format can be changed easily
         * by modifying the xml.
         */
        sFormatBuilder.setLength(0);

        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;

        return sFormatter.format(durationformat, timeArgs).toString();
    }
}
