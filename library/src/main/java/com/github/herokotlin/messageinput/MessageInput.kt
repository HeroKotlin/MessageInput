package com.github.herokotlin.messageinput

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.util.AttributeSet
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.message_input.view.*
import android.view.WindowManager
import android.widget.Toast
import com.zhihu.matisse.engine.impl.GlideEngine
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import com.github.herokotlin.emotioninput.EmotionInputCallback
import com.github.herokotlin.emotioninput.model.Emotion
import com.github.herokotlin.messageinput.model.Image
import com.github.herokotlin.voiceinput.VoiceInputCallback
import com.github.herokotlin.voiceinput.VoiceManager
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType

class MessageInput : LinearLayout {

    companion object {

        const val LOG_TAG = "MessageInput"

        const val CAMERA_PERMISSION_REQUEST_CODE = 1321

        const val CAMERA_ACTIVITY_REQUEST_CODE = 1322

        const val IMAGE_PERMISSION_REQUEST_CODE = 1323

        const val IMAGE_ACTIVITY_REQUEST_CODE = 1324

        // 打字输入模式
        const val VIEW_MODE_KEYBOARD = 0

        // 语音输入模式
        const val VIEW_MODE_VOICE = 1

        // 表情输入模式
        const val VIEW_MODE_EMOTION = 2

        // 更多输入模式
        const val VIEW_MODE_MORE = 3

        // resize
        const val ADJUST_MODE_RESIZE = 1

        // nothing
        const val ADJUST_MODE_NOTHING = 2

    }

    var callback = object: Callback { }

    private lateinit var configuration: Configuration

    private var viewMode = VIEW_MODE_KEYBOARD

        set(value) {

            if (field == value) {
                return
            }

            when (value) {
                VIEW_MODE_VOICE -> {
                    voicePanel.requestPermissions()
                    voicePanel.visibility = View.VISIBLE
                    emotionPanel.visibility = View.GONE
                    morePanel.visibility = View.GONE
                }
                VIEW_MODE_EMOTION -> {
                    voicePanel.visibility = View.GONE
                    emotionPanel.visibility = View.VISIBLE
                    morePanel.visibility = View.GONE
                }
                VIEW_MODE_MORE -> {
                    voicePanel.visibility = View.GONE
                    emotionPanel.visibility = View.GONE
                    morePanel.visibility = View.VISIBLE
                }
            }

            // 切换到语音、表情、更多
            if (value != VIEW_MODE_KEYBOARD) {

                showContent()

                if (field == VIEW_MODE_KEYBOARD && !contentPanel.isKeyboardVisible) {
                    callback.onLift()
                }

                // 只要切到其他 view mode 都要改成 nothing
                // 否则，当再次聚焦输入框时，高度会有问题
                adjustMode = ADJUST_MODE_NOTHING

                // 基于 nothing 模式，软键盘落下去不会影响布局
                blurInput()

            }

            field = value

        }

    private var adjustMode = 0

        set(value) {
            if (field == value) {
                return
            }

            val mode = when (value) {
                ADJUST_MODE_NOTHING -> { WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING }
                else -> { WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE }
            }

            (context as Activity).window.setSoftInputMode(mode)

            field = value
        }

    private var content = ""

        set(value) {

            if (field == value) {
                return
            }

            if (field.isBlank() or value.isBlank()) {
                if (field.isBlank()) {
                    sendButton.visibility = View.VISIBLE
                    moreButton.visibility = View.GONE
                }
                else {
                    sendButton.visibility = View.GONE
                    moreButton.visibility = View.VISIBLE
                }
            }

            field = value

        }

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

        LayoutInflater.from(context).inflate(R.layout.message_input, this)

        textarea.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                content = textarea.text.toString()
            }
        })

        emotionPanel.callback = object: EmotionInputCallback {
            override fun onEmotionClick(emotion: Emotion) {
                if (emotion.inline) {
                    textarea.text.insert(textarea.selectionStart, emotion.code)
                }
                else {
                    callback.onEmotionSend(emotion)
                }
            }

            override fun onDeleteClick() {
                val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                textarea.onKeyDown(KeyEvent.KEYCODE_DEL, event)
            }

            override fun onSendClick() {
                onSendClick()
            }
        }

        voiceButton.setOnClickListener {
            viewMode = when (viewMode) {
                VIEW_MODE_VOICE -> { VIEW_MODE_KEYBOARD }
                else -> { VIEW_MODE_VOICE }
            }
            if (viewMode == VIEW_MODE_KEYBOARD) {
                focusInput()
            }
        }

        emotionButton.setOnClickListener {
            viewMode = when (viewMode) {
                VIEW_MODE_EMOTION -> { VIEW_MODE_KEYBOARD }
                else -> { VIEW_MODE_EMOTION }
            }
            if (viewMode == VIEW_MODE_KEYBOARD) {
                focusInput()
            }
        }

        moreButton.setOnClickListener {
            viewMode = when (viewMode) {
                VIEW_MODE_MORE -> { VIEW_MODE_KEYBOARD }
                else -> { VIEW_MODE_MORE }
            }
            if (viewMode == VIEW_MODE_KEYBOARD) {
                focusInput()
            }
        }

        sendButton.setOnClickListener {
            onSendClick()
        }

        imageButton.onClick = {
            requestImagePermissions()
        }

        cameraButton.onClick = {
            requestCameraPermissions()
        }

        voicePanel.callback = object: VoiceInputCallback {

            override fun onFinishRecord(filePath: String, duration: Int) {
                callback.onAudioRecordSuccess(filePath, duration)
            }

        }

        textarea.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {

                var isLift = false

                if (viewMode != VIEW_MODE_KEYBOARD) {
                    viewMode = VIEW_MODE_KEYBOARD
                }
                else {
                    isLift = true
                }

                // 等软键盘起来后再改成 resize
                // 方便下次能正常触发

                postDelayed(
                    {
                        if (adjustMode == ADJUST_MODE_NOTHING && viewMode == VIEW_MODE_KEYBOARD) {
                            hideContent()
                            adjustMode = ADJUST_MODE_RESIZE
                        }
                        if (isLift) {
                            callback.onLift()
                        }
                    },
                    400
                )

            }
        }

        contentPanel.onVisibleChange = {
            if (!it) {
                blurInput()
                if (viewMode == VIEW_MODE_KEYBOARD) {
                    adjustMode = ADJUST_MODE_RESIZE
                    hideContent()
                }
            }
        }

        // 初始布局可自动调整大小
        adjustMode = ADJUST_MODE_RESIZE

    }

    fun init(configuration: Configuration) {

        this.configuration = configuration

        for (filter in configuration.getEmotionFilters()) {
            textarea.addFilter(filter)
        }

        emotionPanel.emotionSetList = configuration.getEmotionSets()

//        voiceInput.savePath = configuration.getVoiceRecordSavePath()

    }

    private fun onSendClick() {
        val text = textarea.text
        if (text.isNotBlank()) {
            callback.onTextSend(text.toString())
            text.clear()
        }
    }

    private fun openCameraActivity() {

        val intent = Intent(context, CameraActivity::class.java)

        intent.putExtra("savePath", configuration.getCameraRecordSavePath())

        (context as Activity).startActivityForResult(intent, CAMERA_ACTIVITY_REQUEST_CODE)

    }

    private fun openImageActivity() {

        Matisse.from(context as Activity)
                .choose(MimeType.ofImage())
                .theme(R.style.Matisse_Zhihu)
                .countable(true)
                .maxSelectable(9)
                .gridExpectedSize(resources.getDimensionPixelSize(R.dimen.message_input_image_grid_expected_size))
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .thumbnailScale(0.85f)
                .originalEnable(true)
                .imageEngine(GlideEngine())
                .forResult(IMAGE_ACTIVITY_REQUEST_CODE)
    }

    private fun requestCameraPermissions() {

        var permissions = arrayOf<String>()

        if (!voicePanel.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissions = permissions.plus(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (!voicePanel.hasPermission(Manifest.permission.RECORD_AUDIO)) {
            permissions = permissions.plus(Manifest.permission.RECORD_AUDIO)
        }

        if (!voicePanel.hasPermission(Manifest.permission.CAMERA)) {
            permissions = permissions.plus(Manifest.permission.CAMERA)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                context as Activity,
                permissions,
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
        else {
            openCameraActivity()
        }

    }

    private fun requestImagePermissions() {

        var permissions = arrayOf<String>()

        if (!voicePanel.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissions = permissions.plus(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                context as Activity,
                permissions,
                IMAGE_PERMISSION_REQUEST_CODE
            )
        }
        else {
            openImageActivity()
        }

    }

    private fun readImage(path: String): Image {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        return Image(path, options.outWidth, options.outHeight)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null) {
            if (requestCode == CAMERA_ACTIVITY_REQUEST_CODE) {
                when (resultCode) {
                    CameraActivity.RESULT_CODE_VIDEO -> {
                        val path = data.getStringExtra("firstFrame")
                        callback.onVideoRecordSuccess(
                            data.getStringExtra("video"),
                            data.getIntExtra("duration", 0),
                            readImage(path)
                        )
                    }
                    CameraActivity.RESULT_CODE_PHOTO -> {
                        val path = data.getStringExtra("photo")
                        callback.onPhotoCaptureSuccess(readImage(path))
                    }
                }
            }
            else if (requestCode == IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                callback.onMediaSelectSuccess(
                    Matisse.obtainPathResult(data).map { readImage(it) }
                )
            }
        }
    }

    fun requestPermissionsResult(requestCode: Int, grantResults: IntArray) {

        voicePanel.requestPermissionsResult(requestCode, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.count() == 3) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED
                ) {
                    openCameraActivity()
                    return
                }
            }
            Toast.makeText(context, R.string.message_input_request_camera_permissions_failed, Toast.LENGTH_SHORT).show()
        }
        else if (requestCode == IMAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.count() == 1) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImageActivity()
                    return
                }
            }
            Toast.makeText(context, R.string.message_input_request_image_permissions_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun requestPermission(permission: String): Boolean {

        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(permission),
                VoiceManager.PERMISSION_REQUEST_CODE
            )
            return false
        }

        return true

    }

    fun minimize() {
        if (viewMode == VIEW_MODE_KEYBOARD) {
            if (contentPanel.isKeyboardVisible) {
                blurInput()
                hideContent()
                callback.onFall()
            }
        }
        else {
            viewMode = VIEW_MODE_KEYBOARD
            hideContent()
            callback.onFall()
        }
    }

    private fun showContent() {
        contentPanel.visibility = View.VISIBLE
    }

    private fun hideContent() {
        contentPanel.visibility = View.GONE
    }

    private fun focusInput() {
        textarea.requestFocus()
        val inputManager = textarea.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.showSoftInput(textarea, 0)
    }

    private fun blurInput() {
        textarea.clearFocus()
        val inputManager = textarea.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(textarea.windowToken, 0)
    }

}