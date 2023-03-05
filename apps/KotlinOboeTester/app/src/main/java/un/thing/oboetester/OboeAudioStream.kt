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

import java.io.IOException

/**
 * Implementation of an AudioStreamBase using Oboe.
 */
abstract class OboeAudioStream : AudioStreamBase() {
    var streamIndex = INVALID_STREAM_INDEX

    @Throws(IOException::class)
    override fun stopPlayback() {
        val result = stopPlaybackNative()
        if (result != 0) {
            throw IOException("Stop Playback failed! result = $result")
        }
    }

    external fun stopPlaybackNative(): Int

    @Throws(IOException::class)
    override fun startPlayback() {
        val result = startPlaybackNative()
        if (result != 0) {
            throw IOException("Start Playback failed! result = $result")
        }
    }

    external fun startPlaybackNative(): Int

    // Write disabled because the synth is in native code.
    override fun write(buffer: FloatArray?, offset: Int, length: Int): Int {
        return 0
    }

    @Throws(IOException::class)
    override fun open(
        requestedConfiguration: StreamConfiguration,
        actualConfiguration: StreamConfiguration, bufferSizeInFrames: Int
    ) {
        super.open(requestedConfiguration, actualConfiguration, bufferSizeInFrames)
        val result = openNative(
            requestedConfiguration!!.nativeApi,
            requestedConfiguration.sampleRate,
            requestedConfiguration.channelCount,
            requestedConfiguration.channelMask,
            requestedConfiguration.format,
            requestedConfiguration.sharingMode,
            requestedConfiguration.performanceMode,
            requestedConfiguration.inputPreset,
            requestedConfiguration.usage,
            requestedConfiguration.contentType,
            requestedConfiguration.deviceId,
            requestedConfiguration.sessionId,
            requestedConfiguration.channelConversionAllowed,
            requestedConfiguration.formatConversionAllowed,
            requestedConfiguration.rateConversionQuality,
            requestedConfiguration.isMMap,
            isInput()
        )
        if (result < 0) {
            streamIndex = INVALID_STREAM_INDEX
            throw IOException("Open failed! result = $result")
        } else {
            streamIndex = result
        }
        actualConfiguration!!.nativeApi = nativeApi
        actualConfiguration.sampleRate = getSampleRate()//sampleRate
        actualConfiguration.sharingMode = sharingMode
        actualConfiguration.performanceMode = performanceMode
        actualConfiguration.inputPreset = inputPreset
        actualConfiguration.usage = usage
        actualConfiguration.contentType = contentType
        actualConfiguration.framesPerBurst = getFramesPerBurst()//framesPerBurst
        actualConfiguration.bufferCapacityInFrames = getBufferCapacityInFrames()  //bufferCapacityInFrames
        actualConfiguration.channelCount = getChannelCount()//channelCount
        actualConfiguration.channelMask = getChannelMask()//channelMask
        actualConfiguration.deviceId = getDeviceId()//deviceId
        actualConfiguration.sessionId = sessionId
        actualConfiguration.format = format
        actualConfiguration.isMMap = isMMap
        actualConfiguration.direction =
            if (isInput()) StreamConfiguration.DIRECTION_INPUT else StreamConfiguration.DIRECTION_OUTPUT
    }

    private external fun openNative(
        nativeApi: Int,
        sampleRate: Int,
        channelCount: Int,
        channelMask: Int,
        format: Int,
        sharingMode: Int,
        performanceMode: Int,
        inputPreset: Int,
        usage: Int,
        contentType: Int,
        deviceId: Int,
        sessionId: Int,
        channelConversionAllowed: Boolean,
        formatConversionAllowed: Boolean,
        rateConversionQuality: Int,
        isMMap: Boolean,
        isInput: Boolean
    ): Int

    override fun close() {
        if (streamIndex >= 0) {
            close(streamIndex)
            streamIndex = INVALID_STREAM_INDEX
        }
    }

    external fun close(streamIndex: Int)

    //@Override
    //val bufferCapacityInFrames: Int
        //get() = getBufferCapacityInFrames(streamIndex)

    private external fun getBufferCapacityInFrames(streamIndex: Int): Int
/*
    override fun setBufferSizeInFrames(bufferSizeInFrames: Int): Int {
        super.bufferSizeInFrames = bufferSizeInFrames
        return super.bufferSizeInFrames
    }*/
    /*
    override var bufferSizeInFrames: Int
        get() = getBufferSizeInFrames(streamIndex)
        //set (bufferSizeInFrames) {
        //    field = bufferSizeInFrames
        //}
*/
    private external fun getBufferSizeInFrames(streamIndex: Int): Int

    override fun isThresholdSupported() : Boolean { return true }
/*
    fun setBufferSizeInFrames(thresholdFrames: Int): Int {
        return setBufferSizeInFrames(streamIndex, thresholdFrames)
    }
*/
    override fun setBufferSizeInFrames(bufferSize: Int): Int {
        return setBufferSizeInFrames(streamIndex, bufferSize)
    }

    private external fun setBufferSizeInFrames(streamIndex: Int, thresholdFrames: Int): Int
    val nativeApi: Int
        get() = getNativeApi(streamIndex)

    private external fun getNativeApi(streamIndex: Int): Int
    override fun getFramesPerBurst(): Int {
        return getFramesPerBurst(streamIndex)
    }
    /*
    override val framesPerBurst: Int
        get() = getFramesPerBurst(streamIndex)
*/
    private external fun getFramesPerBurst(streamIndex: Int): Int
    val sharingMode: Int
        get() = getSharingMode(streamIndex)

    private external fun getSharingMode(streamIndex: Int): Int
    val performanceMode: Int
        get() = getPerformanceMode(streamIndex)

    private external fun getPerformanceMode(streamIndex: Int): Int
    val inputPreset: Int
        get() = getInputPreset(streamIndex)

    private external fun getInputPreset(streamIndex: Int): Int
    override fun getSampleRate(): Int {
        return getSampleRate(streamIndex)
    }
    /*
    override val sampleRate: Int
        get() = getSampleRate(streamIndex)
*/
    private external fun getSampleRate(streamIndex: Int): Int
    val format: Int
        get() = getFormat(streamIndex)

    private external fun getFormat(streamIndex: Int): Int
    val usage: Int
        get() = getUsage(streamIndex)

    private external fun getUsage(streamIndex: Int): Int
    val contentType: Int
        get() = getContentType(streamIndex)

    private external fun getContentType(streamIndex: Int): Int
    override fun getChannelCount(): Int {
        return getChannelCount(streamIndex)
    }

//    override val channelCount: Int
 //       get() = getChannelCount(streamIndex)

    private external fun getChannelCount(streamIndex: Int): Int

    fun getChannelMask(): Int {
        return getChannelMask(streamIndex)
    }
   // val channelMask: Int
     //   get() = getChannelMask(streamIndex)

    private external fun getChannelMask(streamIndex: Int): Int

    fun getDeviceId() : Int {
        return getDeviceId(streamIndex)
    }
    private external fun getDeviceId(streamIndex: Int): Int

    val sessionId: Int
        get() = getSessionId(streamIndex)

    private external fun getSessionId(streamIndex: Int): Int

    val isMMap: Boolean
        get() = isMMap(streamIndex)

    private external fun isMMap(streamIndex: Int): Boolean


    external override fun getCallbackCount(): Long // TODO Move to another class?

    override fun getLastErrorCallbackResult(): Int {
        return getLastErrorCallbackResult(streamIndex)
    }
    //val lastErrorCallbackResult: Int
     //   get() = getLastErrorCallbackResult(streamIndex)
    private external fun getLastErrorCallbackResult(streamIndex: Int): Int

    override fun getFramesWritten(): Long {
        return getFramesWritten(streamIndex)
    }

    private external fun getFramesWritten(streamIndex: Int): Long

    override fun getFramesRead(): Long {
        return getFramesRead(streamIndex)
    }

    private external fun getFramesRead(streamIndex: Int): Long



    override fun getXRunCount(): Int { return getXRunCount(streamIndex) }
    private external fun getXRunCount(streamIndex: Int): Int

    //val latency: Double
      //  get() = getTimestampLatency(streamIndex)

    override fun getLatency(): Double {
        return getTimestampLatency(streamIndex)
    }

    private external fun getTimestampLatency(streamIndex: Int): Double
   // val cpuLoad: Double
     //   get() = getCpuLoad(streamIndex)

    override fun getCpuLoad(): Double {
        return getCpuLoad(streamIndex)
    }

    private external fun getCpuLoad(streamIndex: Int): Double
    //override
    //val callbackTimeStr: String
      //  get() = callbackTimeString

    //val callbackTimeString: String
      //  external get

    override fun getCallbackTimeStr(): String? {
        return getCallbackTimeString()
    }
    open external fun getCallbackTimeString(): String?

    external override fun setWorkload(workload: Double)
    //val state: Int
      //  get() = getState(streamIndex)

    override fun getState(): Int {
        return getState(streamIndex)
    }

    private external fun getState(streamIndex: Int): Int

    companion object {
        private const val INVALID_STREAM_INDEX = -1
        @JvmStatic
        external fun setCallbackReturnStop(b: Boolean)
        @JvmStatic
        external fun setUseCallback(checked: Boolean)
        @JvmStatic
        external fun setCallbackSize(callbackSize: Int)
        @JvmStatic
        external fun getOboeVersionNumber() : Int
        //val oboeVersionNumber: Int
         //   external get
    }
}