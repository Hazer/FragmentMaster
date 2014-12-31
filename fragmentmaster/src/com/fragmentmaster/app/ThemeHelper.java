package com.fragmentmaster.app;

import android.content.Context;
import android.util.TypedValue;

import com.fragmentmaster.R;
import com.fragmentmaster.annotation.Configuration;

public class ThemeHelper {

    public static int getMasterFragmentTheme(Context context, IMasterFragment fragment) {
        int masterFragmentTheme = -1;
        Class clazz = fragment.getClass();
        // Get theme from Configuration annotation.
        if (clazz.isAnnotationPresent(Configuration.class)) {
            Configuration configuration = (Configuration) clazz.getAnnotation(Configuration.class);
            masterFragmentTheme = configuration.theme();
        }
        // Get theme from Theme attrs.
        if (masterFragmentTheme == -1) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.masterFragmentTheme, outValue, true);
            masterFragmentTheme = outValue.resourceId;
        }
        return masterFragmentTheme;
    }

    public static int getMasterFragmentBackground(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.windowBackground, outValue, true);
        return outValue.resourceId;
    }
}