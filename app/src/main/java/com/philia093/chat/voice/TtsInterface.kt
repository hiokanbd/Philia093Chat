package com.philia093.chat.voice

/**
 * TTS interface placeholder.
 * Implement this to add text-to-speech with specific voice models.
 *
 * Usage:
 *   val tts = YourTtsImpl(apiKey, voiceId)
 *   tts.speak("昔涟的回复文本")
 */
interface TtsInterface {
    /** List available voice models */
    suspend fun listVoices(): List<VoiceModel>

    /** Convert text to speech, returns audio file path */
    suspend fun synthesize(text: String, voiceId: String): String?

    /** Stream audio as it's generated */
    suspend fun synthesizeStream(text: String, voiceId: String, onChunk: (ByteArray) -> Unit)

    fun stop()
    fun release()
}

data class VoiceModel(
    val id: String,
    val name: String,
    val language: String,
    val gender: String = "female",
    val sampleUrl: String? = null
)

/**
 * No-op implementation for when TTS is not configured.
 */
class NoOpTts : TtsInterface {
    override suspend fun listVoices() = emptyList<VoiceModel>()
    override suspend fun synthesize(text: String, voiceId: String) = null
    override suspend fun synthesizeStream(text: String, voiceId: String, onChunk: (ByteArray) -> Unit) {}
    override fun stop() {}
    override fun release() {}
}
