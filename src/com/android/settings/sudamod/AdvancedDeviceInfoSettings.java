/*
 * Copyright (C) 2008 The Android Open Source Project
 * Modifications (C) 2015 The SudaMod Project
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

package com.android.settings.sudamod;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.opengl.GLES20.GL_EXTENSIONS;
import static android.opengl.GLES20.GL_RENDERER;
import static android.opengl.GLES20.GL_SHADING_LANGUAGE_VERSION;
import static android.opengl.GLES20.GL_VENDOR;
import static android.opengl.GLES20.GL_VERSION;
import static android.opengl.GLES20.glGetString;

public class AdvancedDeviceInfoSettings extends RestrictedSettingsFragment {

    private static final String LOG_TAG = "AdvancedDeviceInfoSettings";

    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private static final String FILENAME_PROC_MEMINFO = "/proc/meminfo";
    private static final String FILENAME_PROC_CPUINFO = "/proc/cpuinfo";

    private static final String KEY_KERNEL_VERSION = "kernel_version";
    private static final String KEY_DEVICE_CPU = "device_cpu";
    private static final String KEY_DEVICE_CPU_FEATURES = "device_cpu_features";
    private static final String KEY_DEVICE_MEMORY = "device_memory";
    private static final String GROUP_DEVICE_GPU = "device_gpu";

    public static final int[] GL_INFO = new int[]{
            GL_VENDOR,                  // gpu vendor
            GL_RENDERER,                // gpu renderer
            GL_VERSION,                 // opengl version
            GL_EXTENSIONS,              // opengl extensions
            GL_SHADING_LANGUAGE_VERSION // shader language version
    };

    public static final int[] GL_STRINGS = new int[]{
            R.string.gpu_vendor,        // gpu vendor
            R.string.gpu_renderer,      // gpu renderer
            R.string.gpu_gl_version,    // opengl version
            R.string.gpu_gl_extensions, // opengl extensions
            R.string.gpu_shader_version // shader language version
    };

    public AdvancedDeviceInfoSettings() {
        super(null /* Don't PIN protect the entire screen */);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.device_advanced_info_settings);

        setStringSummary(KEY_KERNEL_VERSION, getFormattedKernelVersion());

        String info = getCPUInfo();
        if (info != null) {
            setStringSummary(KEY_DEVICE_CPU, info);
        } else {
            removePref(KEY_DEVICE_CPU);
        }

        info = getCpuFeatures();
        if (info != null) {
            setStringSummary(KEY_DEVICE_CPU_FEATURES, info);
        } else {
            removePref(KEY_DEVICE_CPU_FEATURES);
        }

        info = getMemInfo();
        if (info != null) {
            setStringSummary(KEY_DEVICE_MEMORY, info);
        } else {
            removePref(KEY_DEVICE_MEMORY);
        }

        PreferenceCategory category = (PreferenceCategory) findPreference(GROUP_DEVICE_GPU);
        final ArrayList<String> glesInformation = getOpenGLESInformation();
        if (glesInformation.size() != 0) {
            String tmp;
            Preference infoPref;
            for (int i = 0; i < glesInformation.size(); i++) {
                tmp = glesInformation.get(i);
                if (!TextUtils.isEmpty(tmp)) {
                    infoPref = new Preference(getActivity());
                    infoPref.setTitle(GL_STRINGS[i]);
                    infoPref.setSummary(tmp);
                    category.addPreference(infoPref);
                }
            }
        }
    }

    private void removePref(final String key) {
        final Preference preference = findPreference(key);
        if (preference != null) {
            getPreferenceScreen().removePreference(preference);
        }
    }

    private void setStringSummary(final String key, final String value) {
        try {
            findPreference(key).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(key).setSummary(getResources().getString(R.string.device_info_default));
        }
    }

    /**
     * Reads a line from the specified file.
     *
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws java.io.IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        final FileReader fr = new FileReader(filename);
        final BufferedReader reader = new BufferedReader(fr, 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
            fr.close();
        }
    }

    public static String getFormattedKernelVersion() {
        try {
            return formatKernelVersion(readLine(FILENAME_PROC_VERSION));
        } catch (IOException e) {
            Log.e(LOG_TAG, "IO Exception when getting kernel version for Device Info screen", e);
            return "Unavailable";
        }
    }

    public static String formatKernelVersion(String rawKernelVersion) {
        // Example (see tests for more):
        // Linux version 3.0.31-g6fb96c9 (android-build@xxx.xxx.xxx.xxx.com) \
        //     (gcc version 4.6.x-xxx 20120106 (prerelease) (GCC) ) #1 SMP PREEMPT \
        //     Thu Jun 28 11:02:39 PDT 2012

        final String PROC_VERSION_REGEX =
                "Linux version (\\S+) " + /* group 1: "3.0.31-g6fb96c9" */
                        "\\((\\S+?)\\) " +        /* group 2: "x@y.com" (kernel builder) */
                        "(?:\\(gcc.+? \\)) " +    /* ignore: GCC version information */
                        "(#\\d+) " +              /* group 3: "#1" */
                        "(?:.*?)?" +              /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
                        "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 4: "Thu Jun 28 11:02:39 PDT 2012" */

        Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(rawKernelVersion);
        if (!m.matches()) {
            Log.e(LOG_TAG, "Regex did not match on /proc/version: " + rawKernelVersion);
            return "Unavailable";
        } else if (m.groupCount() < 4) {
            Log.e(LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount()
                    + " groups");
            return "Unavailable";
        }
        return m.group(1) + "\n" +                     // 3.0.31-g6fb96c9
                m.group(2) + " " + m.group(3) + "\n" + // x@y.com #1
                m.group(4);                            // Thu Jun 28 11:02:39 PDT 2012
    }

    private String getMemInfo() {
        String result = null;
        try {
            /* /proc/meminfo entries follow this format:
             * MemTotal:         362096 kB
             * MemFree:           29144 kB
             * Buffers:            5236 kB
             * Cached:            81652 kB
             */
            String firstLine = readLine(FILENAME_PROC_MEMINFO);
            if (firstLine != null) {
                String parts[] = firstLine.split("\\s+");
                if (parts.length == 3) {
                    result = Long.parseLong(parts[1]) / 1024 + " MB";
                }
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException at getMemInfo()!", e);
        }

        return result;
    }

    private String getCPUInfo() {
        String result = null;

        try {
            /* The expected /proc/cpuinfo output is as follows:
             * Processor    : ARMv7 Processor rev 2 (v7l)
             * BogoMIPS     : 272.62
             */
            String firstLine = readLine(FILENAME_PROC_CPUINFO);
            if (firstLine != null) {
                result = firstLine.split(":")[1].trim();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException at getCPUInfo()!", e);
        }

        return result;
    }

    private String getCpuFeatures() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(FILENAME_PROC_CPUINFO), 256);
            try {
                String tmp;
                while ((tmp = reader.readLine().toLowerCase()) != null) {
                    if (tmp.contains("features")) {
                        return tmp.split(":")[1].trim();
                    }
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException at getCpuFeatures()!", e);
        }
        return null;
    }

    private boolean isOpenGLES20Supported() {
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo info = am.getDeviceConfigurationInfo();
        if (info == null) {
            // we could not get the configuration information, let's return false
            return false;
        }
        final int glEsVersion = ((info.reqGlEsVersion & 0xffff0000) >> 16);
        return (glEsVersion >= 2);
    }

    private ArrayList<String> getOpenGLESInformation() {
        final ArrayList<String> glesInformation = new ArrayList<String>(GL_INFO.length);

        if (!isOpenGLES20Supported()) {
            // OpenGL ES 2.0 not supported, return empty list
            return glesInformation;
        }

        // get a hold of the display and initialize
        final EGLDisplay dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        final int[] vers = new int[2];
        EGL14.eglInitialize(dpy, vers, 0, vers, 1);

        // find a suitable opengl config. since we do not render, we are not that strict
        // about the exact attributes
        final int[] configAttr = {
                EGL14.EGL_COLOR_BUFFER_TYPE, EGL14.EGL_RGB_BUFFER,
                EGL14.EGL_LEVEL, 0,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
        };
        final EGLConfig[] configs = new EGLConfig[1];
        final int[] numConfig = new int[1];
        EGL14.eglChooseConfig(dpy, configAttr, 0, configs, 0, 1, numConfig, 0);
        if (numConfig[0] == 0) {
            Log.w(LOG_TAG, "no config found! PANIC!");
        }
        final EGLConfig config = configs[0];

        // we need a surface for our context, even if we do not render anything
        // so let's create a little offset surface
        final int[] surfAttr = {
                EGL14.EGL_WIDTH, 64,
                EGL14.EGL_HEIGHT, 64,
                EGL14.EGL_NONE
        };
        final EGLSurface surf = EGL14.eglCreatePbufferSurface(dpy, config, surfAttr, 0);

        // finally let's create our context
        final int[] ctxAttrib = { EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE };
        final EGLContext ctx =
                EGL14.eglCreateContext(dpy, config, EGL14.EGL_NO_CONTEXT, ctxAttrib, 0);

        // set up everything, make the context our current context
        EGL14.eglMakeCurrent(dpy, surf, surf, ctx);

        // get the informations we desire
        for (final int glInfo : GL_INFO) {
            glesInformation.add(glGetString(glInfo));
        }

        // free and destroy everything
        EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroySurface(dpy, surf);
        EGL14.eglDestroyContext(dpy, ctx);
        EGL14.eglTerminate(dpy);

        return glesInformation;
    }

}
