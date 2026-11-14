package com.fortresstv.player

import com.fortresstv.player.omdb.OmdbMetadata
import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamCategory
import com.fortresstv.player.xtream.XtreamSeries
import org.junit.Assert.assertEquals
import org.junit.Test

class VodCatalogBrowsingTest {

    @Test
    fun filterAndSortVodItemsAppliesCategoryParentalQueryAndRatingSort() {
        val cats = listOf(XtreamCategory("1", "Drama"), XtreamCategory("2", "XXX Adult"))
        val cat = XtreamCatalog(
            sourceId = "src",
            sourceName = "Src",
            seriesCategories = cats,
            series = listOf(
                XtreamSeries(1, "Alpha", "1", null, "6.0", "2020", null, ageRating = "TV-14"),
                XtreamSeries(2, "Beta", "1", null, "0", "2023", null, ageRating = "TV-PG"),
                XtreamSeries(3, "Adult", "2", null, "9.0", "2022", null, ageRating = "TV-MA")
            )
        )

        val visibleCats = buildVisibleVodCategories(listOf(cat), hideAdultContent = true) { it.seriesCategories }
        assertEquals(listOf("1"), visibleCats.map { it.category.id })

        val omdbMap = mapOf(
            2 to OmdbMetadata(imdbRating = "8.4", rottenTomatoes = "92%")
        )
        val result = filterAndSortVodItems(
            catalogs = listOf(cat),
            selectedCategory = null,
            appliedQuery = "",
            hideAdultContent = true,
            maximumAge = 16,
            blockUnrated = false,
            sortOption = VodSortOption.RATING,
            filterTopRated = false,
            filterFresh = false,
            categoriesOf = { it.seriesCategories },
            itemsOf = { it.series },
            wrap = ::CatalogSeries,
            categoryIdOf = { it.categoryId },
            nameOf = { it.name },
            yearOf = { it.year },
            ageRatingOf = { it.ageRating },
            providerRatingOf = { it.rating },
            cachedOmdbOf = { omdbMap[it.id] },
            unwrap = { it.series }
        )

        assertEquals(1, result.topRatedCount)
        assertEquals(1, result.freshCount)
        assertEquals(listOf("Beta", "Alpha"), result.items.map { it.series.name })
    }
}
