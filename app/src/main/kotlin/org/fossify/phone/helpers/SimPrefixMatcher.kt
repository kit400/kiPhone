package org.fossify.phone.helpers

/**
 * Picks a SIM for an outgoing number by operator prefixes (e.g. Kyivstar: 067, 068, 096...).
 * Pure Kotlin, no Android dependencies.
 *
 * Numbers are compared in the national format: +380 67 123 45 67, 380671234567,
 * 80671234567 and 0671234567 all become 0671234567.
 */
object SimPrefixMatcher {
    private const val COUNTRY_CODE = "380"

    /** "039, 067;+38068 096" -> [039, 067, 068, 096] */
    fun parsePrefixes(raw: String): List<String> {
        return raw.split(',', ';', ' ', '\n', '\t')
            .map { toNational(it.filter(Char::isDigit)) }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    /** Returns null for service codes (USSD *100#, etc.) that must not be matched by prefixes. */
    fun normalizeNumber(number: String): String? {
        val cleaned = number.removePrefix("tel:").trim()
        if (cleaned.isEmpty() || cleaned.any { it == '*' || it == '#' }) {
            return null
        }

        val digits = cleaned.filter(Char::isDigit)
        return when {
            digits.startsWith(COUNTRY_CODE) && digits.length == 12 -> digits.substring(2)
            digits.startsWith("80") && digits.length == 11 -> digits.substring(1)
            else -> digits
        }.ifEmpty { null }
    }

    /**
     * @param prefixesBySim SIM id -> its prefixes
     * @return id of the SIM with the longest matching prefix, or null if nothing matches
     */
    fun findSim(number: String, prefixesBySim: Map<String, List<String>>): String? {
        val normalized = normalizeNumber(number) ?: return null
        return prefixesBySim
            .flatMap { (simId, prefixes) -> prefixes.map { simId to it } }
            .filter { (_, prefix) -> normalized.startsWith(prefix) }
            .maxByOrNull { (_, prefix) -> prefix.length }
            ?.first
    }

    private fun toNational(prefix: String): String {
        return if (prefix.startsWith(COUNTRY_CODE)) prefix.substring(2) else prefix
    }
}
