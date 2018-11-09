package com.github.herokotlin.messageinput.view

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.FrameLayout

class KeyboardLayout : FrameLayout {

    companion object {
        const val KEY_KEYBOARD_HEIGHT = "message_input_keyboard_height"
    }

    var keyboardHeight = 0

    var isKeyboardVisible = false

    var onVisibleChange: ((isVisible: Boolean) -> Unit)? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    private fun init() {

        val preference = (context as Activity).getPreferences(Context.MODE_PRIVATE)
        val savedHeight = preference.getInt(KEY_KEYBOARD_HEIGHT, 0)
        if (savedHeight > 0) {
            keyboardHeight = savedHeight
        }

        val rect = Rect()

        viewTreeObserver.addOnGlobalLayoutListener {

            getWindowVisibleDisplayFrame(rect)

            val screenHeight = resources.displayMetrics.heightPixels

            val height = screenHeight - rect.bottom

            // 大于 1/5 则认为是打开了软键盘
            val isVisible = height > screenHeight / 5
            if (isVisible != isKeyboardVisible) {
                isKeyboardVisible = isVisible
                val onChange = onVisibleChange
                if (onChange != null) {
                    onChange(isVisible)
                }
            }

            if (height != keyboardHeight) {
                keyboardHeight = height
                if (isVisible) {
                    layoutParams.height = height
                    val editor = preference.edit()
                    editor.putInt(KEY_KEYBOARD_HEIGHT, height)
                    editor.apply()
                }
            }

        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (keyboardHeight > 0) {
            layoutParams.height = keyboardHeight
        }
    }

}