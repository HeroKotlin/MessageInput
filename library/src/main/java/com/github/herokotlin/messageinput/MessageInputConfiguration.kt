package com.github.herokotlin.messageinput

import android.content.Context
import android.widget.ImageView

abstract class MessageInputConfiguration(val context: Context) {

    /**
     * 表情和文本的高度比例
     */
    var emotionTextHeightRatio = 1.1f

    /**
     * 加载图片
     */
    abstract fun loadImage(imageView: ImageView, url: String)

    /**
     * 请求权限
     */
    abstract fun requestPermissions(permissions: List<String>, requestCode: Int): Boolean

}