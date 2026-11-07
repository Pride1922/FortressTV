package com.fortresstv.player.profile

import org.json.JSONObject
import java.util.UUID

enum class ProfileKind {
    ADULT, CHILD;

    val persistedValue: String
        get() = name.lowercase()

    companion object {
        fun fromPersisted(value: String?): ProfileKind = when (value?.lowercase()?.trim()) {
            "child" -> CHILD
            else -> ADULT
        }
    }
}

data class HouseholdProfile(
    val id: String = UUID.randomUUID().toString(),
    val householdId: String? = null,
    val name: String,
    val kind: ProfileKind = ProfileKind.ADULT,
    val language: String = "en",
    val hideAdultContent: Boolean = kind == ProfileKind.CHILD,
    val maximumAge: Int? = if (kind == ProfileKind.CHILD) 12 else null,
    val blockUnrated: Boolean = kind == ProfileKind.CHILD,
    val avatarUrl: String? = null
) {
    init {
        require(name.trim().isNotEmpty() && name.trim().length <= 60) {
            "Profile name must be between 1 and 60 characters"
        }
        if (kind == ProfileKind.CHILD) {
            require(hideAdultContent) { "Child profiles must hide adult content" }
            require(blockUnrated) { "Child profiles must block unrated content" }
            require(maximumAge != null && maximumAge in 0..18) {
                "Child profiles must have a maximum age between 0 and 18"
            }
        }
    }

    val isChild: Boolean get() = kind == ProfileKind.CHILD
    val isAdult: Boolean get() = kind == ProfileKind.ADULT

    fun encode(): JSONObject = JSONObject().apply {
        put("id", id)
        householdId?.let { put("household_id", it) }
        put("name", name)
        put("kind", kind.persistedValue)
        put("language", language)
        put("hide_adult_content", hideAdultContent)
        if (maximumAge != null) put("maximum_age", maximumAge) else put("maximum_age", JSONObject.NULL)
        put("block_unrated", blockUnrated)
        if (avatarUrl != null) put("avatar_url", avatarUrl) else put("avatar_url", JSONObject.NULL)
    }

    companion object {
        fun decode(json: JSONObject): HouseholdProfile {
            val kind = ProfileKind.fromPersisted(json.optString("kind", "adult"))
            val maxAge = if (json.isNull("maximum_age")) null else json.optInt("maximum_age").takeIf { it in 0..18 }
            val avatar = json.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" }
            return HouseholdProfile(
                id = json.getString("id"),
                householdId = json.optString("household_id").takeIf(String::isNotBlank),
                name = json.getString("name"),
                kind = kind,
                language = json.optString("language", "en").takeIf { it in listOf("en", "pt-PT", "pt") } ?: "en",
                hideAdultContent = if (kind == ProfileKind.CHILD) true else json.optBoolean("hide_adult_content", false),
                maximumAge = if (kind == ProfileKind.CHILD) (maxAge ?: 12) else maxAge,
                blockUnrated = if (kind == ProfileKind.CHILD) true else json.optBoolean("block_unrated", false),
                avatarUrl = avatar
            )
        }

        fun defaultAdult(name: String = "Primary", language: String = "en"): HouseholdProfile = HouseholdProfile(
            name = name,
            kind = ProfileKind.ADULT,
            language = if (language.startsWith("pt")) "pt-PT" else "en",
            hideAdultContent = false,
            maximumAge = null,
            blockUnrated = false
        )

        fun restrictedDefault(language: String = "en"): HouseholdProfile = HouseholdProfile(
            id = "restricted_default",
            name = if (language.startsWith("pt")) "Restrito" else "Restricted",
            kind = ProfileKind.CHILD,
            language = if (language.startsWith("pt")) "pt-PT" else "en",
            hideAdultContent = true,
            maximumAge = 6,
            blockUnrated = true
        )
    }
}
