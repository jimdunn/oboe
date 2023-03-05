/*
 * Copyright 2017 The Android Open Source Project
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
package un.thing.oboetester

import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object AudioQueryTools {
    private val GETPROP_EXECUTABLE_PATH = "/system/bin/getprop"
    fun getSystemProperty(propName: String?): String {
        var process: Process? = null
        var bufferedReader: BufferedReader? = null
        try {
            process = ProcessBuilder().command(GETPROP_EXECUTABLE_PATH, propName)
                .redirectErrorStream(true).start()
            bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            var line = bufferedReader.readLine()
            if (line == null) {
                line = "" //prop not set
            }
            return line
        } catch (e: Exception) {
            return ""
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close()
                } catch (e: IOException) {
                }
            }
            process?.destroy()
        }
    }

    fun getAudioFeatureReport(packageManager: PackageManager): String {
        val report = StringBuffer()
        report.append(
            "\nProAudio Feature     : "
                    + packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO)
        )
        report.append(
            ("\nLowLatency Feature   : "
                    + packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY))
        )
        report.append(
            ("\nMIDI Feature         : "
                    + packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI))
        )
        report.append(
            ("\nUSB Host Feature     : "
                    + packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST))
        )
        report.append(
            ("\nUSB Accessory Feature: "
                    + packageManager.hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY))
        )
        return report.toString()
    }

    fun getAudioManagerReport(audioManager: AudioManager): String {
        val report = StringBuffer()
        val unprocessedSupport =
            audioManager.getParameters(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)
        report.append("\nSUPPORT_UNPROCESSED  : " + (if ((unprocessedSupport == null)) "null" else "yes"))
        return report.toString()
    }

    private fun formatKeyValueLine(key: String, value: String): String {
        val numSpaces = Math.max(1, 21 - key.length)
        val spaces = String.format("%0" + numSpaces + "d", 0).replace("0", " ")
        return "\n$key$spaces: $value"
    }

    private fun getSystemPropertyLine(key: String): String {
        return formatKeyValueLine(key, getSystemProperty(key))
    }

    fun convertSdkToShortName(sdk: Int): String {
        if (sdk < 16) return "early"
        if (sdk > 34) return "future"
        val names = arrayOf(
            "J",  // 16
            "J+",
            "J++",
            "K",
            "K+",
            "L",  // 21
            "L+",
            "M",
            "N",  // 24
            "N_MR1",
            "O",
            "O_MR1",
            "P",  // 28
            "Q",
            "R",
            "S",
            "S_V2",
            "T",  // 33
            "U"
        )
        return names[sdk - 16]
    }

    val mediaPerformanceClass: String
        get() {
            val mpc = Build.VERSION.MEDIA_PERFORMANCE_CLASS
            val text = if ((mpc == 0)) "not declared" else convertSdkToShortName(mpc)
            return formatKeyValueLine(
                "Media Perf Class",
                "$mpc ($text)"
            )
        }
    val audioPropertyReport: String
        get() {
            val report = StringBuffer()
            report.append(getSystemPropertyLine("aaudio.mmap_policy"))
            report.append(getSystemPropertyLine("aaudio.mmap_exclusive_policy"))
            report.append(getSystemPropertyLine("aaudio.mixer_bursts"))
            report.append(getSystemPropertyLine("aaudio.wakeup_delay_usec"))
            report.append(getSystemPropertyLine("aaudio.minimum_sleep_usec"))
            report.append(getSystemPropertyLine("aaudio.hw_burst_min_usec"))
            report.append(getSystemPropertyLine("aaudio.in_mmap_offset_usec"))
            report.append(getSystemPropertyLine("aaudio.out_mmap_offset_usec"))
            report.append(getSystemPropertyLine("ro.product.manufacturer"))
            report.append(getSystemPropertyLine("ro.product.brand"))
            report.append(getSystemPropertyLine("ro.product.model"))
            report.append(getSystemPropertyLine("ro.product.name"))
            report.append(getSystemPropertyLine("ro.product.device"))
            report.append(getSystemPropertyLine("ro.product.cpu.abi"))
            report.append(getSystemPropertyLine("ro.soc.manufacturer"))
            report.append(getSystemPropertyLine("ro.soc.model"))
            report.append(getSystemPropertyLine("ro.arch"))
            report.append(getSystemPropertyLine("ro.hardware"))
            report.append(getSystemPropertyLine("ro.hardware.chipname"))
            report.append(getSystemPropertyLine("ro.board.platform"))
            report.append(getSystemPropertyLine("ro.build.changelist"))
            report.append(getSystemPropertyLine("ro.build.description"))
            return report.toString()
        }
}