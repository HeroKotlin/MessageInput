package com.github.herokotlin.messageinput.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout

import com.github.herokotlin.messageinput.R
import kotlinx.android.synthetic.main.feature_button.view.*

class FeatureButton: LinearLayout {

    lateinit var onClick: () -> Unit

    var icon = 0

        set(value) {
            field = value
            if (value > 0) {
                iconView.setImageResource(value)
            }
        }

    var title = ""

        set(value) {
            field = value
            titleView.text = value
        }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {

        LayoutInflater.from(context).inflate(R.layout.feature_button, this)

        val typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.FeatureButton, defStyle, 0)

        icon = typedArray.getResourceId(R.styleable.FeatureButton_feature_button_icon, 0)

        title = typedArray.getString(R.styleable.FeatureButton_feature_button_title) ?: ""

        // 获取完 TypedArray 的值后，
        // 一般要调用 recycle 方法来避免重新创建的时候出错
        typedArray.recycle()

        iconView.setOnClickListener {
            onClick()
        }

    }
}