package cn.cathor.muzeipixivsearch.controller

import cn.cathor.muzeipixivsearch.Definition
import cn.cathor.muzeipixivsearch.debug
import cn.cathor.muzeipixivsearch.items.PixivItem
import cn.cathor.muzeipixivsearch.items.PixivItemBean
import cn.cathor.muzeipixivsearch.items.PixivLogin
import cn.cathor.muzeipixivsearch.urlclient.URLClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.orm.SugarRecord
import org.jetbrains.anko.async
import org.jetbrains.anko.uiThread
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Created by Cathor on 2016/10/14.
 */

class RequestController{

    companion object{
        private val timePattern = Pattern.compile("img/(\\d{4})/(\\d{2})/(\\d{2})/(\\d{2})/(\\d{2})/(\\d{2})")
        private var _instace: RequestController? = null
        fun getInstance(): RequestController{return _instace!!}
    }

    val client: URLClient

    private constructor(cookies: MutableMap<String, MutableMap<String, String>>, headers: MutableMap<String, String>){
        this.client = URLClient(headers = headers, manager = cookies)
    }



    private class LittleItem(val item: PixivItem, val star: Int)


    public class Builder{
        private var headers: MutableMap<String, String> = hashMapOf(
                "Accept-Encoding" to "identity",
                "Accept-Language" to "zh-CN,zh;q=0.8",
                "Connection" to "keep-alive",
                "Content-Charset" to "UTF-8",
                "Accept-Charset" to "UTF-8",
                "Accept" to "*/*",
                "User-Agent" to "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.94 Safari/537.36",
                "Cache-Control" to "max-age=0",
                "Referer" to "http://www.pixiv.net/")

        private var cookies: MutableMap<String, MutableMap<String, String>> = hashMapOf()
        fun setHeaders(headers: MutableMap<String, String>): Builder{
            this.headers = headers
            return this
        }


        fun setCookies(cookies: String): Builder{
            this.cookies = Gson().fromJson(cookies, object : TypeToken<HashMap<String, HashMap<String, String>>>(){}.type)
            return this
        }

        fun setCookies(cookies: MutableMap<String, MutableMap<String, String>>): Builder{
            this.cookies = cookies
            return this
        }

        fun build(): RequestController{
            _instace = RequestController(cookies, headers)
            return getInstance()
        }
    }

    fun formatURL(tag: String, page: Int, mode: String) : Map<String, String>{
        return mapOf("sort" to "date", "include_stats" to "True", "period" to "all",
                "include_sanity_level" to "True", "page" to "$page", "q" to "$tag",
                "mode" to mode, "per_page" to "20", "image_sizes" to "px_480mw,large",
                "order" to "desc", "types" to "illustration")
    }

    fun search(id: String, password: String, tag: String, allowedTime: Int, minStar: Int, dailyCount: Int, callback: (MutableList<PixivItem>) -> Unit, error: (Exception) -> Unit, status: ((String) -> Unit)? = null, r18: Boolean = true, mode: String = "tag"){
        async() {
            try {
                uiThread {
                    status?.invoke("Login")
                }
                if (Definition.DEBUG_LOG) debug(allowedTime)
                val login_url = "https://oauth.secure.pixiv.net/auth/token"
                val login_params = mapOf("username" to id, "password" to password, "grant_type" to "password",
                        "client_id" to "bYGKuGVw91e0NMfPGp44euvGt59s", "client_secret" to "HP3RmkgAmEGro0gn1x9ioawQE8WMfvLXDz3ZqxpK")
                client.putHeaders("User-Agent", "PixivIOSApp/5.1.1")
                val login_response = client.POST(login_url, login_params)
                if (Definition.DEBUG_LOG) debug("this is wtf $login_response")
                val login = Gson().fromJson(login_response, PixivLogin::class.java)
                if (Definition.DEBUG_LOG) debug(login)
                client.putHeaders("Authorization", "Bearer ${login.response!!.access_token}")
                //client.putHeaders("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.94 Safari/537.36")

                var minDate: Calendar? = null
                val timeFormat = "yyyy-MM-dd"
                val dateFormat = SimpleDateFormat(timeFormat)
                if (allowedTime > 0){
                    var dateNow = Date()
                    dateNow = dateFormat.parse(dateFormat.format(dateNow))
                    if (Definition.DEBUG_LOG) debug(dateNow)
                    minDate = Calendar.getInstance()
                    minDate.time = dateNow
                    minDate.add(Calendar.DATE, -allowedTime)
                }
                if (Definition.DEBUG_LOG) debug(minDate)
                val baseURL = "https://public-api.secure.pixiv.net/v1/search/works.json"

                var page = 1
                var params: Map<String, String>? = formatURL(tag, page, mode)
                val groupByItems = mutableMapOf<String, MutableList<LittleItem>>()
                while (params != null) {
                    uiThread {
                        status?.invoke("Processing page $page")
                    }
                    if(Definition.DEBUG_LOG) debug(params)
                    val content = client.GET_stream(baseURL, params)
                    page += 1
                    params = formatURL(tag, page, mode)

                    val pixiv_response = Gson().fromJson(BufferedReader(InputStreamReader(content)), PixivItemBean::class.java)
                    if (Definition.DEBUG_LOG) debug(pixiv_response.pagination)

                    if (pixiv_response.pagination!!.next == 0){
                        params = null
                    }
                    for (item in pixiv_response.response!!){
                        val time = Calendar.getInstance()
                        time.clear()
                        val neededTime = item.created_time!!.substring(0, 10)
                        time.time = dateFormat.parse(neededTime)
                        if (Definition.DEBUG_LOG) debug(item.age_limit)
                        if(!r18 && item.age_limit != "all-age") {
                            if (Definition.DEBUG_LOG) debug("passed")
                            continue
                        }
                        if (Definition.DEBUG_LOG) debug(neededTime)
                        if (minDate == null || !minDate.time.after(time.time) ){
                            val link = "http://www.pixiv.net/member_illust.php?mode=medium&illust_id=${item.id}"
                            val title = item.title
                            val author = item.user!!.name
                            val description = item.caption
                            val imgUrl = item.image_urls!!.large
                            val smallImgUrl = item.image_urls!!.px_480mw
                            val star = item.stats!!.favorited_count!!.privateX + item.stats!!.favorited_count!!.publicX
                            if(neededTime !in groupByItems.keys){
                                groupByItems.put(neededTime, mutableListOf())
                            }
                            groupByItems[neededTime]!!.add(LittleItem(PixivItem(title!!, "$author, $description", link, imgUrl!!, smallImgUrl!!, neededTime), star))
                        }
                        else{
                            params = null
                        }
                    }
                }
                val mappedItems = groupByItems.map {
                    var temp_list = it.value.filter { it.star > minStar }.sortedBy { it.star }
                    temp_list.subList(0, if (dailyCount > temp_list.size) temp_list.size else dailyCount).map { it.item }
                }
                val itemsList = mutableListOf<PixivItem>()
                for (items in mappedItems){
                    itemsList.addAll(items)
                }
                uiThread {
                    status?.invoke("Storing ${itemsList.size} images")
                }
                SugarRecord.deleteAll(PixivItem::class.java)
                for(item in itemsList){
                    item.save()
                }
                uiThread {
                    callback(itemsList)
                }
            }
            catch (e: Exception){
                uiThread {
                    error(e)
                    if (Definition.DEBUG_LOG) debug(e)
                }
            }
        }
    }
}