package com.example.tanglishpro.core

/** Small, deterministic offline phrase translator. Unknown words are kept as typed. */
object OfflineTranslator {
    private val patterns = listOf(
        Regex("^(?:naan)\\s+(?:veetukku|veetuku)\\s+(?:poren|pogiren)$") to "I am going home.",
        Regex("^(?:naan)\\s+(.+?)\\s+(?:ku|kku)\\s+(?:poren|pogiren|poaren)$") to "I am going to %s.",
        Regex("^(?:naan)\\s+(?:sapda|saapida)\\s+(?:poren|pogiren)$") to "I am going to eat.",
        Regex("^(?:naan)\\s+(.+?)\\s+(?:sapdren|saapduren|saapiduven)$") to "I am eating %s.",
        Regex("^(?:naan)\\s+(.+?)\\s+(?:padikuren|padikkiren)$") to "I am studying %s.",
        Regex("^(?:naan)\\s+(.+?)\\s+(?:paakuren|paakkiren)$") to "I am watching %s.",
        Regex("^(?:naan)\\s+(.+?)\\s+(?:panren|pannuren)$") to "I am doing %s.",
        Regex("^(?:naan)\\s+(.+?)\\s+(?:venum|vendum)$") to "I want %s.",
        Regex("^(?:enakku)\\s+(.+?)\\s+(?:venum|vendum)$") to "I need %s.",
        Regex("^(?:naan)\\s+(?:nalla\\s+)?(?:iruken|irukken)$") to "I am doing well.",
        Regex("^(?:naan)\\s+(?:varren|varen)$") to "I am coming.",
        Regex("^(?:naan)\\s+(?:varuven)$") to "I will come.",
        Regex("^(?:naan)\\s+(?:vandhuten|vanthuten)$") to "I have arrived.",
        Regex("^(?:nee|neenga)\\s+(?:enna)\\s+(?:panra|panre|panreenga)$") to "What are you doing?",
        Regex("^(?:nee|neenga)\\s+(?:epdi|eppadi)\\s+(?:iruka|irukeenga|irukkinga)$") to "How are you?",
        Regex("^(?:nee)\\s+(?:enga)\\s+(?:pora|porai)$") to "Where are you going?",
        Regex("^(?:nee)\\s+(?:saptiya|saaptiya)$") to "Have you eaten?"
    )

    fun translate(input: String): String {
        val cleaned = input.trim().replace(Regex("\\s+"), " ")
            .trimEnd('.', '!', '?')
        if (cleaned.isBlank()) return ""
        val normalized = cleaned.lowercase()
        for ((pattern, template) in patterns) {
            val match = pattern.matchEntire(normalized) ?: continue
            val translated = if (template.contains("%s")) {
                val noun = match.groupValues[1].replaceFirstChar { it.uppercase() }
                String.format(template, noun.replaceFirstChar { it.lowercase() })
            } else template
            return translated
        }
        val exact = mapOf(
            "nandri" to "Thank you.",
            "romba nandri" to "Thank you very much.",
            "seri" to "Okay.",
            "aama" to "Yes.",
            "illa" to "No.",
            "mannichidu" to "I am sorry.",
            "enakku puriyala" to "I do not understand."
        )[normalized]
        if (exact != null) return exact
        return cleaned.replaceFirstChar { it.uppercase() } + "."
    }
}
