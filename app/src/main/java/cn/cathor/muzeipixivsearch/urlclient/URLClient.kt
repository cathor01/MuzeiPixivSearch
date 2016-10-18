package cn.cathor.muzeipixivsearch.urlclient

import android.util.Log
import cn.cathor.muzeipixivsearch.Definition
import cn.cathor.muzeipixivsearch.debug
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import java.util.regex.Pattern
import javax.net.ssl.*

/**
 * Created by Cathor on 2016/4/26 8:58.
 */

class URLClient(val headers: MutableMap<String, String>, val manager: MutableMap<String, MutableMap<String, String>>){
    companion object{
        private val COOKIE_KEY = "Set-Cookie"
    }

    fun putHeaders(k: String, v: String){
        if (k !in headers.keys) {
            headers.put(k, v)
        }
        else{
            headers[k] = v
        }
    }

    val DO_NOT_VERIFY: HostnameVerifier = HostnameVerifier { hostname, session -> true }


    fun getCookiesJar(): MutableMap<String, MutableMap<String, String>>{
        return manager
    }


    fun getCookies(url: URL): String?{
        var temp = url.host.split(".")
        val hostname = temp[temp.size - 2] + "." + temp[temp.size - 1]
        var key = hostname + "_" + if(url.port != -1) url.port else 80
        if(manager.contains(key)){
            if (Definition.DEBUG_LOG) debug("$url : ${manager[key]}")
            val cookies_m = manager[key]!!
            return cookies_m.map { "${it.key}=${it.value}" }.joinToString("; ")
        }
        return ""
    }

    fun setCookies(url: URL, cookies: String){
        var temp = url.host.split(".")
        val hostname = temp[temp.size - 2] + "." + temp[temp.size - 1]
        var key = hostname + "_" + if(url.port != -1) url.port else 80
        if (key !in manager.keys){
            manager.put(key, mutableMapOf<String, String>())
        }
        val cookie_m = manager[key]!!
        val cookies_kv = cookies.replace(" ", "").split(";").map { it.split("=") }
        for (item in cookies_kv){
            if (item.size == 2) {
                if (item[0] in cookie_m.keys) {
                    cookie_m[item[0]] = item[1]
                } else {
                    cookie_m.put(item[0], item[1])
                }
            }
        }
    }


    class BadResponse: Exception{
        val code: Int
        constructor(code: Int): super("rsponse_code:" + code){
            this.code = code
        }
    }

    /***
     * POST 请求，自动记录对应host，port的cookie，
     * @param url 可为String类型或是URL类型
     * @param data 可为String（标准的Query，形如name=cathor&password=123456）或是Map{'name':'cathor', 'password':'123456'}
     * */

    fun POST(url: URL, data: String): String{
        return POST(url, data.toByteArray())
    }

    fun POST(url : URL, data: ByteArray?, property: Map<String, String>? = null, process_out: ((OutputStream)-> Unit)? = null): String{
        var connection = url.openConnection() as HttpURLConnection
        if (url.protocol.toLowerCase() == "https") {
            trustAllHosts()
            connection = connection as HttpsURLConnection
            connection.hostnameVerifier = DO_NOT_VERIFY
        }
        connection.requestMethod = "POST"
        connection.useCaches = false
        connection.readTimeout = 5000
        for((k, v) in headers){
            connection.setRequestProperty(k, v)
        }
        var cookies = getCookies(url)
        if(cookies != null) {
            connection.setRequestProperty("Cookie", cookies)
        }
        if(property != null){
            for ((key, value) in property){
                connection.setRequestProperty(key, value)
            }
        }
        connection.doInput = true
        connection.doOutput = true
        connection.setRequestProperty("Content-length", "" + data?.size)
        if(process_out != null) {
            process_out(connection.outputStream)
        }
        else if (data != null){
            val output = DataOutputStream(connection.outputStream)
            output.write(data)
            output.flush()
            output.close()
        }
        var response_code = connection.responseCode
        if(response_code < 400){
            var sb = StringBuffer()
            var readLine: String?
            // 处理响应流，必须与服务器响应流输出的编码一致
            var responseReader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
            readLine = responseReader.readLine()
            while (readLine != null) {
                sb.append(readLine).append("\n")
                readLine = responseReader.readLine()
            }
            responseReader.close();
            var headers = connection.headerFields
            if(headers.containsKey(COOKIE_KEY)){
                var list = headers[COOKIE_KEY]!!
                var result = list.joinToString(separator = ";")
                setCookies(url, result)
            }
            return unicode2String(sb.toString())
        }
        else{
            throw BadResponse(response_code)
        }
    }



    fun POST_stream(url: URL, data: Map<String, Any>?): InputStream{
        var connection = url.openConnection() as HttpURLConnection
        if (url.protocol.toLowerCase() == "https") {
            trustAllHosts()
            connection = connection as HttpsURLConnection
            connection.hostnameVerifier = DO_NOT_VERIFY
        }
        connection.requestMethod = "POST"
        connection.readTimeout = 10000
        for((k, v) in headers){
            connection.setRequestProperty(k, v)
            if(Definition.DEBUG_LOG) debug("$k:\n$v")
        }
        var cookies = getCookies(url)
        if(cookies != null) {
            connection.setRequestProperty("Cookie", cookies)
        }
        connection.doInput = true
        if(data != null){
            connection.doOutput = true
            var compiled_data = connectData(data)
            if (Definition.DEBUG_LOG) debug(compiled_data)
            if (Definition.DEBUG_LOG) debug("23333333333333333333333")
            connection.setRequestProperty("Content-length", "" + compiled_data.length)

            var printer = PrintWriter(connection.outputStream)
            printer.write(compiled_data)
            printer.flush()
            printer.close()
        }
        if (Definition.DEBUG_LOG) debug("post OK")
        var response_code = connection.responseCode
        if (Definition.DEBUG_LOG) debug(response_code)
        var headers = connection.headerFields
        if(headers.containsKey(COOKIE_KEY)){
            var list = headers[COOKIE_KEY]!!
            var result = list.joinToString(separator = ";")
            setCookies(url, result)
        }
        if(response_code < 400){
            return connection.inputStream
        }
        throw BadResponse(response_code)
    }

    fun POST(url: String, data: Map<String, Any>? = null): String{
        return POST(URL(url), data)
    }

    fun POST(url : URL, data: Map<String, Any>? = null): String{
        var stream = POST_stream(url, data)
        var sb = StringBuffer()
        var readLine: String?
        // 处理响应流，必须与服务器响应流输出的编码一致
        var responseReader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
        readLine = responseReader.readLine()
        while (readLine != null) {
            sb.append(readLine).append("\n")
            readLine = responseReader.readLine()
        }
        responseReader.close()
        if (Definition.DEBUG_LOG) debug("test debug")
        return unicode2String(sb.toString())
    }

    fun POST(url: String, data: String): String{
        return POST(URL(url), data)
    }

    fun GET_stream(url_string: String, data: Map<String, Any>? = null, tried: Boolean = false): InputStream{
        var url_s = url_string
        if(data != null){
            url_s += "?" + connectData(data)
        }
        val url = URL(url_s)
        var connection = url.openConnection() as HttpURLConnection
        if (url.protocol.toLowerCase() == "https") {
            if(Definition.DEBUG_LOG) debug("use https")
            trustAllHosts()
            connection = connection as HttpsURLConnection
            connection.hostnameVerifier = DO_NOT_VERIFY
        }
        for((k, v) in headers){
            connection.setRequestProperty(k, v)
        }
        connection.requestMethod = "GET"
        connection.readTimeout = 10000
        connection.connectTimeout = 100000
        connection.doInput = true
        connection.doOutput = false
        connection.useCaches = false
        var cookies = getCookies(url)
        if(cookies != null) {
            connection.setRequestProperty("Cookie", cookies)
        }
        var response_code = connection.responseCode
        var headers = connection.headerFields
        if(headers.containsKey(COOKIE_KEY)){
            var list = headers[COOKIE_KEY]!!
            var result = list.joinToString(separator = ";")
            setCookies(url, result)
        }
        if(response_code < 400){
            return connection.inputStream
        }
        else if(tried == false){
            return GET_stream(url_string, data, true)
        }
        throw BadResponse(response_code)
    }

    fun GET(url : String, data: Map<String, Any>? = null): String{
        var stream = GET_stream(url, data)
        var sb = StringBuffer()
        var readLine: String?
        // 处理响应流，必须与服务器响应流输出的编码一致
        var responseReader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
        readLine = responseReader.readLine()
        while (readLine != null) {
            sb.append(readLine).append("\n")
            readLine = responseReader.readLine()
        }
        responseReader.close()
        stream.close()
        return unicode2String(sb.toString())
    }


    /***
     * GET 请求，自动记录对应host，port的cookie，
     * @param url 可为String类型或是URL类型
     * @param data 可为String（标准的Query，形如name=cathor&password=123456）或是Map{'name':'cathor', 'password':'123456'}
     * */

    fun GET(url : URL, data: String): String{
        var result: URL
        result = URL(url.toString() + "?" + data)
        var connection = url.openConnection() as HttpURLConnection
        if (url.protocol.toLowerCase() == "https") {
            trustAllHosts()
            connection = connection as HttpsURLConnection
            connection.hostnameVerifier = DO_NOT_VERIFY
        }
        connection.requestMethod = "GET"
        connection.readTimeout = 10000
        for((k, v) in headers){
            connection.setRequestProperty(k, v)
        }
        connection.doInput = true
        var cookies = getCookies(url)
        if(cookies != null) {
            connection.setRequestProperty("Cookie", cookies)
        }

        var response_code = connection.responseCode
        if(response_code < 400){
            var sb = StringBuffer();
            var readLine: String;
            // 处理响应流，必须与服务器响应流输出的编码一致
            var responseReader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
            readLine = responseReader.readLine()
            while (readLine != null) {
                sb.append(readLine).append("\n");
            }
            responseReader.close();
            var headers = connection.headerFields
            if(headers.containsKey(COOKIE_KEY)){
                var list = headers[COOKIE_KEY]!!
                setCookies(url, list.joinToString(separator = ";"))
            }
            return unicode2String(sb.toString())

        }
        else{
            throw BadResponse(response_code)
        }
    }

    class UnSupportDataTypeException(var msg: String?): Exception(msg)

    /**
     * unicode 转字符串
     */
    fun unicode2String(unicode: String):String {
        if (Definition.DEBUG_LOG) debug(unicode.length)
        var pattern = Pattern.compile("\\\\u([0-9a-f]{4})")
        var temp = unicode
        var matcher = pattern.matcher(temp)
        while(matcher.find()) {
            temp = temp.replaceFirst(Regex("\\\\u[0-9a-f]{4}"), Integer.parseInt(matcher.group(1), 16).toChar().toString())
            matcher = pattern.matcher(temp)
        }
        return temp;
    }

    private fun connectData(data: Map<String, Any>): String{
        var result = ArrayList<KVpare>()
        for((k, v) in data){
            if(v is Array<*>){
                for(value in v){
                    result.add(KVpare(k, value.toString()))
                }
            }
            else if(v is MutableList<*>){
                for(value in v){
                    result.add(KVpare(k, value.toString()))
                }
            }
            else if (v is Map<*, *>){
                throw UnSupportDataTypeException("Map")
            }
            else{
                var value = v.toString()
                result.add(KVpare(k, value))
            }
        }
        var temp_string = result.joinToString(separator = "&", transform = {
            it.toString()
        })
        return temp_string
    }

    private data class KVpare(val key: String, val value:  String){
        override fun toString(): String {
            if(key.length > 0 && value.length > 0) {
                return URLEncoder.encode(key, "UTF-8").replace(' ', '+') + "=" + URLEncoder.encode(value, "UTF-8").replace(' ', '+')
            }
            return ""
        }
    }

    private fun trustAllHosts() {
        val TAG = "trustAllHosts"
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<out X509Certificate> {
                return arrayOf()
            }

            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                Log.i(TAG, "checkClientTrusted")
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                Log.i(TAG, "checkServerTrusted")
            }
        })

        // Install the all-trusting trust manager
        try {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, java.security.SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}