package com.basem.qiyam

data class Reminder(val arabic: String, val english: String, val source: String)

/**
 * Short reminders about the night prayer, rotated night by night.
 * English renderings are brief paraphrases written for this app.
 */
object Reminders {
    val all = listOf(
        Reminder(
            "يَنْزِلُ رَبُّنَا تَبَارَكَ وَتَعَالَى كُلَّ لَيْلَةٍ إِلَى السَّمَاءِ الدُّنْيَا حِينَ يَبْقَى ثُلُثُ اللَّيْلِ الآخِرُ، يَقُولُ: مَنْ يَدْعُونِي فَأَسْتَجِيبَ لَهُ، مَنْ يَسْأَلُنِي فَأُعْطِيَهُ، مَنْ يَسْتَغْفِرُنِي فَأَغْفِرَ لَهُ",
            "Every night, when the last third remains, our Lord descends to the lowest heaven and says: who will call on Me so I answer, who will ask of Me so I give, who will seek My forgiveness so I forgive?",
            "Bukhari 1145 · Muslim 758",
        ),
        Reminder(
            "وَمِنَ اللَّيْلِ فَتَهَجَّدْ بِهِ نَافِلَةً لَّكَ عَسَىٰ أَن يَبْعَثَكَ رَبُّكَ مَقَامًا مَّحْمُودًا",
            "And in part of the night, rise and pray as an extra devotion for you. It may be that your Lord raises you to a praised station.",
            "Al-Isra 17:79",
        ),
        Reminder(
            "كَانُوا قَلِيلًا مِّنَ اللَّيْلِ مَا يَهْجَعُونَ ۝ وَبِالْأَسْحَارِ هُمْ يَسْتَغْفِرُونَ",
            "They used to sleep only a little of the night, and in the hours before dawn they would seek forgiveness.",
            "Adh-Dhariyat 51:17–18",
        ),
        Reminder(
            "تَتَجَافَىٰ جُنُوبُهُمْ عَنِ الْمَضَاجِعِ يَدْعُونَ رَبَّهُمْ خَوْفًا وَطَمَعًا",
            "Their sides leave their beds as they call on their Lord in fear and in hope.",
            "As-Sajdah 32:16",
        ),
        Reminder(
            "إِنَّ نَاشِئَةَ اللَّيْلِ هِيَ أَشَدُّ وَطْئًا وَأَقْوَمُ قِيلًا",
            "Rising in the night is firmer for the soul and more upright in speech.",
            "Al-Muzzammil 73:6",
        ),
        Reminder(
            "وَالْمُسْتَغْفِرِينَ بِالْأَسْحَارِ",
            "…and those who seek forgiveness in the hours before dawn.",
            "Aal-Imran 3:17",
        ),
        Reminder(
            "أَفْضَلُ الصَّلَاةِ بَعْدَ الْفَرِيضَةِ صَلَاةُ اللَّيْلِ",
            "The best prayer after the obligatory prayers is the prayer at night.",
            "Muslim 1163",
        ),
    )

    /** Said on waking (Bukhari 6312). Shown on the alarm screen every night. */
    val wakingDua = Reminder(
        "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
        "Praise be to Allah, who gave us life after causing us to die, and to Him is the return.",
        "Bukhari 6312",
    )
}
