package com.github.herokotlin.messageinput

import com.github.herokotlin.emotioninput.filter.EmotionFilter
import com.github.herokotlin.emotioninput.model.EmotionSet

interface MessageInputConfiguration {

    fun getVoiceRecordSavePath(): String

    fun getCameraRecordSavePath(): String

    fun getEmotionFilters(): List<EmotionFilter> {
        return listOf()
    }

    fun getEmotionSets(): List<EmotionSet> {
        return listOf()
    }


}