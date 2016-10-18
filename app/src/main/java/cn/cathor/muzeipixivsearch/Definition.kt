package cn.cathor.muzeipixivsearch

import android.util.Log

/**
 * Created by Cathor on 2016/10/14.
 */

class Definition{
    companion object{
        val DEBUG_LOG = true
    }
}

inline fun <reified T> T.debug(word: Any?): Unit{
    Log.d(T::class.qualifiedName, "debug " + word)
}