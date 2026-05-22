package com.philia093.chat.ui

import android.content.Context
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class PopularFont(
    val name: String,
    val fileName: String,
    val url: String,
    val preview: String = "昔涟 · 心之涟漪"
)

// Curated popular Chinese fonts available via CDN
val POPULAR_FONTS = listOf(
    PopularFont("默认系统字体", "", ""),
    PopularFont(
        "霞鹜文楷",
        "LXGWWenKai-Regular.ttf",
        "https://github.com/lxgw/LxgwWenKai/releases/download/v1.330/LXGWWenKai-Regular.ttf"
    ),
    PopularFont(
        "思源黑体",
        "NotoSansSC-Regular.ttf",
        "https://github.com/googlefonts/noto-cjk/releases/download/Sans2.004/03_NotoSansCJKsc.zip"
    ),
    PopularFont(
        "思源宋体",
        "NotoSerifSC-Regular.otf",
        "https://github.com/googlefonts/noto-cjk/releases/download/Serif2.002/09_NotoSerifCJKsc.zip"
    ),
    PopularFont(
        "站酷快乐体",
        "ZCOOLQingKeHuangYou.ttf",
        "https://github.com/googlefonts/zcool-qingke-huangyou/raw/main/fonts/ttf/ZCOOLQingKeHuangYou-Regular.ttf"
    ),
    PopularFont(
        "马善政楷书",
        "MaShanZheng-Regular.ttf",
        "https://github.com/googlefonts/mashanzheng/raw/main/fonts/ttf/MaShanZheng-Regular.ttf"
    ),
)

object FontManager {

    private val cacheDir get() = File("/data/data/com.philia093.chat/files/fonts")

    fun getCachedTypeface(context: Context, font: PopularFont): Typeface? {
        if (font.fileName.isEmpty()) return Typeface.DEFAULT
        val file = File(context.filesDir, "fonts/${font.fileName}")
        return if (file.exists()) Typeface.createFromFile(file) else null
    }

    suspend fun downloadFont(font: PopularFont, onProgress: (Float) -> Unit = {}): File? {
        if (font.url.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            try {
                val dir = File("/data/data/com.philia093.chat/files/fonts")
                dir.mkdirs()
                val outFile = File(dir, font.fileName)
                if (outFile.exists()) return@withContext outFile

                val url = URL(font.url)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 60000
                conn.connect()

                val total = conn.contentLength.toFloat()
                var loaded = 0L
                conn.inputStream.use { input ->
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            loaded += bytes
                            if (total > 0) onProgress(loaded / total)
                        }
                    }
                }
                outFile
            } catch (e: Exception) {
                null
            }
        }
    }

    fun isDownloaded(font: PopularFont): Boolean {
        if (font.fileName.isEmpty()) return true
        return File("/data/data/com.philia093.chat/files/fonts/${font.fileName}").exists()
    }
}
