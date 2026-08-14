package com.transbuddy.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * ImageLoader — High Performance Image Loader (MVC Utility)
 * Optimized for ultra-fast, smooth scrolling:
 *  - Two-level caching (Memory LRU + Disk Cache)
 *  - Automatic bitmap downsampling to 128x128px (reduces RAM & CPU decoding overhead by 95%+)
 *  - 6-worker thread pool for concurrent asynchronous fetches
 */
object ImageLoader {
    private val memoryCache: LruCache<String, Bitmap>
    private val executor = Executors.newFixedThreadPool(6)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val sslContext: SSLContext by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        })
        val sc = SSLContext.getInstance("TLS")
        sc.init(null, trustAllCerts, java.security.SecureRandom())
        sc
    }

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 6 // Expand LRU cache memory budget
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    fun loadImage(urlStr: String?, imageView: ImageView, placeholderResId: Int) {
        if (urlStr.isNullOrEmpty()) {
            imageView.setImageResource(placeholderResId)
            imageView.imageTintList = null
            imageView.colorFilter = null
            imageView.tag = null
            return
        }

        imageView.tag = urlStr

        // 1. Check Memory Cache (0ms Instant Return)
        val cachedBitmap = memoryCache.get(urlStr)
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap)
            imageView.imageTintList = null
            imageView.colorFilter = null
            return
        }

        // Show Placeholder while loading asynchronously
        imageView.setImageResource(placeholderResId)

        val context = imageView.context.applicationContext

        // 2. Fetch asynchronously from Disk Cache or Network
        executor.execute {
            try {
                val diskFile = getDiskCacheFile(context, urlStr)

                // Check Disk Cache first
                if (diskFile.exists() && diskFile.length() > 0) {
                    val bitmap = decodeSampledBitmapFromFile(diskFile.absolutePath, 128, 128)
                    if (bitmap != null) {
                        memoryCache.put(urlStr, bitmap)
                        mainHandler.post {
                            if (imageView.tag == urlStr) {
                                imageView.setImageBitmap(bitmap)
                                imageView.imageTintList = null
                                imageView.colorFilter = null
                            }
                        }
                        return@execute
                    }
                }

                // 3. Network Fetch
                val url = URL(urlStr)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.doInput = true
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
                connection.setRequestProperty("Accept", "image/*,*/*")

                if (connection is HttpsURLConnection) {
                    connection.sslSocketFactory = sslContext.socketFactory
                    connection.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
                }

                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val bytes = inputStream.readBytes()
                    inputStream.close()

                    if (bytes.isNotEmpty()) {
                        // Downsample Bitmap to 128x128 max to save 95%+ RAM
                        val bitmap = decodeSampledBitmapFromByteArray(bytes, 128, 128)
                        if (bitmap != null) {
                            memoryCache.put(urlStr, bitmap)

                            // Save downsampled bitmap to disk cache asynchronously
                            try {
                                val fos = FileOutputStream(diskFile)
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                                fos.flush()
                                fos.close()
                            } catch (ignored: Exception) {}

                            mainHandler.post {
                                if (imageView.tag == urlStr) {
                                    imageView.setImageBitmap(bitmap)
                                    imageView.imageTintList = null
                                    imageView.colorFilter = null
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep placeholder on load failure
            }
        }
    }

    private fun getDiskCacheFile(context: Context, urlStr: String): File {
        val cacheDir = File(context.cacheDir, "img_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val md5Key = md5(urlStr)
        return File(cacheDir, "$md5Key.jpg")
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun decodeSampledBitmapFromByteArray(data: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(data, 0, data.size, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565 // Lowers memory footprint by 50%
        return BitmapFactory.decodeByteArray(data, 0, data.size, options)
    }

    private fun decodeSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
