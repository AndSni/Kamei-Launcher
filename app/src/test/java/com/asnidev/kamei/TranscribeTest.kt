package com.asnidev.kamei

import com.asnidev.kamei.lang.DisplayLang
import com.asnidev.kamei.lang.Transcribe
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscribeTest {

    private fun ja(s: String) = Transcribe.label(s, DisplayLang.JA)
    private fun ko(s: String) = Transcribe.label(s, DisplayLang.KO)
    private fun el(s: String) = Transcribe.label(s, DisplayLang.EL)

    @Test
    fun knownNames() {
        assertEquals("ユーチューブ", ja("YouTube"))
        assertEquals("유튜브", ko("YouTube"))
        assertEquals("Γιουτιουμπ", el("YouTube"))
        assertEquals("クローム", ja("Chrome"))
        assertEquals("크롬", ko("Chrome"))
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
        assertEquals("킨들", ko("Kindle"))
        assertEquals("포켓", ko("Pocket"))
        assertEquals("Σαπ Ραϊτ", el("Sharp Right"))
    }

    @Test
    fun leavesOtherScriptsAlone() {
        assertEquals("設定", ja("設定"))
        assertEquals("Яндекс", ja("Яндекс"))
        assertEquals("YouTube", Transcribe.label("YouTube", DisplayLang.ZH))
        assertEquals("1ワン", ja("1One"))
    }
}
