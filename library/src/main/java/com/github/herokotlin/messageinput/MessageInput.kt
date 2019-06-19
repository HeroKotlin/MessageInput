package com.github.herokotlin.messageinput

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.view.WindowManager
import android.graphics.BitmapFactory
import android.support.v4.content.ContextCompat
import android.widget.FrameLayout
import android.widget.ImageView
import com.github.herokotlin.circleview.CircleView
import com.github.herokotlin.circleview.CircleViewCallback
import com.github.herokotlin.emotioninput.EmotionInputCallback
import com.github.herokotlin.emotioninput.EmotionInputConfiguration
import com.github.herokotlin.emotioninput.filter.EmotionFilter
import com.github.herokotlin.emotioninput.model.Emotion
import com.github.herokotlin.emotioninput.model.EmotionSet
import com.github.herokotlin.messageinput.enum.AdjustMode
import com.github.herokotlin.messageinput.enum.FeatureType
import com.github.herokotlin.messageinput.enum.ViewMode
import com.github.herokotlin.messageinput.model.ImageFile
import com.github.herokotlin.messageinput.view.FeatureButton
import com.github.herokotlin.permission.Permission
import com.github.herokotlin.voiceinput.VoiceInputCallback
import com.github.herokotlin.voiceinput.VoiceInputConfiguration
import kotlinx.android.synthetic.main.message_input.view.*

class MessageInput : LinearLayout {

    companion object {

        // 必须小于 2^16，否则 startActivityForResult 会报错
        const val CAMERA_ACTIVITY_REQUEST_CODE = 59143

        fun setSoftInputMode(activity: Activity, resize: Boolean) {

            activity.window.setSoftInputMode(
                if (resize) WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            )

        }
    }

    lateinit var configuration: MessageInputConfiguration

    lateinit var callback: MessageInputCallback

    // 用于请求权限
    var activity: Activity? = null

        set(value) {

            if (field == value) {
                return
            }

            field = value

            voicePanel.activity = value

        }

    var viewMode = ViewMode.KEYBOARD

        set(value) {

            if (field == value) {
                return
            }

            when (value) {
                ViewMode.VOICE -> {
                    voicePanel.requestPermissions()
                    voicePanel.visibility = View.VISIBLE
                    emotionPanel.visibility = View.GONE
                    morePanel.visibility = View.GONE
                }
                ViewMode.EMOTION -> {
                    voicePanel.visibility = View.GONE
                    emotionPanel.visibility = View.VISIBLE
                    morePanel.visibility = View.GONE
                }
                ViewMode.MORE -> {
                    voicePanel.visibility = View.GONE
                    emotionPanel.visibility = View.GONE
                    morePanel.visibility = View.VISIBLE
                }
            }

            // 切换到语音、表情、更多
            if (value != ViewMode.KEYBOARD) {

                showContentPanel()

                if (field == ViewMode.KEYBOARD && !contentPanel.isKeyboardVisible) {
                    callback.onLift()
                }

                // 只要切到其他 view mode 都要改成 nothing
                // 否则，当再次聚焦输入框时，高度会有问题
                adjustMode = AdjustMode.NOTHING

                // 基于 nothing 模式，软键盘落下去不会影响布局
                hideKeyboard()

            }

            field = value

        }

    private var adjustMode = AdjustMode.DEFAULT

        set(value) {

            if (field == value) {
                return
            }

            val resize = when (value) {
                AdjustMode.NOTHING -> {
                    false
                }
                else -> {
                    true
                }
            }

            setSoftInputMode(activity ?: (context as Activity), resize)

            field = value

        }

    private var plainText = ""

        set(value) {

            if (field == value) {
                return
            }

            if (field.isBlank() or value.isBlank()) {
                if (field.isBlank()) {
                    sendButton.visibility = View.VISIBLE
                    moreButton.visibility = View.GONE
                    emotionPanel.isSubmitButtonEnabled = true
                }
                else {
                    sendButton.visibility = View.GONE
                    moreButton.visibility = View.VISIBLE
                    emotionPanel.isSubmitButtonEnabled = false
                }
            }

            field = value

            callback.onTextChange(value)

        }

    private var isFeatureListCreated = false

    private val videoPermission = Permission(19906, listOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    ))

    private val featurePanelPaddingVertical: Int by lazy {
        resources.getDimensionPixelSize(R.dimen.message_input_feature_panel_padding_vertical)
    }

    private val featureButtonWidth: Int by lazy {
        resources.getDimensionPixelSize(R.dimen.message_input_feature_button_width)
    }

    private val featureButtonHeight: Int by lazy {
        resources.getDimensionPixelSize(R.dimen.message_input_feature_button_height)
    }

    private val featureButtonRowSpacing: Int by lazy {
        resources.getDimensionPixelSize(R.dimen.message_input_feature_button_row_spacing)
    }

    private val featureButtonColumnSpacing: Int by lazy {
        resources.getDimensionPixelSize(R.dimen.message_input_feature_button_column_spacing)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun init(configuration: MessageInputConfiguration, callback: MessageInputCallback) {
        this.configuration = configuration
        this.callback = callback
        init()
    }

    private fun init() {

        LayoutInflater.from(context).inflate(R.layout.message_input, this)

        textarea.emotionTextHeightRatio = configuration.emotionTextHeightRatio

        textarea.onTextChange = {
            plainText = textarea.text.toString()
        }

        emotionPanel.configuration = object: EmotionInputConfiguration() {
            override fun loadImage(imageView: ImageView, url: String) {
                configuration.loadImage(imageView, url)
            }
        }

        emotionPanel.callback = object: EmotionInputCallback {
            override fun onEmotionClick(emotion: Emotion) {
                if (emotion.inline) {
                    textarea.insertEmotion(emotion)
                }
                else {
                    callback.onSendEmotion(emotion)
                }
            }

            override fun onDeleteClick() {
                textarea.deleteBackward()
            }

            override fun onSubmitClick() {
                sendText()
            }
        }

        val circleViewCallback = object: CircleViewCallback {

            override fun onTouchDown(circleView: CircleView) {
                circleView.centerColor = ContextCompat.getColor(context, R.color.message_input_circle_button_bg_color_pressed)
                circleView.invalidate()
            }

            override fun onTouchUp(circleView: CircleView, inside: Boolean, isLongPress: Boolean) {
                circleView.centerColor = ContextCompat.getColor(context, R.color.message_input_circle_button_bg_color_normal)
                circleView.invalidate()
                if (inside) {
                    if (circleView == voiceButton) {
                        if (viewMode == ViewMode.VOICE) {
                            showKeyboard()
                        }
                        else {
                            viewMode = ViewMode.VOICE
                        }
                    }
                    else if (circleView == emotionButton) {
                        if (viewMode == ViewMode.EMOTION) {
                            showKeyboard()
                        }
                        else {
                            viewMode = ViewMode.EMOTION
                        }
                    }
                    else if (circleView == moreButton) {
                        if (viewMode == ViewMode.MORE) {
                            showKeyboard()
                        }
                        else {
                            viewMode = ViewMode.MORE
                        }
                    }
                }
            }

            override fun onTouchEnter(circleView: CircleView) {
                circleView.centerColor = ContextCompat.getColor(context, R.color.message_input_circle_button_bg_color_pressed)
                circleView.invalidate()
            }

            override fun onTouchLeave(circleView: CircleView) {
                circleView.centerColor = ContextCompat.getColor(context, R.color.message_input_circle_button_bg_color_normal)
                circleView.invalidate()
            }
        }

        voiceButton.callback = circleViewCallback

        emotionButton.callback = circleViewCallback

        moreButton.callback = circleViewCallback

        sendButton.setOnClickListener {
            sendText()
        }

        val voiceInputConfiguration = object: VoiceInputConfiguration() { }

        voiceInputConfiguration.audioBitRate = configuration.audioBitRate
        voiceInputConfiguration.audioSampleRate = configuration.audioSampleRate

        voicePanel.init(
            voiceInputConfiguration,
            object: VoiceInputCallback {

                override fun onFinishRecord(audioPath: String, audioDuration: Int) {
                    callback.onSendAudio(audioPath, audioDuration)
                }

                override fun onPlayButtonClick() {
                    callback.onUseAudio()
                }

                override fun onRecordButtonClick() {
                    callback.onUseAudio()
                }

                override fun onRecordDurationLessThanMinDuration() {
                    callback.onRecordAudioDurationLessThanMinDuration()
                }

                override fun onPermissionsGranted() {
                    callback.onRecordAudioPermissionsGranted()
                }

                override fun onPermissionsDenied() {
                    callback.onRecordAudioPermissionsDenied()
                }

                override fun onExternalStorageNotWritable() {
                    callback.onRecordAudioExternalStorageNotWritable()
                }

                override fun onPermissionsNotGranted() {
                    callback.onRecordAudioPermissionsNotGranted()
                }

                override fun onRequestPermissions(activity: Activity, permissions: Array<out String>, requestCode: Int) {
                    callback.onRequestPermissions(activity, permissions, requestCode)
                }
            }
        )

        videoPermission.onExternalStorageNotWritable = {
            callback.onRecordVideoExternalStorageNotWritable()
        }
        videoPermission.onPermissionsNotGranted = {
            callback.onRecordVideoPermissionsNotGranted()
        }
        videoPermission.onPermissionsGranted = {
            callback.onRecordVideoPermissionsGranted()
        }
        videoPermission.onPermissionsDenied = {
            callback.onRecordVideoPermissionsDenied()
        }
        videoPermission.onRequestPermissions = { activity, permissions, requestCode ->
            callback.onRequestPermissions(activity, permissions, requestCode)
        }

        textarea.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {

                var isLift = false

                if (viewMode != ViewMode.KEYBOARD) {
                    viewMode = ViewMode.KEYBOARD
                }
                else {
                    isLift = true
                }

                val callback = {
                    // 从别的模式切过来的
                    if (adjustMode == AdjustMode.NOTHING && viewMode == ViewMode.KEYBOARD) {
                        hideContentPanel()
                        adjustMode = AdjustMode.RESIZE
                    }
                    if (isLift) {
                        callback.onLift()
                    }
                }

                // 等软键盘起来后再改成 resize
                // 方便下次能正常触发
                postDelayed(callback, 400)

            }
        }

        contentPanel.onVisibleChange = { isVisible ->
            if (!isVisible) {
                hideKeyboard()
                if (viewMode == ViewMode.KEYBOARD) {
                    adjustMode = AdjustMode.RESIZE
                    hideContentPanel()
                    callback.onFall()
                }
            }
        }

        // 初始布局可自动调整大小
        adjustMode = AdjustMode.RESIZE

    }

    fun reset() {
        if (viewMode == ViewMode.KEYBOARD) {
            if (contentPanel.isKeyboardVisible) {
                hideKeyboard()
                hideContentPanel()
            }
        }
        else {
            viewMode = ViewMode.KEYBOARD
            hideContentPanel()
            callback.onFall()
        }
    }

    fun getText(): String {
        return plainText
    }

    fun setText(text: String) {
        textarea.clear()
        textarea.insertText(text)
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

    /**
     * 确保音频可用，通常是外部要用音频了
     */
    fun ensureAudioAvailable() {
        if (voicePanel.isRecording) {
            voicePanel.stopRecord()
        }
        else if (voicePanel.isPlaying) {
            voicePanel.stopPlay()
        }
    }

    private fun sendText() {
        if (plainText.isNotBlank()) {
            callback.onSendText(plainText)
            textarea.clear()
        }
    }

    private fun openCameraActivity() {

        if (!videoPermission.checkExternalStorageWritable()) {
            return
        }

        val context = activity ?: (context as Activity)

        videoPermission.requestPermissions(context) {
            val intent = Intent(context, CameraActivity::class.java)
            context.startActivityForResult(intent, CAMERA_ACTIVITY_REQUEST_CODE)
        }

    }

    private fun readImage(path: String): ImageFile {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        return ImageFile(path, options.outWidth, options.outHeight)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && requestCode == CAMERA_ACTIVITY_REQUEST_CODE) {
            when (resultCode) {
                CameraActivity.RESULT_CODE_VIDEO -> {
                    val path = data.getStringExtra("thumbnail")
                    callback.onSendVideo(
                        data.getStringExtra("video"),
                        data.getIntExtra("duration", 0),
                        readImage(path)
                    )
                }
                CameraActivity.RESULT_CODE_PHOTO -> {
                    val path = data.getStringExtra("photo")
                    callback.onSendPhoto(readImage(path))
                }
            }
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        voicePanel.onRequestPermissionsResult(requestCode, permissions, grantResults)

        videoPermission.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ensureFeatureListCreated(w)
    }

    private fun ensureFeatureListCreated(width: Int) {

        if (isFeatureListCreated || width <= 0) {
            return
        }

        // 屏幕宽度不可控，因此改成计算获得
        val columnCount = 4

        val columnSpaing = (columnCount - 1) * featureButtonColumnSpacing

        val buttonWidth = featureButtonWidth
        val buttonHeight = featureButtonHeight

        val paddingHorizontal = (width - columnCount * buttonWidth - columnSpaing) / 2
        val paddingVertical = featurePanelPaddingVertical

        val featureList = configuration.featureList
        for (i in 0 until featureList.count()) {

            val featureButton: FeatureButton = when (featureList[i]) {
                FeatureType.PHOTO -> {
                    createFeatureButton(R.string.message_input_photo_feature_title, R.drawable.message_input_photo_feature_icon) {
                        callback.onClickPhotoFeature()
                    }
                }
                FeatureType.CAMERA -> {
                    createFeatureButton(R.string.message_input_camera_feature_title, R.drawable.message_input_camera_feature_icon) {
                        openCameraActivity()
                    }
                }
                FeatureType.FILE -> {
                    createFeatureButton(R.string.message_input_file_feature_title, R.drawable.message_input_file_feature_icon) {
                        callback.onClickFileFeature()
                    }
                }
                FeatureType.USER -> {
                    createFeatureButton(R.string.message_input_user_feature_title, R.drawable.message_input_user_feature_icon) {
                        callback.onClickUserFeature()
                    }
                }
                FeatureType.MOVIE -> {
                    createFeatureButton(R.string.message_input_movie_feature_title, R.drawable.message_input_movie_feature_icon) {
                        callback.onClickMovieFeature()
                    }
                }
                FeatureType.PHONE -> {
                    createFeatureButton(R.string.message_input_phone_feature_title, R.drawable.message_input_phone_feature_icon) {
                        callback.onClickPhoneFeature()
                    }
                }
                FeatureType.LOCATION -> {
                    createFeatureButton(R.string.message_input_location_feature_title, R.drawable.message_input_location_feature_icon) {
                        callback.onClickLocationFeature()
                    }
                }
                FeatureType.FAVOR -> {
                    createFeatureButton(R.string.message_input_favor_feature_title, R.drawable.message_input_favor_feature_icon) {
                        callback.onClickFavorFeature()
                    }
                }
            }

            val row = Math.floor(i.toDouble() / columnCount).toInt()
            val column = i % columnCount

            val layoutParams = featureButton.layoutParams as FrameLayout.LayoutParams

            layoutParams.setMargins(
                paddingHorizontal + column * (buttonWidth + featureButtonColumnSpacing),
                paddingVertical + row * (buttonHeight + featureButtonRowSpacing),
                0,0
            )

        }

        isFeatureListCreated = true

    }

    private fun createFeatureButton(title: Int, image: Int, onClick: () -> Unit): FeatureButton {

        val featureButton = FeatureButton(context)

        featureButton.icon = image
        featureButton.title = resources.getString(title)
        featureButton.onClick = onClick

        morePanel.addView(featureButton)

        return featureButton

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