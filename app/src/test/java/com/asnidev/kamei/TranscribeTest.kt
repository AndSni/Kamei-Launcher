package com.asnidev.kamei

import com.asnidev.kamei.lang.Transcribe
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscribeTest {

    private fun ja(s: String) = Transcribe.label(s)

    @Test
    fun knownNames() {
        assertEquals("ユーチューブ", ja("YouTube"))
        assertEquals("クローム", ja("Chrome"))
        assertEquals("カメイ", ja("Kamei"))
    }

    @Test
    fun soundRules() {
        assertEquals("スポティファイ", ja("Spotify"))
        assertEquals("オーガニック・マップ", ja("Organic Maps"))
        assertEquals("シャープ・ライト", ja("Sharp Right"))
        assertEquals("ラジオ・ステーション", ja("Radio Station"))
        assertEquals("ポケット", ja("Pocket"))
        assertEquals("モニター", ja("Monitor"))
        assertEquals("ブイエルシー", ja("VLC"))
    }

    @Test
    fun leavesOtherScriptsAlone() {
        assertEquals("設定", ja("設定"))
        assertEquals("Яндекс", ja("Яндекс"))
        assertEquals("1ワン", ja("1One"))
    }
}
