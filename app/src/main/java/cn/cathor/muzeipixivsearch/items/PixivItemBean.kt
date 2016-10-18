package cn.cathor.muzeipixivsearch.items

import com.google.gson.annotations.SerializedName

class PixivItemBean {
    var status: String? = null
    var count: Int = 0

    var pagination: PaginationBean? = null

    var response: List<ResponseBean>? = null

    class PaginationBean {
        var previous: Any? = null
        var next: Int = 0
        var current: Int = 0
        var per_page: Int = 0
        var total: Int = 0
        var pages: Int = 0
    }

    class ResponseBean {
        var id: Int = 0
        var title: String? = null
        var caption: String? = null
        /**
         * px_480mw : http://i2.pixiv.net/c/480x960/img-master/img/2016/10/16/23/47/41/59506169_p0_master1200.jpg
         * large : http://i2.pixiv.net/img-original/img/2016/10/16/23/47/41/59506169_p0.jpg
         */

        var image_urls: ImageUrlsBean? = null
        var width: Int = 0
        var height: Int = 0
        /**
         * scored_count : 0
         * score : 0
         * views_count : 1
         * favorited_count : {"public":0,"private":0}
         * commented_count : 0
         */

        var stats: StatsBean? = null
        var publicity: Int = 0
        var age_limit: String? = null
        var created_time: String? = null
        var reuploaded_time: String? = null

        var user: UserBean? = null
        var isIs_manga: Boolean = false
        var isIs_liked: Boolean = false
        var favorite_id: Int = 0
        var page_count: Int = 0
        var book_style: String? = null
        var type: String? = null
        var metadata: Any? = null
        var content_type: Any? = null
        var sanity_level: String? = null
        var tags: List<String>? = null
        var tools: List<*>? = null

        class ImageUrlsBean {
            var px_480mw: String? = null
            var large: String? = null
        }

        class StatsBean {
            var scored_count: Int = 0
            var score: Int = 0
            var views_count: Int = 0
            /**
             * public : 0
             * private : 0
             */

            var favorited_count: FavoritedCountBean? = null
            var commented_count: Int = 0

            class FavoritedCountBean {
                @SerializedName("public")
                var publicX: Int = 0
                @SerializedName("private")
                var privateX: Int = 0
            }
        }

        class UserBean {
            var id: Int = 0
            var account: String? = null
            var name: String? = null
            var isIs_following: Boolean = false
            var isIs_follower: Boolean = false
            var isIs_friend: Boolean = false
            var is_premium: Any? = null
            /**
             * px_50x50 : http://i1.pixiv.net/user-profile/img/2011/03/20/10/30/28/2886248_030f3d83361eb493900128bcd9f7a734_50.jpg
             */

            var profile_image_urls: ProfileImageUrlsBean? = null
            var stats: Any? = null
            var profile: Any? = null

            class ProfileImageUrlsBean {
                var px_50x50: String? = null
            }
        }
    }
}