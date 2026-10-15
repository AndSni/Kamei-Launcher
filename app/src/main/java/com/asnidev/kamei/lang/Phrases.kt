package com.asnidev.kamei.lang

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Every phrase the home screen says, apart from dates and numbers. `{n}` is a number, `{t}` a time. */
enum class Phrase {
    NOTIFICATIONS, NO_NOTIFICATIONS, N_NOTIFICATIONS, ALLOW_NOTIFICATIONS,
    WEATHER, SET_UP_WEATHER, UPDATING, NO_LOCATION, NO_DATA,
    CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, HEAVY_RAIN, SHOWERS, SLEET, SNOW, THUNDER,
    RAIN_IN, RAIN_NOW, RAIN_STOPS_IN, SNOW_IN, SNOW_NOW, SNOW_STOPS_IN, RAIN_AT,
    KIND_MESSAGE, KIND_EMAIL, KIND_CALL, KIND_EVENT, KIND_REMINDER, KIND_ALARM, KIND_SOCIAL, KIND_PROMO, KIND_SYSTEM, KIND_OTHER,
    JUST_NOW, MIN_AGO, HOURS_AGO, N_MORE,
    WORD_OF_DAY,
    BATTERY, SIGNAL_WEAK, SIGNAL_VERY_WEAK, NO_SERVICE,
}

/** Something the home screen shows, which a long hold translates; [reading] is its kana. */
data class Words(val display: String, val translated: String, val reading: String? = null)

/**
 * The bundled dictionary: the home screen's Japanese, and each translation
 * language's text for every [Phrase], in the order the enum lists them. Nothing
 * is ever looked up online.
 */
object Phrases {

    private val JA = Locale.JAPANESE

    /** Languages the long-hold popup and the learning lines can translate into. */
    val targets: List<Pair<String, String>> = listOf(
        "en" to "English", "lv" to "Latviešu", "lt" to "Lietuvių", "et" to "Eesti", "ru" to "Русский",
        "uk" to "Українська", "de" to "Deutsch", "fr" to "Français", "es" to "Español", "pl" to "Polski",
        "fi" to "Suomi", "sv" to "Svenska", "zh" to "中文", "ko" to "한국어", "el" to "Ελληνικά",
    )

    private val table: Map<String, List<String>> = mapOf(
        "ja" to listOf(
            "通知", "新しい通知はありません", "通知{n}件", "タップして通知へのアクセスを許可",
            "天気", "タップして天気を設定", "更新中…", "位置情報なし", "天気データなし",
            "晴れ", "晴れ時々曇り", "曇り", "霧", "霧雨", "雨", "大雨", "にわか雨", "みぞれ", "雪", "雷雨",
            "あと{n}分で雨", "雨が降っています", "あと{n}分で雨がやみます", "あと{n}分で雪", "雪が降っています", "あと{n}分で雪がやみます", "{t}頃に雨",
            "メッセージ", "メール", "電話", "予定", "リマインダー", "アラーム", "SNS", "広告", "システム", "お知らせ",
            "たった今", "{n}分前", "{n}時間前", "ほか{n}件",
            "今日の言葉",
            "バッテリー {n}%", "電波が弱い", "電波がとても弱い", "圏外",
        ),
        "zh" to listOf(
            "通知", "没有新通知", "{n}条通知", "点按以允许访问通知",
            "天气", "点按以设置天气", "正在更新…", "无位置信息", "无天气数据",
            "晴", "晴间多云", "阴", "雾", "毛毛雨", "雨", "大雨", "阵雨", "雨夹雪", "雪", "雷雨",
            "{n}分钟后下雨", "正在下雨", "{n}分钟后雨停", "{n}分钟后下雪", "正在下雪", "{n}分钟后雪停", "{t}左右下雨",
            "消息", "邮件", "电话", "日程", "提醒", "闹钟", "社交", "广告", "系统", "通知",
            "刚刚", "{n}分钟前", "{n}小时前", "另外{n}条",
            "每日一词",
            "电量 {n}%", "信号弱", "信号很弱", "无服务",
        ),
        "ko" to listOf(
            "알림", "새 알림이 없습니다", "알림 {n}개", "탭하여 알림 접근 허용",
            "날씨", "탭하여 날씨 설정", "업데이트 중…", "위치 정보 없음", "날씨 정보 없음",
            "맑음", "구름 조금", "흐림", "안개", "이슬비", "비", "폭우", "소나기", "진눈깨비", "눈", "뇌우",
            "{n}분 후 비", "비가 오는 중", "{n}분 후 비 그침", "{n}분 후 눈", "눈이 오는 중", "{n}분 후 눈 그침", "{t}경 비",
            "메시지", "이메일", "전화", "일정", "리마인더", "알람", "소셜", "광고", "시스템", "알림",
            "방금", "{n}분 전", "{n}시간 전", "외 {n}개",
            "오늘의 단어",
            "배터리 {n}%", "신호 약함", "신호 매우 약함", "서비스 안 됨",
        ),
        "el" to listOf(
            "Ειδοποιήσεις", "Καμία νέα ειδοποίηση", "Ειδοποιήσεις: {n}", "Πατήστε για πρόσβαση στις ειδοποιήσεις",
            "Καιρός", "Πατήστε για ρύθμιση καιρού", "Ενημέρωση…", "Χωρίς τοποθεσία", "Χωρίς δεδομένα καιρού",
            "Αίθριος", "Αραιή συννεφιά", "Συννεφιά", "Ομίχλη", "Ψιχάλα", "Βροχή", "Ισχυρή βροχή", "Μπόρες", "Χιονόνερο", "Χιόνι", "Καταιγίδα",
            "Βροχή σε {n} λεπτά", "Βρέχει τώρα", "Η βροχή σταματά σε {n} λεπτά", "Χιόνι σε {n} λεπτά", "Χιονίζει τώρα", "Το χιόνι σταματά σε {n} λεπτά", "Βροχή γύρω στις {t}",
            "Μήνυμα", "Email", "Κλήση", "Εκδήλωση", "Υπενθύμιση", "Ξυπνητήρι", "Κοινωνικά δίκτυα", "Προσφορά", "Σύστημα", "Ειδοποίηση",
            "Μόλις τώρα", "Πριν από {n} λεπτά", "Πριν από {n} ώρες", "Και {n} ακόμη",
            "Λέξη της ημέρας",
            "Μπαταρία {n}%", "Αδύναμο σήμα", "Πολύ αδύναμο σήμα", "Χωρίς υπηρεσία",
        ),
        "en" to listOf(
            "Notifications", "No new notifications", "Notifications: {n}", "Tap to allow notification access",
            "Weather", "Tap to set up weather", "Updating…", "No location", "No weather data",
            "Clear", "Partly cloudy", "Cloudy", "Fog", "Drizzle", "Rain", "Heavy rain", "Showers", "Sleet", "Snow", "Thunderstorm",
            "Rain in {n} min", "Raining now", "Rain stops in {n} min", "Snow in {n} min", "Snowing now", "Snow stops in {n} min", "Rain around {t}",
            "Message", "Email", "Call", "Event", "Reminder", "Alarm", "Social", "Promotion", "System", "Notification",
            "Just now", "{n} min ago", "{n} h ago", "{n} more",
            "Word of the day",
            "Battery {n}%", "Weak signal", "Very weak signal", "No service",
        ),
        "lv" to listOf(
            "Paziņojumi", "Nav jaunu paziņojumu", "Paziņojumi: {n}", "Pieskarieties, lai atļautu piekļuvi paziņojumiem",
            "Laikapstākļi", "Pieskarieties, lai iestatītu laikapstākļus", "Atjaunina…", "Nav atrašanās vietas", "Nav laikapstākļu datu",
            "Skaidrs", "Mainīgs mākoņu daudzums", "Mākoņains", "Migla", "Smidzina", "Lietus", "Stiprs lietus", "Lietusgāzes", "Slapjš sniegs", "Sniegs", "Pērkona negaiss",
            "Lietus pēc {n} min", "Pašlaik līst", "Lietus beigsies pēc {n} min", "Sniegs pēc {n} min", "Pašlaik snieg", "Sniegs beigsies pēc {n} min", "Lietus ap {t}",
            "Ziņa", "E-pasts", "Zvans", "Notikums", "Atgādinājums", "Modinātājs", "Sociālie tīkli", "Reklāma", "Sistēma", "Paziņojums",
            "Tikko", "Pirms {n} min", "Pirms {n} h", "Vēl {n}",
            "Dienas vārds",
            "Akumulators {n}%", "Vājš signāls", "Ļoti vājš signāls", "Nav pārklājuma",
        ),
        "lt" to listOf(
            "Pranešimai", "Naujų pranešimų nėra", "Pranešimai: {n}", "Palieskite, kad leistumėte pasiekti pranešimus",
            "Orai", "Palieskite, kad nustatytumėte orus", "Atnaujinama…", "Vieta nežinoma", "Orų duomenų nėra",
            "Giedra", "Mažai debesuota", "Debesuota", "Rūkas", "Dulksna", "Lietus", "Smarkus lietus", "Trumpalaikis lietus", "Šlapdriba", "Sniegas", "Perkūnija",
            "Lietus po {n} min.", "Dabar lyja", "Lietus baigsis po {n} min.", "Sniegas po {n} min.", "Dabar sninga", "Sniegas baigsis po {n} min.", "Lietus apie {t}",
            "Žinutė", "El. laiškas", "Skambutis", "Įvykis", "Priminimas", "Žadintuvas", "Socialiniai tinklai", "Reklama", "Sistema", "Pranešimas",
            "Ką tik", "Prieš {n} min.", "Prieš {n} val.", "Dar {n}",
            "Dienos žodis",
            "Akumuliatorius {n}%", "Silpnas signalas", "Labai silpnas signalas", "Nėra ryšio",
        ),
        "et" to listOf(
            "Märguanded", "Uusi märguandeid pole", "Märguanded: {n}", "Puuduta, et lubada juurdepääs märguannetele",
            "Ilm", "Puuduta, et ilm seadistada", "Värskendamine…", "Asukoht puudub", "Ilmaandmed puuduvad",
            "Selge", "Vahelduv pilvisus", "Pilves", "Udu", "Uduvihm", "Vihm", "Tugev vihm", "Hoovihm", "Lörts", "Lumi", "Äike",
            "Vihm {n} min pärast", "Praegu sajab vihma", "Vihm lõpeb {n} min pärast", "Lumi {n} min pärast", "Praegu sajab lund", "Lumesadu lõpeb {n} min pärast", "Vihm umbes kell {t}",
            "Sõnum", "E-kiri", "Kõne", "Sündmus", "Meeldetuletus", "Äratus", "Sotsiaalmeedia", "Reklaam", "Süsteem", "Märguanne",
            "Just praegu", "{n} min tagasi", "{n} h tagasi", "Veel {n}",
            "Päeva sõna",
            "Aku {n}%", "Nõrk levi", "Väga nõrk levi", "Levi puudub",
        ),
        "ru" to listOf(
            "Уведомления", "Нет новых уведомлений", "Уведомления: {n}", "Нажмите, чтобы разрешить доступ к уведомлениям",
            "Погода", "Нажмите, чтобы настроить погоду", "Обновление…", "Нет местоположения", "Нет данных о погоде",
            "Ясно", "Переменная облачность", "Облачно", "Туман", "Морось", "Дождь", "Сильный дождь", "Ливни", "Мокрый снег", "Снег", "Гроза",
            "Дождь через {n} мин", "Сейчас идёт дождь", "Дождь закончится через {n} мин", "Снег через {n} мин", "Сейчас идёт снег", "Снег закончится через {n} мин", "Дождь около {t}",
            "Сообщение", "Письмо", "Звонок", "Событие", "Напоминание", "Будильник", "Соцсети", "Реклама", "Система", "Уведомление",
            "Только что", "{n} мин назад", "{n} ч назад", "Ещё {n}",
            "Слово дня",
            "Батарея {n}%", "Слабый сигнал", "Очень слабый сигнал", "Нет сети",
        ),
        "uk" to listOf(
            "Сповіщення", "Немає нових сповіщень", "Сповіщення: {n}", "Торкніться, щоб дозволити доступ до сповіщень",
            "Погода", "Торкніться, щоб налаштувати погоду", "Оновлення…", "Немає місцезнаходження", "Немає даних про погоду",
            "Ясно", "Мінлива хмарність", "Хмарно", "Туман", "Мряка", "Дощ", "Сильний дощ", "Зливи", "Мокрий сніг", "Сніг", "Гроза",
            "Дощ через {n} хв", "Зараз іде дощ", "Дощ закінчиться через {n} хв", "Сніг через {n} хв", "Зараз іде сніг", "Сніг закінчиться через {n} хв", "Дощ близько {t}",
            "Повідомлення", "Лист", "Дзвінок", "Подія", "Нагадування", "Будильник", "Соцмережі", "Реклама", "Система", "Сповіщення",
            "Щойно", "{n} хв тому", "{n} год тому", "Ще {n}",
            "Слово дня",
            "Акумулятор {n}%", "Слабкий сигнал", "Дуже слабкий сигнал", "Немає мережі",
        ),
        "de" to listOf(
            "Benachrichtigungen", "Keine neuen Benachrichtigungen", "Benachrichtigungen: {n}", "Tippen, um Zugriff auf Benachrichtigungen zu erlauben",
            "Wetter", "Tippen, um das Wetter einzurichten", "Aktualisiere…", "Kein Standort", "Keine Wetterdaten",
            "Klar", "Teilweise bewölkt", "Bewölkt", "Nebel", "Nieselregen", "Regen", "Starkregen", "Schauer", "Schneeregen", "Schnee", "Gewitter",
            "Regen in {n} Min.", "Es regnet gerade", "Regen endet in {n} Min.", "Schnee in {n} Min.", "Es schneit gerade", "Schnee endet in {n} Min.", "Regen gegen {t}",
            "Nachricht", "E-Mail", "Anruf", "Termin", "Erinnerung", "Wecker", "Soziale Netzwerke", "Werbung", "System", "Benachrichtigung",
            "Gerade eben", "vor {n} Min.", "vor {n} Std.", "{n} weitere",
            "Wort des Tages",
            "Akku {n} %", "Schwaches Signal", "Sehr schwaches Signal", "Kein Netz",
        ),
        "fr" to listOf(
            "Notifications", "Aucune nouvelle notification", "Notifications : {n}", "Appuyez pour autoriser l'accès aux notifications",
            "Météo", "Appuyez pour configurer la météo", "Mise à jour…", "Aucune position", "Aucune donnée météo",
            "Ciel dégagé", "Partiellement nuageux", "Nuageux", "Brouillard", "Bruine", "Pluie", "Forte pluie", "Averses", "Neige fondue", "Neige", "Orage",
            "Pluie dans {n} min", "Il pleut", "Fin de la pluie dans {n} min", "Neige dans {n} min", "Il neige", "Fin de la neige dans {n} min", "Pluie vers {t}",
            "Message", "E-mail", "Appel", "Événement", "Rappel", "Alarme", "Réseaux sociaux", "Promotion", "Système", "Notification",
            "À l'instant", "Il y a {n} min", "Il y a {n} h", "{n} de plus",
            "Mot du jour",
            "Batterie {n} %", "Signal faible", "Signal très faible", "Aucun service",
        ),
        "es" to listOf(
            "Notificaciones", "No hay notificaciones nuevas", "Notificaciones: {n}", "Toca para permitir el acceso a las notificaciones",
            "Tiempo", "Toca para configurar el tiempo", "Actualizando…", "Sin ubicación", "Sin datos meteorológicos",
            "Despejado", "Parcialmente nublado", "Nublado", "Niebla", "Llovizna", "Lluvia", "Lluvia intensa", "Chubascos", "Aguanieve", "Nieve", "Tormenta",
            "Lluvia en {n} min", "Está lloviendo", "La lluvia para en {n} min", "Nieve en {n} min", "Está nevando", "La nieve para en {n} min", "Lluvia hacia las {t}",
            "Mensaje", "Correo", "Llamada", "Evento", "Recordatorio", "Alarma", "Redes sociales", "Promoción", "Sistema", "Notificación",
            "Ahora mismo", "Hace {n} min", "Hace {n} h", "{n} más",
            "Palabra del día",
            "Batería {n} %", "Señal débil", "Señal muy débil", "Sin servicio",
        ),
        "pl" to listOf(
            "Powiadomienia", "Brak nowych powiadomień", "Powiadomienia: {n}", "Dotknij, aby zezwolić na dostęp do powiadomień",
            "Pogoda", "Dotknij, aby skonfigurować pogodę", "Aktualizowanie…", "Brak lokalizacji", "Brak danych pogodowych",
            "Bezchmurnie", "Częściowe zachmurzenie", "Pochmurno", "Mgła", "Mżawka", "Deszcz", "Ulewa", "Przelotne opady", "Deszcz ze śniegiem", "Śnieg", "Burza",
            "Deszcz za {n} min", "Teraz pada deszcz", "Deszcz ustanie za {n} min", "Śnieg za {n} min", "Teraz pada śnieg", "Śnieg ustanie za {n} min", "Deszcz około {t}",
            "Wiadomość", "E-mail", "Połączenie", "Wydarzenie", "Przypomnienie", "Alarm", "Społecznościowe", "Promocja", "System", "Powiadomienie",
            "Przed chwilą", "{n} min temu", "{n} godz. temu", "Jeszcze {n}",
            "Słowo dnia",
            "Bateria {n}%", "Słaby sygnał", "Bardzo słaby sygnał", "Brak zasięgu",
        ),
        "fi" to listOf(
            "Ilmoitukset", "Ei uusia ilmoituksia", "Ilmoitukset: {n}", "Napauta salliaksesi ilmoitusten käytön",
            "Sää", "Napauta määrittääksesi sään", "Päivitetään…", "Ei sijaintia", "Ei säätietoja",
            "Selkeää", "Puolipilvistä", "Pilvistä", "Sumua", "Tihkusadetta", "Sadetta", "Rankkasadetta", "Sadekuuroja", "Räntää", "Lunta", "Ukkosta",
            "Sadetta {n} min päästä", "Nyt sataa", "Sade lakkaa {n} min päästä", "Lunta {n} min päästä", "Nyt sataa lunta", "Lumisade lakkaa {n} min päästä", "Sadetta noin klo {t}",
            "Viesti", "Sähköposti", "Puhelu", "Tapahtuma", "Muistutus", "Herätys", "Some", "Mainos", "Järjestelmä", "Ilmoitus",
            "Juuri nyt", "{n} min sitten", "{n} h sitten", "{n} lisää",
            "Päivän sana",
            "Akku {n} %", "Heikko signaali", "Erittäin heikko signaali", "Ei verkkoa",
        ),
        "sv" to listOf(
            "Aviseringar", "Inga nya aviseringar", "Aviseringar: {n}", "Tryck för att tillåta åtkomst till aviseringar",
            "Väder", "Tryck för att ställa in vädret", "Uppdaterar…", "Ingen plats", "Inga väderdata",
            "Klart", "Halvklart", "Mulet", "Dimma", "Duggregn", "Regn", "Kraftigt regn", "Regnskurar", "Snöblandat regn", "Snö", "Åska",
            "Regn om {n} min", "Det regnar nu", "Regnet upphör om {n} min", "Snö om {n} min", "Det snöar nu", "Snön upphör om {n} min", "Regn runt {t}",
            "Meddelande", "E-post", "Samtal", "Händelse", "Påminnelse", "Alarm", "Sociala medier", "Kampanj", "System", "Avisering",
            "Nyss", "För {n} min sedan", "För {n} tim sedan", "{n} till",
            "Dagens ord",
            "Batteri {n} %", "Svag signal", "Mycket svag signal", "Ingen täckning",
        ),
    )

    fun has(lang: String): Boolean = lang in table

    /** The phrase as the home screen writes it, in Japanese. */
    fun ja(phrase: Phrase, n: Int? = null, t: String? = null): String = say("ja", phrase, n, t)

    /** The phrase in [lang] (English when the dictionary lacks that language). */
    fun say(lang: String, phrase: Phrase, n: Int? = null, t: String? = null): String {
        val text = (table[lang] ?: table.getValue("en"))[phrase.ordinal]
        return text.replace("{n}", n?.toString() ?: "").replace("{t}", t ?: "")
    }

    /** "system" → the phone's language when the dictionary has it, else English. */
    fun resolveTarget(target: String): String {
        if (target != "system") return target
        val phone = Locale.getDefault().language
        return if (has(phone)) phone else "en"
    }

    /**
     * The language translations go into: [target], unless that's Japanese (a phone
     * set to Japanese, say: no use translating Japanese into Japanese), then English.
     */
    fun effectiveTarget(target: String): String = resolveTarget(target).takeIf { it != "ja" } ?: "en"

    fun words(target: String, phrase: Phrase, n: Int? = null, t: String? = null) =
        Words(ja(phrase, n, t), say(effectiveTarget(target), phrase, n, t), JaReading.say(phrase, n, t))

    fun weekday(now: LocalDateTime): String = DateTimeFormatter.ofPattern("EEEE", JA).format(now)

    fun date(now: LocalDateTime): String = DateTimeFormatter.ofPattern("M月d日", JA).format(now)

    /** The whole date in the target language, e.g. "Wednesday, October 14, 2026". */
    fun fullDate(target: String, now: LocalDateTime): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.forLanguageTag(resolveTarget(target))).format(now)

    /** "5 min ago" style, in [lang]. */
    fun ago(lang: String, then: Long, now: Long): String {
        val min = ((now - then) / 60_000).toInt()
        return when {
            min < 1 -> say(lang, Phrase.JUST_NOW)
            min < 60 -> say(lang, Phrase.MIN_AGO, min)
            else -> say(lang, Phrase.HOURS_AGO, min / 60)
        }
    }

    fun time(target: String, now: LocalDateTime): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.forLanguageTag(resolveTarget(target))).format(now)
}
