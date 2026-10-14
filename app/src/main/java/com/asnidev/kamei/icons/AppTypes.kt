package com.asnidev.kamei.icons

import android.content.pm.ApplicationInfo

/**
 * Kinds of app, each with words a search may use for it and "hint" glyphs: ones
 * that nod at the app sideways (music → a feather, a wave, the wind) so only the
 * owner makes the link. Never the obvious symbol, which would give the app away.
 */
enum class AppType(val label: String, val words: List<String>, val hints: List<String>) {
    PHONE("Phone", listOf("phone", "call", "dial", "dialer", "telephone"), listOf("ear", "spiral", "antenna", "satellite", "bell")),
    MESSAGES("Messages", listOf("message", "messages", "sms", "chat", "messenger", "text"), listOf("bottle", "feather", "paper-bag", "balloon", "bubble")),
    EMAIL("Email", listOf("mail", "email", "gmail", "inbox", "post"), listOf("send", "bottle", "mailbox", "sailboat", "parachute")),
    CAMERA("Camera", listOf("camera", "photo", "selfie", "scan"), listOf("eye", "sunglasses", "flare", "moon-2", "telescope")),
    GALLERY("Photos", listOf("gallery", "photos", "pictures", "album", "images"), listOf("frame", "mountain", "butterfly", "sunrise", "leaf")),
    MUSIC("Music", listOf("music", "audio", "songs", "player", "radio", "podcast", "sound"), listOf("feather", "wave-sine", "wind", "butterfly", "spiral")),
    VIDEO("Video", listOf("video", "tv", "movies", "films", "stream", "youtube"), listOf("armchair", "moon-stars", "ufo", "lamp", "ice-cream")),
    BROWSER("Browser", listOf("browser", "web", "internet", "chrome", "firefox"), listOf("spider", "planet", "compass", "fish-hook", "zeppelin")),
    MAPS("Maps", listOf("maps", "map", "navigation", "gps", "directions"), listOf("shoe", "compass", "trees", "anchor", "tent")),
    CALENDAR("Calendar", listOf("calendar", "agenda", "schedule", "events", "planner"), listOf("moon", "seedling", "hourglass", "plant-2", "leaf")),
    CLOCK("Clock", listOf("clock", "alarm", "timer", "stopwatch"), listOf("hourglass", "egg", "candle", "sun-high", "snowman")),
    SETTINGS("Settings", listOf("settings", "system", "preferences", "config"), listOf("puzzle", "anchor", "magnet", "hexagon", "bolt")),
    WEATHER("Weather", listOf("weather", "forecast", "rain", "yr", "drops", "meteo"), listOf("sailboat", "windmill", "kayak", "cactus", "umbrella-2")),
    NOTES("Notes", listOf("notes", "notepad", "memo", "keep", "journal", "docs", "write"), listOf("feather", "seedling", "cloud", "leaf", "lamp")),
    MONEY("Money", listOf("bank", "pay", "wallet", "money", "finance", "crypto", "budget"), listOf("anchor", "crown", "diamond", "seedling", "pig")),
    SHOPPING("Shopping", listOf("shop", "shopping", "store", "market", "buy", "cart"), listOf("gift", "balloon", "cherry", "lollipop", "hanger")),
    SOCIAL("Social", listOf("social", "friends", "feed", "community", "dating"), listOf("campfire", "confetti", "balloon", "torii", "sparkles")),
    GAMES("Games", listOf("game", "games", "play", "puzzle"), listOf("dice", "chess", "ghost", "rocket", "pumpkin-scary")),
    FITNESS("Fitness", listOf("fitness", "health", "workout", "run", "steps", "sport", "ride", "strava"), listOf("mountain", "flame", "bike", "swimming", "trophy")),
    CONTACTS("Contacts", listOf("contacts", "people", "address book"), listOf("tree", "puzzle", "key", "fingerprint", "lock-heart")),
    FILES("Files", listOf("files", "file manager", "storage", "downloads", "drive", "cloud"), listOf("box", "pyramid", "mushroom", "bread", "cookie")),
    CALCULATOR("Calculator", listOf("calculator", "calc", "math"), listOf("abacus", "triangle-square-circle", "infinity", "pyramid", "dice")),
    NEWS("News", listOf("news", "reader", "rss", "magazine"), listOf("coffee", "mug", "sunrise", "bubble-tea", "telescope")),
    FOOD("Food", listOf("food", "delivery", "recipes", "restaurant", "eat"), listOf("carrot", "pepper", "lemon-2", "grill", "cake")),
    TRAVEL("Travel", listOf("travel", "flights", "trains", "bus", "taxi", "hotel", "transport"), listOf("zeppelin", "tent", "sailboat", "parachute", "cactus")),
    ;

    companion object {
        /** App types whose words appear in [query] ("music", "bank app"…). */
        fun matching(query: String): List<AppType> {
            val q = query.trim().lowercase()
            if (q.length < 3) return emptyList()
            return entries.filter { t -> t.words.any { w -> w.startsWith(q) || q.split(' ').any { it == w } } }
        }

        /**
         * A best guess at what kind of app this is, from Android's own category,
         * the app's label and its package name. Null when nothing fits.
         */
        fun guess(pkg: String, label: String, category: Int): AppType? {
            val text = "$pkg $label".lowercase().replace('.', ' ').replace('_', ' ')
            // Specific words first: a "photo editor" is a gallery, not a camera.
            ORDER.firstOrNull { t -> t.words.any { w -> Regex("\\b${Regex.escape(w)}").containsMatchIn(text) } }?.let { return it }
            return when (category) {
                ApplicationInfo.CATEGORY_GAME -> GAMES
                ApplicationInfo.CATEGORY_AUDIO -> MUSIC
                ApplicationInfo.CATEGORY_VIDEO -> VIDEO
                ApplicationInfo.CATEGORY_IMAGE -> GALLERY
                ApplicationInfo.CATEGORY_SOCIAL -> SOCIAL
                ApplicationInfo.CATEGORY_NEWS -> NEWS
                ApplicationInfo.CATEGORY_MAPS -> MAPS
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> NOTES
                else -> null
            }
        }

        private val ORDER = listOf(
            WEATHER, CALCULATOR, CALENDAR, CONTACTS, GALLERY, CAMERA, EMAIL, MESSAGES, PHONE, CLOCK, MAPS, MUSIC, VIDEO,
            BROWSER, MONEY, SHOPPING, FOOD, TRAVEL, FITNESS, NEWS, NOTES, FILES, SOCIAL, GAMES, SETTINGS,
        )
    }
}
