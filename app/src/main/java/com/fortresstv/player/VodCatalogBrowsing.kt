package com.fortresstv.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortresstv.player.omdb.OmdbMetadata
import com.fortresstv.player.omdb.OmdbMetadataResolver
import com.fortresstv.player.omdb.OmdbPreFetchTarget
import com.fortresstv.player.omdb.OmdbSettings
import com.fortresstv.player.profile.ParentalControls
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.tvFocusHighlight
import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamCategory

internal data class VodCatalogFilterResult<R>(
    val items: List<R>,
    val topRatedCount: Int,
    val freshCount: Int,
    val ratedCount: Int = 0,
    val totalCount: Int = items.size
) {
    val ratingCoverage: Float
        get() = if (totalCount <= 0) 1f else ratedCount.toFloat() / totalCount.toFloat()
}

internal const val MIN_RATING_COVERAGE_THRESHOLD = 0.35f

internal fun parseUsableRating(raw: String?): Double? {
    val value = raw?.trim()?.toDoubleOrNull() ?: return null
    if (value.isNaN() || value <= 0.0 || value > 10.0) return null
    return value
}

internal fun resolveEffectiveRating(cachedImdbRating: String?, providerRating: String?): Double? =
    parseUsableRating(cachedImdbRating) ?: parseUsableRating(providerRating)

internal fun formatEffectiveRatingBadge(cachedImdbRating: String?, providerRating: String?): String? {
    if (parseUsableRating(cachedImdbRating) != null) return cachedImdbRating!!.trim()
    if (parseUsableRating(providerRating) != null) return providerRating!!.trim()
    return null
}

internal fun shouldRunLowCoverageEnrichment(
    ratedCount: Int,
    totalCount: Int,
    threshold: Float = MIN_RATING_COVERAGE_THRESHOLD
): Boolean = totalCount > 0 && (ratedCount.toFloat() / totalCount.toFloat()) < threshold

internal fun buildVisibleVodCategories(
    catalogs: List<XtreamCatalog>,
    hideAdultContent: Boolean,
    categoriesOf: (XtreamCatalog) -> List<XtreamCategory>
): List<CatalogCategory> = catalogs.flatMap { catalog ->
    categoriesOf(catalog)
        .filter { !hideAdultContent || !isAdultContent(it.name, null) }
        .map { CatalogCategory(catalog.sourceId, catalog.sourceName, it) }
}.distinctBy { it.sourceId to it.category.id }

internal fun <T, R> filterAndSortVodItems(
    catalogs: List<XtreamCatalog>,
    selectedCategory: CatalogCategory?,
    appliedQuery: String,
    hideAdultContent: Boolean,
    maximumAge: Int?,
    blockUnrated: Boolean,
    sortOption: VodSortOption,
    filterTopRated: Boolean,
    filterFresh: Boolean,
    categoriesOf: (XtreamCatalog) -> List<XtreamCategory>,
    itemsOf: (XtreamCatalog) -> List<T>,
    wrap: (XtreamCatalog, T) -> R,
    categoryIdOf: (T) -> String?,
    nameOf: (T) -> String,
    yearOf: (T) -> String?,
    ageRatingOf: (T) -> String?,
    providerRatingOf: (T) -> String?,
    cachedOmdbOf: (T) -> OmdbMetadata?,
    transformFiltered: (List<R>) -> List<R> = { it },
    unwrap: (R) -> T
): VodCatalogFilterResult<R> {
    val baseFiltered = catalogs.flatMap { catalog ->
        val categoryNames = categoriesOf(catalog).associate { it.id to it.name }
        itemsOf(catalog).asSequence()
            .filter { item ->
                selectedCategory == null ||
                    (catalog.sourceId == selectedCategory.sourceId && categoryIdOf(item) == selectedCategory.category.id)
            }
            .filter { item ->
                (!hideAdultContent || !isAdultContent(nameOf(item), categoryNames[categoryIdOf(item)])) &&
                    ParentalControls.isContentAllowedForAge(ageRatingOf(item), maximumAge, blockUnrated)
            }
            .filter { item -> appliedQuery.isBlank() || nameOf(item).contains(appliedQuery, ignoreCase = true) }
            .map { wrap(catalog, it) }
            .toList()
    }.let(transformFiltered)

    val scored = baseFiltered.map { wrapped ->
        val item = unwrap(wrapped)
        val cached = cachedOmdbOf(item)
        val imdbScore = resolveEffectiveRating(cached?.imdbRating, providerRatingOf(item))
        val rtScore = cached?.rottenTomatoes?.removeSuffix("%")?.trim()?.toIntOrNull() ?: 0
        ScoredVodItem(item, wrapped, imdbScore, rtScore)
    }

    val ratedCount = scored.count { it.imdbScore != null }
    val topRatedCount = scored.count { (it.imdbScore ?: 0.0) >= 7.5 }
    val freshCount = scored.count { it.rtScore >= 60 }

    val sorted = scored
        .filter { (!filterTopRated || (it.imdbScore ?: 0.0) >= 7.5) && (!filterFresh || it.rtScore >= 60) }
        .let { list ->
            when (sortOption) {
                VodSortOption.DEFAULT -> list.map { it.wrapped }
                VodSortOption.RATING -> list.sortedWith(
                    compareByDescending<ScoredVodItem<T, R>> { it.imdbScore != null }
                        .thenByDescending { it.imdbScore ?: 0.0 }
                        .thenBy { nameOf(it.item).lowercase() }
                ).map { it.wrapped }
                VodSortOption.YEAR -> list.sortedByDescending { yearOf(it.item)?.toIntOrNull() ?: 0 }.map { it.wrapped }
                VodSortOption.ALPHABETICAL -> list.sortedBy { nameOf(it.item).lowercase() }.map { it.wrapped }
            }
        }

    return VodCatalogFilterResult(
        items = sorted,
        topRatedCount = topRatedCount,
        freshCount = freshCount,
        ratedCount = ratedCount,
        totalCount = scored.size
    )
}

private data class ScoredVodItem<T, R>(
    val item: T,
    val wrapped: R,
    val imdbScore: Double?,
    val rtScore: Int
)

@Composable
internal fun <T> rememberVodOmdbPrefetchVersion(
    gridState: LazyGridState,
    visibleItems: List<T>,
    omdbSettings: OmdbSettings,
    paused: Boolean,
    targetOf: (T) -> OmdbPreFetchTarget
): Int {
    val context = LocalContext.current
    var ratingCacheVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(gridState, visibleItems, omdbSettings, paused) {
        ratingCacheVersion++
        if (!omdbSettings.enabled || omdbSettings.apiKey.isBlank() || paused) return@LaunchedEffect
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
                visibleItems.getOrNull(info.index)?.let(targetOf)
            }
        }.collect { targets ->
            OmdbMetadataResolver.prefetchCuratedBatch(context, omdbSettings, targets)
            ratingCacheVersion++
        }
    }
    return ratingCacheVersion
}

@Composable
internal fun <T> rememberLowCoverageRatingEnrichment(
    catalogs: List<XtreamCatalog>,
    visibleItems: List<T>,
    ratedCount: Int,
    totalCount: Int,
    omdbSettings: OmdbSettings,
    gridState: LazyGridState,
    sourceIdOf: (T) -> String,
    ratingOf: (T) -> String?,
    targetOf: (T) -> OmdbPreFetchTarget
): Int {
    val context = LocalContext.current
    var enrichmentRefreshToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(catalogs, omdbSettings, totalCount) {
        if (!omdbSettings.enabled || omdbSettings.apiKey.isBlank()) return@LaunchedEffect
        if (!shouldRunLowCoverageEnrichment(ratedCount, totalCount)) return@LaunchedEffect

        val targetsBySource = LinkedHashMap<String, MutableList<OmdbPreFetchTarget>>()
        for (item in visibleItems) {
            if (parseUsableRating(ratingOf(item)) != null) continue
            val sourceId = sourceIdOf(item)
            val list = targetsBySource.getOrPut(sourceId) { mutableListOf() }
            if (list.size < OmdbMetadataResolver.DEFAULT_PER_SOURCE_ENRICHMENT_BUDGET * 2) {
                list.add(targetOf(item))
            }
        }
        if (targetsBySource.isEmpty()) return@LaunchedEffect

        val newlyResolved = OmdbMetadataResolver.prefetchWithPerSourceBudget(
            context = context,
            settings = omdbSettings,
            targetsBySource = targetsBySource,
            perSourceBudget = OmdbMetadataResolver.DEFAULT_PER_SOURCE_ENRICHMENT_BUDGET
        )
        if (newlyResolved > 0 && !gridState.isScrollInProgress && gridState.firstVisibleItemIndex == 0) {
            enrichmentRefreshToken++
        }
    }
    return enrichmentRefreshToken
}

@Composable
internal fun VodRatingFilterChipsRow(
    filterTopRated: Boolean,
    topRatedCount: Int,
    onToggleTopRated: () -> Unit,
    filterFresh: Boolean,
    freshCount: Int,
    onToggleFresh: () -> Unit
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onToggleTopRated,
            shape = RoundedCornerShape(8.dp),
            color = if (filterTopRated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, if (filterTopRated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.tvFocusHighlight(shape = RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (filterTopRated) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFFFFB800),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${strings.topRatedFilter} · $topRatedCount",
                    color = if (filterTopRated) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = if (filterTopRated) FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        if (freshCount > 0 || filterFresh) {
            Surface(
                onClick = onToggleFresh,
                shape = RoundedCornerShape(8.dp),
                color = if (filterFresh) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (filterFresh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.tvFocusHighlight(shape = RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🍅", fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${strings.freshFilter} · $freshCount",
                        color = if (filterFresh) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = if (filterFresh) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
