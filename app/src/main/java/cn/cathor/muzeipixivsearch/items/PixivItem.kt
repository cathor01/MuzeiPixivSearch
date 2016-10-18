package cn.cathor.muzeipixivsearch.items

import com.orm.SugarRecord
import com.orm.dsl.Table

/**
 * Created by Cathor on 2016/10/15.
 */


class PixivItem: SugarRecord {
    internal var title: String? = null

    internal var description: String? = null

    internal var url: String? = null

    internal var imgUrl: String? = null

    internal var time: String? = null

    internal var smallImgUrl: String? = null

    constructor(): super()

    constructor(title: String, description: String, url: String, imgUrl: String, smallImgUrl: String, time: String){
        this.title = title
        this.description = description
        this.url = url
        this.imgUrl = imgUrl
        this.time = time
        this.smallImgUrl = smallImgUrl
    }
}