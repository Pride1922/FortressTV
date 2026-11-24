package com.fortresstv.player

import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.library.UserLibraryStore

internal fun UserLibraryStore.scopedHiddenCategories(sourceIds: List<String>): Set<String> =
    sourceIds.flatMap { source -> hiddenCategories(source).map { sourceCategoryKey(source, it) } }.toSet()

internal fun UserLibraryStore.scopedCategoryOrder(sourceIds: List<String>): List<String> =
    sourceIds.flatMap { source -> categoryOrder(source).map { sourceCategoryKey(source, it) } }

/** UI identity only; storage and cloud payloads retain the original source-local name. */
internal fun sourceCategoryKey(sourceId: String, name: String): String =
    "category@${sourceId.length}:$sourceId$name"

internal fun sourceCategoryParts(key: String): Pair<String, String>? {
    if (!key.startsWith("category@")) return null
    val separator = key.indexOf(':', 9)
    if (separator < 0) return null
    val length = key.substring(9, separator).toIntOrNull() ?: return null
    val start = separator + 1
    if (length < 0 || length > key.length - start) return null
    return key.substring(start, start + length) to key.substring(start + length)
}

internal fun sourceCategoryName(key: String): String = sourceCategoryParts(key)?.second ?: key
internal fun PlaylistChannel.categoryKey(): String =
    if (group.isNullOrBlank()) "" else sourceCategoryKey(sourceId, group)

internal fun PlaylistChannel.hasHiddenCategory(hidden: Set<String>): Boolean =
    categoryKey() in hidden || group.orEmpty() in hidden

internal fun orderedSourceCategories(categories: List<String>, order: List<String>): List<String> {
    val sourceOrder = categories.map { sourceCategoryParts(it)?.first }.distinct().withIndex().associate { it.value to it.index }
    val orderIndex = order.withIndex().associate { it.value to it.index }
    return categories.sortedWith(compareBy({ sourceOrder[sourceCategoryParts(it)?.first] ?: 0 }, { orderIndex[it] ?: Int.MAX_VALUE }))
}
