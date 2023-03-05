/*
 * Copyright 2015 The Android Open Source Project
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

import java.io.IOException

/**
 * Base class for any audio input or output.
 */
abstract class AudioStreamBase {

    private var mRequestedStreamConfiguration: StreamConfiguration? = null
    private var mActualStreamConfiguration: StreamConfiguration? = null
    var mLatencyStatistics: AudioStreamBase.DoubleStatistics? = null

    private var mBufferSizeInFrames = 0

    open fun getStreamStatus(): StreamStatus? {
        val status = StreamStatus()
        status.bufferSize = getBufferSizeInFrames()
        status.xRunCount = getXRunCount()
        status.framesRead = getFramesRead()
        status.framesWritten = getFramesWritten()
        status.callbackCount = getCallbackCount()
        status.latency = getLatency()
        mLatencyStatistics!!.add(status.latency)
        status.callbackTimeStr = getCallbackTimeStr()
        status.cpuLoad = getCpuLoad()
        status.state = getState()
        return status
    }

    open fun getLatencyStatistics(): AudioStreamBase.DoubleStatistics? {
        return mLatencyStatistics
    }

    class DoubleStatistics {
        private var sum = 0.0
        private var count = 0
        private var minimum = Double.MAX_VALUE
        private var maximum = Double.MIN_VALUE
        fun add(statistic: Double) {
            if (statistic <= 0.0) return
            sum += statistic
            count++
            minimum = Math.min(statistic, minimum)
            maximum = Math.max(statistic, maximum)
        }

        val average: Double
            get() = sum / count

        fun dump(): String {
            return if (count == 0) "?" else String.format(
                "%3.1f/%3.1f/%3.1f ms", minimum,
                average, maximum
            )
        }
    }

    /**
     * Changes dynamic at run-time.
     */
    class StreamStatus {
        var bufferSize = 0
        var xRunCount = 0
        var framesWritten: Long = 0
        var framesRead: Long = 0
        var latency // msec
                = 0.0
        var state = 0
        var callbackCount: Long = 0
        var framesPerCallback = 0
        var cpuLoad = 0.0
        var callbackTimeStr: String? = null

        //fun getFramesPerCallback() : Int { return framesPerCallback}
        //fun setFramesPerCallback(frames: Int) { framesPerCallback = frames}


        // These are constantly changing.
        fun dump(framesPerBurst: Int): String {
            if (bufferSize < 0 || framesWritten < 0) {
                return "idle"
            }
            val buffer = StringBuffer()
            buffer.append("time between callbacks = $callbackTimeStr\n")
            buffer.append(
                """written ${
                    String.format(
                        "0x%08X",
                        framesWritten
                    )
                } - read ${
                    String.format(
                        "0x%08X",
                        framesRead
                    )
                } = ${framesWritten - framesRead} frames
"""
            )
            val cpuLoadText = String.format("%2d%c", (cpuLoad * 100).toInt(), '%')
            buffer.append(
                """${convertStateToString(state)}, #cb=$callbackCount, f/cb=${
                    String.format(
                        "%3d",
                        framesPerCallback
                    )
                }, $cpuLoadText cpu
"""
            )
            buffer.append("buffer size = ")
            if (bufferSize < 0) {
                buffer.append("?")
            } else {
                val numBuffers = bufferSize / framesPerBurst
                val remainder = bufferSize - numBuffers * framesPerBurst
                buffer.append("$bufferSize = ($numBuffers * $framesPerBurst) + $remainder")
            }
            buffer.append(",   xRun# = " + if (xRunCount < 0) "?" else xRunCount)
            return buffer.toString()
        }

        /**
         * Converts ints from Oboe index to human-readable stream state
         */
        private fun convertStateToString(stateId: Int): String {
            val STATE_ARRAY = arrayOf(
                "Uninit.", "Unknown", "Open", "Starting", "Started",
                "Pausing", "Paused", "Flushing", "Flushed",
                "Stopping", "Stopped", "Closing", "Closed", "Disconn."
            )
            return if (stateId < 0 || stateId >= STATE_ARRAY.size) {
                "Invalid - $stateId"
            } else STATE_ARRAY[stateId]
        }
    }

    /**
     *
     * @param requestedConfiguration
     * @param actualConfiguration
     * @param bufferSizeInFrames
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun open(
        requestedConfiguration: StreamConfiguration,
        actualConfiguration: StreamConfiguration,
        bufferSizeInFrames: Int
    ) {
        mRequestedStreamConfiguration = requestedConfiguration
        mActualStreamConfiguration = actualConfiguration
        mBufferSizeInFrames = bufferSizeInFrames
        mLatencyStatistics = DoubleStatistics()
    }

    abstract fun isInput(): Boolean

    @Throws(IOException::class)
    open fun startPlayback() {
    }

    @Throws(IOException::class)
    open fun stopPlayback() {
    }

    abstract fun write(buffer: FloatArray?, offset: Int, length: Int): Int

    abstract fun close()


    open fun getChannelCount(): Int {
        return mActualStreamConfiguration!!.channelCount
    }

    open fun getSampleRate(): Int {
        return mActualStreamConfiguration!!.sampleRate
    }

    open fun getFramesPerBurst(): Int {
        return mActualStreamConfiguration!!.framesPerBurst
    }

    open fun getBufferCapacityInFrames(): Int {
        return mBufferSizeInFrames
    }

    open fun getBufferSizeInFrames(): Int {
        return mBufferSizeInFrames
    }

    open fun setBufferSizeInFrames(bufferSize: Int): Int {
        throw UnsupportedOperationException("bufferSize cannot be changed")
    }

    open fun getCallbackCount(): Long {
        return -1
    }

    open fun getLastErrorCallbackResult(): Int {
        return 0
    }

    open fun getFramesWritten(): Long {
        return -1
    }

    open fun getFramesRead(): Long {
        return -1
    }

    open fun getLatency(): Double {
        return -1.0
    }

    open fun getCpuLoad(): Double {
        return 0.0
    }

    open fun getCallbackTimeStr(): String? {
        return "?"
    }

    open fun getState(): Int {
        return -1
    }

    open fun isThresholdSupported(): Boolean {
        return false
    }

    open fun setWorkload(workload: Double) {}

    abstract fun getXRunCount(): Int

}