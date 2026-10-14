package com.asnidev.kamei.lang

import java.time.LocalDate

/** One word of the day: the word in every language, and how to read it in the non-Latin ones. */
class VocabWord(val key: String, private val words: Map<String, String>, private val readings: Map<String, String>) {
    fun word(lang: String): String = words[lang] ?: words.getValue("en")

    /** Kana for Japanese, pinyin for Chinese, romanisation for Korean and Greek. */
    fun reading(lang: String): String? = readings[lang]
}

/**
 * The bundled word list for "word of the day": everyday words (nature, seasons,
 * home, greetings), so each day's word is useful and the list never needs the net.
 */
object Vocabulary {

    private val LANGS = listOf("en", "lv", "lt", "et", "ru", "uk", "de", "fr", "es", "pl", "fi", "sv")

    // key | en lv lt et ru uk de fr es pl fi sv | ja kana | zh pinyin | ko romanised | el romanised
    private const val TABLE = """
water|water|ūdens|vanduo|vesi|вода|вода|Wasser|eau|agua|woda|vesi|vatten|水|みず|水|shuǐ|물|mul|νερό|neró
fire|fire|uguns|ugnis|tuli|огонь|вогонь|Feuer|feu|fuego|ogień|tuli|eld|火|ひ|火|huǒ|불|bul|φωτιά|fotiá
mountain|mountain|kalns|kalnas|mägi|гора|гора|Berg|montagne|montaña|góra|vuori|berg|山|やま|山|shān|산|san|βουνό|vounó
river|river|upe|upė|jõgi|река|річка|Fluss|rivière|río|rzeka|joki|flod|川|かわ|河|hé|강|gang|ποτάμι|potámi
sea|sea|jūra|jūra|meri|море|море|Meer|mer|mar|morze|meri|hav|海|うみ|海|hǎi|바다|bada|θάλασσα|thálassa
sky|sky|debesis|dangus|taevas|небо|небо|Himmel|ciel|cielo|niebo|taivas|himmel|空|そら|天空|tiānkōng|하늘|haneul|ουρανός|ouranós
rain|rain|lietus|lietus|vihm|дождь|дощ|Regen|pluie|lluvia|deszcz|sade|regn|雨|あめ|雨|yǔ|비|bi|βροχή|vrochí
snow|snow|sniegs|sniegas|lumi|снег|сніг|Schnee|neige|nieve|śnieg|lumi|snö|雪|ゆき|雪|xuě|눈|nun|χιόνι|chióni
wind|wind|vējš|vėjas|tuul|ветер|вітер|Wind|vent|viento|wiatr|tuuli|vind|風|かぜ|风|fēng|바람|baram|άνεμος|ánemos
cloud|cloud|mākonis|debesis|pilv|облако|хмара|Wolke|nuage|nube|chmura|pilvi|moln|雲|くも|云|yún|구름|gureum|σύννεφο|sýnnefo
sun|sun|saule|saulė|päike|солнце|сонце|Sonne|soleil|sol|słońce|aurinko|sol|太陽|たいよう|太阳|tàiyáng|해|hae|ήλιος|ílios
moon|moon|mēness|mėnulis|kuu|луна|місяць|Mond|lune|luna|księżyc|kuu|måne|月|つき|月亮|yuèliang|달|dal|φεγγάρι|fengári
star|star|zvaigzne|žvaigždė|täht|звезда|зірка|Stern|étoile|estrella|gwiazda|tähti|stjärna|星|ほし|星星|xīngxing|별|byeol|αστέρι|astéri
tree|tree|koks|medis|puu|дерево|дерево|Baum|arbre|árbol|drzewo|puu|träd|木|き|树|shù|나무|namu|δέντρο|déntro
flower|flower|zieds|gėlė|lill|цветок|квітка|Blume|fleur|flor|kwiat|kukka|blomma|花|はな|花|huā|꽃|kkot|λουλούδι|louloúdi
forest|forest|mežs|miškas|mets|лес|ліс|Wald|forêt|bosque|las|metsä|skog|森|もり|森林|sēnlín|숲|sup|δάσος|dásos
stone|stone|akmens|akmuo|kivi|камень|камінь|Stein|pierre|piedra|kamień|kivi|sten|石|いし|石头|shítou|돌|dol|πέτρα|pétra
bird|bird|putns|paukštis|lind|птица|птах|Vogel|oiseau|pájaro|ptak|lintu|fågel|鳥|とり|鸟|niǎo|새|sae|πουλί|poulí
fish|fish|zivs|žuvis|kala|рыба|риба|Fisch|poisson|pez|ryba|kala|fisk|魚|さかな|鱼|yú|물고기|mulgogi|ψάρι|psári
cat|cat|kaķis|katė|kass|кошка|кіт|Katze|chat|gato|kot|kissa|katt|猫|ねこ|猫|māo|고양이|goyangi|γάτα|gáta
dog|dog|suns|šuo|koer|собака|собака|Hund|chien|perro|pies|koira|hund|犬|いぬ|狗|gǒu|개|gae|σκύλος|skýlos
horse|horse|zirgs|arklys|hobune|лошадь|кінь|Pferd|cheval|caballo|koń|hevonen|häst|馬|うま|马|mǎ|말|mal|άλογο|álogo
morning|morning|rīts|rytas|hommik|утро|ранок|Morgen|matin|mañana|poranek|aamu|morgon|朝|あさ|早上|zǎoshang|아침|achim|πρωί|proí
night|night|nakts|naktis|öö|ночь|ніч|Nacht|nuit|noche|noc|yö|natt|夜|よる|夜晚|yèwǎn|밤|bam|νύχτα|nýchta
today|today|šodien|šiandien|täna|сегодня|сьогодні|heute|aujourd'hui|hoy|dzisiaj|tänään|idag|今日|きょう|今天|jīntiān|오늘|oneul|σήμερα|símera
tomorrow|tomorrow|rīt|rytoj|homme|завтра|завтра|morgen|demain|mañana|jutro|huomenna|i morgon|明日|あした|明天|míngtiān|내일|naeil|αύριο|ávrio
time|time|laiks|laikas|aeg|время|час|Zeit|temps|tiempo|czas|aika|tid|時間|じかん|时间|shíjiān|시간|sigan|χρόνος|chrónos
friend|friend|draugs|draugas|sõber|друг|друг|Freund|ami|amigo|przyjaciel|ystävä|vän|友達|ともだち|朋友|péngyou|친구|chingu|φίλος|fílos
family|family|ģimene|šeima|perekond|семья|сім'я|Familie|famille|familia|rodzina|perhe|familj|家族|かぞく|家庭|jiātíng|가족|gajok|οικογένεια|oikogéneia
home|home|mājas|namai|kodu|дом|дім|Zuhause|maison|casa|dom|koti|hem|家|いえ|家|jiā|집|jip|σπίτι|spíti
book|book|grāmata|knyga|raamat|книга|книга|Buch|livre|libro|książka|kirja|bok|本|ほん|书|shū|책|chaek|βιβλίο|vivlío
music|music|mūzika|muzika|muusika|музыка|музика|Musik|musique|música|muzyka|musiikki|musik|音楽|おんがく|音乐|yīnyuè|음악|eumak|μουσική|mousikí
food|food|ēdiens|maistas|toit|еда|їжа|Essen|nourriture|comida|jedzenie|ruoka|mat|食べ物|たべもの|食物|shíwù|음식|eumsik|φαγητό|fagitó
tea|tea|tēja|arbata|tee|чай|чай|Tee|thé|té|herbata|tee|te|お茶|おちゃ|茶|chá|차|cha|τσάι|tsái
bread|bread|maize|duona|leib|хлеб|хліб|Brot|pain|pan|chleb|leipä|bröd|パン|パン|面包|miànbāo|빵|ppang|ψωμί|psomí
apple|apple|ābols|obuolys|õun|яблоко|яблуко|Apfel|pomme|manzana|jabłko|omena|äpple|りんご|りんご|苹果|píngguǒ|사과|sagwa|μήλο|mílo
town|town|pilsēta|miestelis|linn|город|місто|Stadt|ville|pueblo|miasto|kaupunki|stad|町|まち|城镇|chéngzhèn|마을|maeul|πόλη|póli
road|road|ceļš|kelias|tee|дорога|дорога|Straße|route|camino|droga|tie|väg|道|みち|路|lù|길|gil|δρόμος|drómos
train|train|vilciens|traukinys|rong|поезд|потяг|Zug|train|tren|pociąg|juna|tåg|電車|でんしゃ|火车|huǒchē|기차|gicha|τρένο|tréno
bicycle|bicycle|velosipēds|dviratis|jalgratas|велосипед|велосипед|Fahrrad|vélo|bicicleta|rower|polkupyörä|cykel|自転車|じてんしゃ|自行车|zìxíngchē|자전거|jajeongeo|ποδήλατο|podílato
school|school|skola|mokykla|kool|школа|школа|Schule|école|escuela|szkoła|koulu|skola|学校|がっこう|学校|xuéxiào|학교|hakgyo|σχολείο|scholeío
work|work|darbs|darbas|töö|работа|робота|Arbeit|travail|trabajo|praca|työ|arbete|仕事|しごと|工作|gōngzuò|일|il|δουλειά|douleiá
love|love|mīlestība|meilė|armastus|любовь|любов|Liebe|amour|amor|miłość|rakkaus|kärlek|愛|あい|爱|ài|사랑|sarang|αγάπη|agápi
dream|dream|sapnis|sapnas|unenägu|сон|сон|Traum|rêve|sueño|sen|uni|dröm|夢|ゆめ|梦|mèng|꿈|kkum|όνειρο|óneiro
heart|heart|sirds|širdis|süda|сердце|серце|Herz|cœur|corazón|serce|sydän|hjärta|心|こころ|心|xīn|마음|maeum|καρδιά|kardiá
hand|hand|roka|ranka|käsi|рука|рука|Hand|main|mano|ręka|käsi|hand|手|て|手|shǒu|손|son|χέρι|chéri
eye|eye|acs|akis|silm|глаз|око|Auge|œil|ojo|oko|silmä|öga|目|め|眼睛|yǎnjing|눈|nun|μάτι|máti
light|light|gaisma|šviesa|valgus|свет|світло|Licht|lumière|luz|światło|valo|ljus|光|ひかり|光|guāng|빛|bit|φως|fos
colour|colour|krāsa|spalva|värv|цвет|колір|Farbe|couleur|color|kolor|väri|färg|色|いろ|颜色|yánsè|색|saek|χρώμα|chróma
red|red|sarkans|raudona|punane|красный|червоний|rot|rouge|rojo|czerwony|punainen|röd|赤|あか|红色|hóngsè|빨강|ppalgang|κόκκινο|kókkino
blue|blue|zils|mėlyna|sinine|синий|синій|blau|bleu|azul|niebieski|sininen|blå|青|あお|蓝色|lánsè|파랑|parang|μπλε|ble
white|white|balts|balta|valge|белый|білий|weiß|blanc|blanco|biały|valkoinen|vit|白|しろ|白色|báisè|하양|hayang|άσπρο|áspro
spring|spring|pavasaris|pavasaris|kevad|весна|весна|Frühling|printemps|primavera|wiosna|kevät|vår|春|はる|春天|chūntiān|봄|bom|άνοιξη|ánoixi
summer|summer|vasara|vasara|suvi|лето|літо|Sommer|été|verano|lato|kesä|sommar|夏|なつ|夏天|xiàtiān|여름|yeoreum|καλοκαίρι|kalokaíri
autumn|autumn|rudens|ruduo|sügis|осень|осінь|Herbst|automne|otoño|jesień|syksy|höst|秋|あき|秋天|qiūtiān|가을|gaeul|φθινόπωρο|fthinóporo
winter|winter|ziema|žiema|talv|зима|зима|Winter|hiver|invierno|zima|talvi|vinter|冬|ふゆ|冬天|dōngtiān|겨울|gyeoul|χειμώνας|cheimónas
thanks|thank you|paldies|ačiū|aitäh|спасибо|дякую|danke|merci|gracias|dziękuję|kiitos|tack|ありがとう|ありがとう|谢谢|xièxie|고마워요|gomawoyo|ευχαριστώ|efcharistó
hello|hello|sveiki|labas|tere|здравствуйте|привіт|hallo|bonjour|hola|cześć|hei|hej|こんにちは|こんにちは|你好|nǐ hǎo|안녕하세요|annyeonghaseyo|γεια σου|geia sou
beautiful|beautiful|skaists|gražus|ilus|красивый|гарний|schön|beau|hermoso|piękny|kaunis|vacker|美しい|うつくしい|美丽|měilì|아름답다|areumdapda|όμορφος|ómorfos
quiet|quiet|kluss|tylus|vaikne|тихий|тихий|ruhig|calme|tranquilo|cichy|hiljainen|tyst|静か|しずか|安静|ānjìng|조용하다|joyonghada|ήσυχος|ísychos
"""

    val all: List<VocabWord> = TABLE.trim().lines().map { line ->
        val f = line.split('|')
        val words = LANGS.mapIndexed { i, l -> l to f[1 + i] }.toMap() +
            mapOf("ja" to f[13], "zh" to f[15], "ko" to f[17], "el" to f[19])
        VocabWord(f[0], words, mapOf("ja" to f[14], "zh" to f[16], "ko" to f[18], "el" to f[20]))
    }

    /**
     * The word for [day]: a fixed scrambled walk through the whole list (a step
     * coprime to its size), so every word comes up once before any repeats.
     * [extra] moves on to the following words, for a tap asking for another.
     */
    fun ofDay(day: LocalDate, extra: Int = 0): VocabWord {
        val n = all.size
        val step = generateSequence(37) { it + 1 }.first { gcd(it, n) == 1 }
        return all[Math.floorMod((day.toEpochDay() + extra) * step, n.toLong()).toInt()]
    }

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
