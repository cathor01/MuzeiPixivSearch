package cn.cathor.muzeipixivsearch.application

import cn.cathor.muzeipixivsearch.controller.RequestController
import com.orm.SugarApp

/**
 * Created by Cathor on 2016/10/16.
 */

class MyApplication: SugarApp(){
    override fun onCreate() {
        super.onCreate()
        var builder = RequestController.Builder()
        builder.build()
    }
}