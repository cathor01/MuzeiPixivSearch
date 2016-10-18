package cn.cathor.muzeipixivsearch.source

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.preference.PreferenceManager
import cn.cathor.muzeipixivsearch.Definition
import cn.cathor.muzeipixivsearch.R
import cn.cathor.muzeipixivsearch.controller.RequestController
import cn.cathor.muzeipixivsearch.debug
import cn.cathor.muzeipixivsearch.items.PixivItem
import com.google.android.apps.muzei.api.Artwork
import com.google.android.apps.muzei.api.MuzeiArtSource
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource
import com.orm.SugarRecord
import java.io.File
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Created by Cathor on 2016/10/14.
 */

class MuzeiItemSource : RemoteMuzeiArtSource("pixiv search") {

    private val MINUTE = 60 * 1000  // a minute in milliseconds

    override fun onCreate() {
        super.onCreate()
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK)
    }

    private fun isOnlyUpdateOnWifi(): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val defaultValue = false
        val v = preferences.getBoolean("pixiv_change_wifi", defaultValue)
        if (Definition.DEBUG_LOG) debug("pixiv_change_wifi = " + v)
        return v
    }

    private fun isEnabledWifi(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return wifi.isConnected
    }

    private fun getChangeInterval(): Int {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val defaultValue = getString(R.string.pref_default_change_frequency)
        val s = preferences.getString("pixiv_change_frequency", defaultValue)
        if (Definition.DEBUG_LOG) debug("pixiv_change_frequency = \"" + s + "\"")
        try {
            return Integer.parseInt(s)
        } catch (e: NumberFormatException) {
            if (Definition.DEBUG_LOG) debug(e.toString())
            return 0
        }

    }

    private fun getIfBig(): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val s = preferences.getBoolean("pixiv_change_big", false)
        if (Definition.DEBUG_LOG) debug("pixiv_change_big = \"" + s + "\"")
        return s
    }

    private fun scheduleUpdate() {
        val changeInterval = getChangeInterval()

        if (changeInterval > 0) {
            scheduleUpdate(System.currentTimeMillis() + changeInterval * MINUTE)
        }
    }

    override fun onTryUpdate(reason: Int) {
        if (Definition.DEBUG_LOG) debug("call try for $reason")
        val token = currentArtwork?.token
        if (isOnlyUpdateOnWifi() && !isEnabledWifi()) {
            if (Definition.DEBUG_LOG) debug("no wifi")
            scheduleUpdate()
            return
        }
        val items = SugarRecord.listAll(PixivItem::class.java, "id")
        if (Definition.DEBUG_LOG) debug("size is ${items.size}")
        var flag = 0
        when(reason){
            MuzeiArtSource.UPDATE_REASON_INITIAL ->
                flag = 0
            MuzeiArtSource.UPDATE_REASON_OTHER ->
                if(token != null) {
                    for (i in 0 until items.size) {
                        if (items[i].id == token.toLong()) {
                            flag = i
                        }
                    }
                }
            else ->
                if(token != null) {
                    for (i in 0 until items.size) {
                        if (items[i].id == token.toLong()) {
                            flag = (i + 1).mod(items.size)
                        }
                    }
                }
        }
        val item = items[flag]
        val uri = findOrDownload(item)
        if (Definition.DEBUG_LOG) debug(uri)
        val art = Artwork.Builder()
                .byline(item.description)
                .title(item.title)
                .token(item.id.toString())
                .imageUri(uri)
                .viewIntent(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                .build()
        publishArtwork(art)
        scheduleUpdate()
    }


    fun findOrDownload(item: PixivItem) : Uri {
        var url: String
        if (getIfBig()) {
            url = item.imgUrl!!
        }
        else{
            url = item.smallImgUrl!!
        }
        if (Definition.DEBUG_LOG) debug(item)
        val file = File(externalCacheDir, getMD5(url))
        if (file.exists()){
            return Uri.parse("file://${file.absolutePath}")
        }
        var output = FileOutputStream(file)

        var input = RequestController.Builder().build().client.GET_stream(url)
        try {
            val buffer = ByteArray(1024 * 5)
            var read = input.read(buffer)
            while (read > 0) {
                output.write(buffer, 0, read)
                read = input.read(buffer)
            }
            input.close()
            output.close()
        }
        catch (e: Exception){
            input.close()
            output.close()
            file.delete()
            output = FileOutputStream(file)

            input = RequestController.Builder().build().client.GET_stream(item.smallImgUrl!!)
            try {
                val buffer = ByteArray(1024 * 5)
                var read = input.read(buffer)
                while (read > 0) {
                    output.write(buffer, 0, read)
                    read = input.read(buffer)
                }
            }
            catch (e: Exception){
                if (Definition.DEBUG_LOG) debug(e)
                throw RetryException(e)
            }
            finally {
                input.close()
                output.close()
                file.delete()
            }
        }
        return Uri.parse("file://${file.absolutePath}")
    }

    fun getMD5(info: String): String {
        try {
            val md5 = MessageDigest.getInstance("MD5")
            md5.update(info.toByteArray(charset("UTF-8")))
            val encryption = md5.digest()

            val strBuf = StringBuffer()
            for (i in encryption.indices) {
                if (Integer.toHexString(0xff and encryption[i].toInt()).length == 1) {
                    strBuf.append("0").append(Integer.toHexString(0xff and encryption[i].toInt()))
                } else {
                    strBuf.append(Integer.toHexString(0xff and encryption[i].toInt()))
                }
            }
            return strBuf.toString()
        } catch (e: NoSuchAlgorithmException) {
            return ""
        } catch (e: UnsupportedEncodingException) {
            return ""
        }

    }

}