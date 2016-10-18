package cn.cathor.muzeipixivsearch.items

/**
 * Created by Cathor on 2016/10/16.
 */

class PixivLogin {


    var response: ResponseBean? = null

    class ResponseBean {
        var access_token: String? = null
        var expires_in: Int = 0
        var token_type: String? = null

        var user: UserBean? = null
        var scope: String? = null
        var refresh_token: String? = null



        class UserBean {
            var account: String? = null
            var name: String? = null
            var id: String? = null
            var isIs_premium: Boolean = false
            var isIs_mail_authorized: Boolean = false

            var profile_image_urls: ProfileImageUrlsBean? = null
            var x_restrict: Int = 0

            class ProfileImageUrlsBean {
                var px_170x170: String? = null
                var px_50x50: String? = null
                var px_16x16: String? = null
                override fun toString(): String {
                    return "ProfileImageUrlsBean(px_170x170=$px_170x170, px_50x50=$px_50x50, px_16x16=$px_16x16)"
                }

            }

            override fun toString(): String {
                return "UserBean(account=$account, name=$name, id=$id, isIs_premium=$isIs_premium, isIs_mail_authorized=$isIs_mail_authorized, profile_image_urls=$profile_image_urls, x_restrict=$x_restrict)"
            }

        }

        override fun toString(): String {
            return "ResponseBean(access_token=$access_token, expires_in=$expires_in, token_type=$token_type, user=$user, scope=$scope, refresh_token=$refresh_token)"
        }
    }

    override fun toString(): String {
        return "PixivLogin(response=$response)"
    }
}
