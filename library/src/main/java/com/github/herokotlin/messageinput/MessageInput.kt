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
import android.text.Editable
import android.text.TextWatcher
import com.github.herokotlin.emotioninput.EmotionInputCallback
import com.github.herokotlin.emotioninput.filter.EmotionFilter
import com.github.herokotlin.emotioninput.model.Emotion
import com.github.herokotlin.emotioninput.model.EmotionSet
import com.github.herokotlin.messageinput.model.Image
import com.github.herokotlin.voiceinput.VoiceInputCallback
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType

class MessageInput : LinearLayout {

    companion object {

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

    var callback = object: MessageInputCallback { }

    private lateinit var configuration: MessageInputConfiguration

    var viewMode = VIEW_MODE_KEYBOARD

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

                showContentPanel()

                if (field == VIEW_MODE_KEYBOARD && !contentPanel.isKeyboardVisible) {
                    callback.onLift()
                }

                // 只要切到其他 view mode 都要改成 nothing
                // 否则，当再次聚焦输入框时，高度会有问题
                adjustMode = ADJUST_MODE_NOTHING

                // 基于 nothing 模式，软键盘落下去不会影响布局
                hideKeyboard()

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

    private var text = ""

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
                text = textarea.text.toString()
            }
        })

        emotionPanel.callback = object: EmotionInputCallback {
            override fun onEmotionClick(emotion: Emotion) {
                if (emotion.inline) {
                    textarea.insertEmotion(emotion)
                }
                else {
                    callback.onEmotionSend(emotion)
                }
            }

            override fun onDeleteClick() {
                textarea.deleteBackward()
            }

            override fun onSendClick() {
                sendText()
            }
        }

        voiceButton.setOnClickListener {
            if (viewMode == VIEW_MODE_VOICE) {
                viewMode = VIEW_MODE_KEYBOARD
                showKeyboard()
            }
            else {
                viewMode = VIEW_MODE_VOICE
            }
        }

        emotionButton.setOnClickListener {
            if (viewMode == VIEW_MODE_EMOTION) {
                viewMode = VIEW_MODE_KEYBOARD
                showKeyboard()
            }
            else {
                viewMode = VIEW_MODE_EMOTION
            }
        }

        moreButton.setOnClickListener {
            if (viewMode == VIEW_MODE_MORE) {
                viewMode = VIEW_MODE_KEYBOARD
                showKeyboard()
            }
            else {
                viewMode = VIEW_MODE_MORE
            }
        }

        sendButton.setOnClickListener {
            sendText()
        }

        imageButton.onClick = {
            requestImagePermissions()
        }

        cameraButton.onClick = {
            requestCameraPermissions()
        }

        voicePanel.callback = object: VoiceInputCallback {

            override fun onFinishRecord(filePath: String, duration: Int) {
                callback.onVoiceSend(filePath, duration)
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
                            hideContentPanel()
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
                hideKeyboard()
                if (viewMode == VIEW_MODE_KEYBOARD) {
                    adjustMode = ADJUST_MODE_RESIZE
                    hideContentPanel()
                }
            }
        }

        // 初始布局可自动调整大小
        adjustMode = ADJUST_MODE_RESIZE

    }

    fun init(configuration: MessageInputConfiguration) {

        this.configuration = configuration

//        voiceInput.savePath = configuration.getVoiceRecordSavePath()

    }

    fun setEmotionSetList(emotionSetList: List<EmotionSet>) {
        emotionPanel.emotionSetList = emotionSetList
    }

    fun addEmotionFilter(emotionFilter: EmotionFilter) {
        textarea.addFilter(emotionFilter)
    }

    fun removeEmotionFilter(emotionFilter: EmotionFilter) {
        textarea.removeFilter(emotionFilter)
    }

    fun minimize() {
        if (viewMode == VIEW_MODE_KEYBOARD) {
            if (contentPanel.isKeyboardVisible) {
                hideKeyboard()
                hideContentPanel()
                callback.onFall()
            }
        }
        else {
            viewMode = VIEW_MODE_KEYBOARD
            hideContentPanel()
            callback.onFall()
        }
    }

    private fun sendText() {
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
                .spanCount(4)
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
                        val image = readImage(path)
                        callback.onVideoSend(
                            data.getStringExtra("video"),
                            data.getIntExtra("duration", 0),
                            image.path,
                            image.width,
                            image.height
                        )
                    }
                    CameraActivity.RESULT_CODE_PHOTO -> {
                        val path = data.getStringExtra("photo")
                        val image = readImage(path)
                        callback.onPhotoSend(image.path, image.width, image.height)
                    }
                }
            }
            else if (requestCode == IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                callback.onImageSend(
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

    private fun showContentPanel() {
        contentPanel.visibility = View.VISIBLE
    }

    private fun hideContentPanel() {
        contentPanel.visibility = View.GONE
    }

    private fun showKeyboard() {
        textarea.requestFocus()
        val inputManager = textarea.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.showSoftInput(textarea, 0)
    }

    private fun hideKeyboard() {
        textarea.clearFocus()
        val inputManager = textarea.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(textarea.windowToken, 0)
    }

}