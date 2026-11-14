package com.fortresstv.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    ENGLISH("en", "English", "🇬🇧"),
    PORTUGUESE("pt", "Português (Portugal)", "🇵🇹");

    companion object {
        fun fromCode(code: String): AppLanguage {
            val normalized = code.lowercase()
            return when {
                normalized.startsWith("pt") -> PORTUGUESE
                else -> ENGLISH
            }
        }

        fun fromSystemLocale(): AppLanguage {
            val systemLang = java.util.Locale.getDefault().language.lowercase()
            return if (systemLang.startsWith("pt")) PORTUGUESE else ENGLISH
        }
    }
}

interface Strings {
    val languageTag: String
    val appName: String
    val allChannels: String
    val sources: String
    val categories: String
    val uncategorized: String
    val navHome: String
    val navLiveTv: String
    val navMovies: String
    val navSeries: String
    val navFavorites: String
    val homeContinueWatching: String
    val homeNextUp: String
    val homeFavoriteChannelsNow: String
    val homeRecentChannels: String
    val homeUnifiedSearchPlaceholder: String
    val homeChannelsCategory: String
    val homeMoviesCategory: String
    val homeSeriesCategory: String
    val homeFeaturedMovies: String
    val homePopularOnTraktMovies: String
    val homeFeaturedSeries: String
    val homePopularOnTraktSeries: String
    val homeRecentlyAddedSeries: String
    val homeNothingFound: String
    val homeStartupScreen: String
    val homeStartupScreenHome: String
    val homeStartupScreenLiveTv: String
    val homeNoContent: String
    val quickZap: String
    val quickZapFavorites: String
    val quickZapRecent: String
    val dockedPlayer: String
    val closeDockedPlayer: String
    val comingSoon: String
    val moviesComingSoon: String
    val seriesComingSoon: String
    val noFavoriteChannels: String
    val addFavorite: String
    val removeFavorite: String
    val favoriteChannels: String
    val recentlyWatched: String
    val noRecentlyWatched: String
    val movieWatchlist: String
    val followedSeries: String
    val addToWatchlist: String
    val removeFromWatchlist: String
    val followSeries: String
    val unfollowSeries: String
    val noFavorites: String
    val noWatchlistMovies: String
    val noFollowedSeries: String
    val noMoviesAvailable: String
    val noSeriesAvailable: String
    val allCategories: String
    val searchMovies: String
    val searchSeries: String
    val loadingEpisodes: String
    val loadingMovieDetails: String
    val movieDetailsUnavailable: String
    val loadingSeriesDetails: String
    val seriesDetailsUnavailable: String
    val genre: String
    val duration: String
    val releaseDate: String
    val director: String
    val cast: String
    val country: String
    val ageRating: String
    val noEpisodesAvailable: String
    fun seasonLabel(season: Int): String
    fun seasonsCount(count: Int): String
    fun episodeLabel(season: Int, episode: Int): String
    val markWatched: String
    val markUnwatched: String
    val watched: String
    val unwatched: String
    fun playNextEpisode(season: Int, episode: Int): String
    val allEpisodesWatched: String
    val allAvailableEpisodesWatched: String
    val followedFilter: String
    val watchlistFilter: String
    val restartSeries: String
    fun unwatchedEpisodesCount(count: Int): String
    fun seasonUnwatchedCount(count: Int): String
    val nextUpBadge: String
    val fastScrollJump: String
    val jumpToLetter: String
    fun searchInGroup(group: String): String
    val searchChannels: String
    val noChannelsMatch: String
    fun channelsCount(count: Int): String
    val settings: String
    val close: String
    val clearSearch: String
    val clearRecentChannels: String
    val save: String
    val cancel: String
    val delete: String
    val retry: String
    val refresh: String
    val edit: String
    val editPlaylist: String
    val rename: String
    val active: String
    val enabledSource: String
    fun sourcesCount(count: Int): String
    val moveSourceUp: String
    val moveSourceDown: String
    val switchAction: String
    val back: String
    val fullscreen: String

    val collapseSidebar: String
    val expandSidebar: String
    val showChannelFilters: String
    val hideChannelFilters: String
    val previousChannel: String
    val manageCategories: String
    val changeCategory: String
    val selectCategory: String
    val searchEpg: String
    val liveBadge: String
    val hideCategory: String
    val showCategory: String
    val showAllCategories: String
    val hideAllCategories: String
    val resetCategoryOrder: String
    val search: String
    val watchedProgress: String
    val sortDefault: String
    val sortByRating: String
    val sortByYear: String
    val sortAlphabetical: String
    val switchPlaylist: String
    val epgNotConfigured: String
    fun epgMatched(matched: Int, total: Int): String
    val unmatchedEpgChannels: String
    val allEpgMatched: String
    val missingTvgId: String
    fun tvgIdNotFound(id: String): String
    fun unmatchedEpgCount(count: Int): String
    val viewUnmatchedChannels: String
    val saveChannelsList: String
    val shareChannelsList: String
    val channelsListSaved: String
    val copiedToClipboard: String
    val xmltvGuide: String
    val now: String
    val next: String
    val whatsNext: String
    fun endsAt(time: String): String
    fun until(time: String): String
    val noCurrentProgramme: String
    val noUpcomingProgramme: String
    val noEpgId: String
    val noEpgData: String
    val programmeGuide: String
    fun todayDate(dayOfMonth: Int): String
    val watchNow: String
    val watchLiveChannel: String
    val zoomInTimeline: String
    val zoomOutTimeline: String
    val channelList: String
    val miniGuide: String
    val aspectRatio: String
    val lockControls: String
    val unlockControls: String
    val brightness: String
    val volume: String
    val aspectFill: String
    val aspectZoom: String
    val aspectFit: String
    val playbackError: String
    val failoverBackupSwitched: String
    fun failoverSourceSwitched(sourceName: String): String
    val sportsHub: String
    val liveMatches: String
    val manageSports: String
    val manageSportsTitle: String
    val manageSportsDescription: String
    val noFollowedSportsMatches: String
    val sportsCategories: String
    val supportedCompetitions: String
    val sportFootball: String
    val sportRugby: String
    val sportMotorsport: String
    val sportBasketball: String
    val sportTennis: String
    val sportOther: String
    val watchLive: String
    val liveNow: String
    fun broadcastOn(channel: String): String
    fun alsoOn(channel: String): String
    val matchDetails: String
    val matchEvents: String
    val noEventsYet: String
    val matchNotStarted: String
    fun watchLiveOn(channel: String): String
    val topLeaders: String
    val channelNotDetected: String
    fun kickoffAt(time: String): String
    val customizeHome: String
    val customizeHomeDescription: String
    fun moveHomeSectionUp(section: String): String
    fun moveHomeSectionDown(section: String): String
    val sectionSports: String
    val sectionContinueWatching: String
    val sectionFavoriteChannels: String
    val sectionRecentChannels: String
    val sectionFollowedSeries: String
    val sectionRecentMovies: String
    val sectionFeaturedSeries: String
    val sectionTraktPopularMovies: String
    val sectionTraktPopularSeries: String
    val buildingPlaybackBuffer: String
    val reconnecting: String
    val resumePlayback: String
    val resumePlaybackDescription: String
    val resume: String
    val restartPlayback: String
    val continueWatching: String
    val continueWatchingUnavailable: String
    val removeFromContinueWatching: String
    val catchup: String
    val watchCatchup: String
    val nextEpisode: String
    val playNextEpisode: String
    val autoPlayNext: String
    val autoPlayNextDesc: String
    val sleepTimer: String
    val sleepTimerHint: String
    val sleepTimerOff: String
    val endOfEpisode: String
    val endOfProgramme: String
    fun sleepTimerSet(minutes: Int): String
    fun nextEpisodeIn(seconds: Int): String
    val playNow: String
    val sleepTimerExpired: String
    val areYouStillWatching: String
    fun stillWatchingCountdown(seconds: Int): String
    val stopPlayback: String
    val setAnotherTimer: String
    val continueWithoutTimer: String
    val playNextThenStop: String
    val audioAndSubtitles: String
    val audioTracks: String
    val subtitles: String
    val subtitlesOff: String
    val noSubtitlesAvailable: String
    val noAudioTracksAvailable: String
    val episodesDrawer: String
    fun seasonNumber(season: Int): String
    val nowPlayingBadge: String

    val videoQuality: String
    val qualityAuto: String
    val qualityAdaptive: String
    val goLive: String
    fun liveBehind(seconds: Long): String
    val liveSynchronized: String
    val streamResilience: String

    val audioAndSubtitlesDefaults: String
    val preferredAudioLanguage: String
    val preferredSubtitleLanguage: String
    val subtitleFallbackEnglish: String
    val subtitleFallbackEnglishDesc: String
    val defaultMediaAudio: String
    val langEnglish: String
    val langPortuguese: String
    val langSpanish: String
    val langFrench: String

    val openSubtitles: String
    val openSubtitlesEnabled: String
    val openSubtitlesApiKey: String
    val openSubtitlesUsername: String
    val openSubtitlesPassword: String
    val openSubtitlesAutoSearch: String
    val openSubtitlesAutoSearchDesc: String
    val openSubtitlesPreferPtPt: String
    val openSubtitlesPreferPtPtDesc: String
    val searchOnlineSubtitles: String
    val searchingSubtitles: String
    val noSubtitlesFound: String
    val downloadingSubtitle: String
    val subtitleDownloaded: String
    val openSubtitlesTestConnection: String
    val openSubtitlesConnectionSuccess: String
    val openSubtitlesConnectionFailed: String
    val openSubtitlesAccountCredentials: String
    val openSubtitlesApiKeyRequired: String
    val openSubtitlesApiKeyHint: String
    val langGerman: String
    val langItalian: String

    val welcomeTitle: String
    val welcomeSubtitle: String
    val playlistM3u: String
    val enterM3uUrl: String
    val orSelectLocalFile: String
    val browseLocalFile: String
    val epgGuideXmltv: String
    val enterXmltvUrl: String
    val loadPlaylist: String
    val loadingPlaylist: String
    val loadingPlaylistAndSettings: String
    fun welcomeUser(name: String): String
    val loadingSavedData: String
    val restoringSecureSession: String
    val selectM3uError: String

    val settingsTitle: String
    val tabAccount: String
    val tabProfiles: String
    val profiles: String
    val switchProfile: String
    val libraryProfileMenu: String
    val closeApp: String
    val manageProfiles: String
    val childProfile: String
    val adultProfile: String
    val pinProtected: String
    val importLocalDataTitle: String
    val importLocalDataMessage: String
    val importLocalDataConfirm: String
    val importLocalDataDismiss: String
    val tabPlaylists: String
    val tabEpg: String
    val tabGeneral: String
    val tabSubtitles: String
    val tabApiKeys: String
    val tmdbHeader: String
    val tmdbEnabled: String
    val tmdbApiKey: String
    val tmdbApiKeyHint: String
    val tmdbLanguage: String
    val tmdbLanguageDesc: String
    val tmdbLanguageAuto: String
    val tmdbTestConnection: String
    val tmdbConnectionSuccess: String
    val tmdbConnectionFailed: String
    val tmdbInfoHelp: String
    val tmdbInfoLink: String
    val omdbHeader: String
    val omdbEnabled: String
    val omdbApiKey: String
    val omdbApiKeyHint: String
    val omdbTestConnection: String
    val omdbConnectionSuccess: String
    val omdbConnectionFailed: String
    val omdbInfoHelp: String
    val omdbInfoLink: String
    val imdbRating: String
    val rottenTomatoes: String
    val metascore: String
    val awards: String
    val topRatedFilter: String
    val freshFilter: String
    val skipIntro: String
    val skipRecap: String
    val skipCredits: String
    val skipPreview: String
    val introDbHeader: String
    val introDbEnabled: String
    val introDbEnabledDesc: String
    val introDbAutoSkipIntro: String
    val introDbAutoSkipIntroDesc: String
    val introDbAutoSkipRecap: String
    val introDbAutoSkipRecapDesc: String
    val traktHeader: String
    val traktDesc: String
    val traktClientId: String
    val traktClientIdHint: String
    val traktClientIdLengthWarning: String
    val traktConnect: String
    val traktDisconnect: String
    val traktConnectedAs: String
    val apiServiceConnected: String
    val apiServiceDisconnected: String
    val apiServiceConnect: String
    val traktVipBadge: String
    val traktScrobble: String
    val traktScrobbleDesc: String
    val traktPairingTitle: String
    val traktPairingInstructions: String
    val traktPairingLink: String
    val traktPairingGenerating: String
    val traktPairingWaiting: String
    val traktPairingExpiresIn: String
    val traktPairingExpired: String
    val traktPairingDenied: String
    val traktPairingSuccess: String
    val traktPairingRetry: String
    val traktHelp: String
    val traktHelpLink: String
    val sectionTraktWatchlist: String
    val sectionTraktRecommendations: String
    val traktWatchlist: String
    val traktWatchlistEmpty: String
    val traktSyncWatchlist: String
    val traktSyncingWatchlist: String
    val traktWatchlistSynced: String
    val traktAddToWatchlist: String
    val traktRemoveFromWatchlist: String
    val traktWatchedHistory: String
    val traktSyncWatchedHistory: String
    val traktSyncingWatchedHistory: String
    val traktWatchedHistorySynced: String
    val traktWatchedHistoryEmpty: String
    val traktRecommendations: String
    val traktRecommendationsEmpty: String
    val traktIncludesTrending: String
    val traktSeriesRecommendationsEmpty: String
    val traktTrendingSeries: String
    val traktSyncRecommendations: String
    val traktSyncingRecommendations: String
    val traktRecommendationsSynced: String
    val traktHideRecommendation: String
    val traktHideRecommendationConfirmation: String
    val traktHiddenRecommendations: String
    val traktNoHiddenRecommendations: String
    val traktRestoreRecommendation: String
    val traktRecommendationActionFailed: String
    val traktHiddenRecommendationsFailed: String
    val traktLoadMoreHiddenRecommendations: String
    val traktHidingRecommendation: String
    val watchTrailer: String
    val openInYouTube: String
    val openTrailerExternally: String
    val trailerUnavailableInApp: String
    val trailerLinkOpenFailed: String
    val genres: String
    val tabAbout: String
    val language: String
    val playback: String
    val playbackMode: String
    val playbackModeDesc: String
    val playbackModeAuto: String
    val playbackModeStable: String
    val playbackModeDataSaver: String
    val playChannelsFullscreen: String
    val playChannelsFullscreenDesc: String
    val autoPictureInPicture: String
    val autoPictureInPictureDesc: String
    val anonymousTelemetry: String
    val anonymousTelemetryDesc: String
    val healthStable: String
    val healthFair: String
    val healthUnstable: String
    val healthTesting: String
    val connectionHealth: String
    val startupLatency: String
    val bufferStalls: String
    val testRegion: String
    val addNewPlaylist: String
    val addPlaylist: String
    val playlistName: String
    val playlistNamePlaceholder: String
    val sourceType: String
    val sourceRole: String
    val roleAutomatic: String
    val roleLiveTv: String
    val roleMovies: String
    val roleSeries: String
    val roleMoviesAndSeries: String
    val roleEverything: String
    val remoteUrl: String
    val localFile: String
    val xtreamCodes: String
    val xtreamServer: String
    val username: String
    val password: String
    val connect: String
    val selectFile: String
    val deletePlaylistConfirmTitle: String
    fun deletePlaylistConfirmMsg(name: String): String
    fun lastUpdated(time: String): String
    val updatedJustNow: String
    fun updatedMinutesAgo(minutes: Long): String
    fun updatedHoursAgo(hours: Long): String
    fun updatedAt(time: String): String
    val notUpdatedYet: String
    val refreshPlaylist: String
    val xmltvSource: String
    val noEpgConfiguredMsg: String
    val configureXmltv: String
    val changeXmltv: String
    val removeEpg: String
    val removeEpgConfirmation: String
    val battery: String
    val timeShift: String
    val timeShiftDesc: String
    val epgScheduleCorrection: String
    val epgScheduleCorrectionDesc: String
    fun epgPreview(baseHour: String, correctedHour: String): String
    val epgSourceTimezone: String
    val epgSourceTimezoneDesc: String
    val timezoneAutoDevice: String
    val directEntryMinutes: String
    val epgManualMapping: String
    val epgManualMappingDesc: String
    val mapEpgChannel: String
    val resetToAutomatic: String
    val mappingUnavailable: String
    val searchXmltvChannels: String
    val noXmltvChannelsFound: String
    val mappedTo: String
    val epgLoadedDays: String
    val epgLoadedDaysDesc: String
    fun epgDaysCount(days: Int): String
    val securityTitle: String
    val securityDesc: String
    val contentDisclaimerTitle: String
    val contentDisclaimer: String
    val openSourceSoftware: String
    val ffmpegAttribution: String
    val viewSourceCode: String
    val appVersion: String
    val noSourcesConfigured: String
    val addSourceGettingStarted: String
    fun versionLabel(version: String, code: Int): String
    fun installedVersion(version: String): String
    val directUrlPrivacyHint: String
    val checkForUpdates: String
    val checkingForUpdates: String
    val updateAvailable: String
    fun updateAvailableDesc(version: String): String
    val upToDate: String
    val downloadingUpdate: String
    val updateDownloaded: String
    val installUpdateHint: String
    val updateNow: String
    val installUpdate: String
    val later: String
    val hideAdultContent: String
    val hideAdultContentSubtitle: String
    val hideAdultContentChildRestricted: String
    val newUpdateAvailable: String
    fun updateFailed(error: String): String

    val parentalControl: String
    val parentalControlDesc: String
    val setParentalPin: String
    val changeParentalPin: String
    val removeParentalPin: String
    val enterParentalPin: String
    val confirmParentalPin: String
    val enterNewPin: String
    val pinIncorrect: String
    val pinsDoNotMatch: String
    val pinSetSuccess: String
    val pinRemovedSuccess: String
    val unlockAdultContent: String
    val unlockSettings: String
    val quickRecents: String

    val showPassword: String
    val transferScanWithApp: String
    val hidePassword: String

    // Local transfer (QR / phone to TV)
    val transferSendFromPhone: String
    val transferSendFromPhoneDesc: String
    val transferDialogTitle: String
    val transferScanQr: String
    val transferOrOpenUrl: String
    val transferPairingCode: String
    val transferSessionExpired: String
    val transferServerStartFailed: String
    val transferWaiting: String
    val transferReceived: String
    val transferConfirmTitle: String
    val transferConfirmDesc: String
    val transferSourceType: String
    val transferSourceServer: String
    val transferAddSource: String
    val transferSuccessMessage: String
    val transferLockedOut: String
    fun transferAttemptsRemaining(remaining: Int): String
    val transferNetworkError: String
    val transferCancelSession: String
    val transferSendToTv: String
    val transferSendToTvDesc: String
    val transferChooseSource: String
    val transferAvailableTvs: String
    val transferConnectedViaQr: String
    val transferSearchingTvs: String
    val transferEnterTvCode: String
    val transferEnterTvCodeDesc: String
    val transferSendNow: String
    val transferSending: String
    val transferSentToTv: String
    val transferConfirmOnTv: String
    val transferSendFailed: String
    val transferNoSendableSources: String
    val transferSourceTab: String
    val transferSourceExplanation: String
    val transferQrRequiredForSource: String
}

object EnglishStrings : Strings {
    override val languageTag = "en-GB"
    override val appName = "FortressTV"
    override val allChannels = "All channels"
    override val sources = "Sources"
    override val categories = "Categories"
    override val uncategorized = "Uncategorized"
    override val navHome = "Home"
    override val navLiveTv = "Live TV"
    override val navMovies = "Movies"
    override val navSeries = "Series"
    override val navFavorites = "Favorites"
    override val homeContinueWatching = "Continue Watching"
    override val homeNextUp = "Next Up"
    override val homeFavoriteChannelsNow = "Favorites On Now"
    override val homeRecentChannels = "Recently Watched Channels"
    override val homeUnifiedSearchPlaceholder = "Search channels, movies and series..."
    override val homeChannelsCategory = "Channels"
    override val homeMoviesCategory = "Movies"
    override val homeSeriesCategory = "Series"
    override val homeFeaturedMovies = "Recently Added Movies"
    override val homePopularOnTraktMovies = "Popular on Trakt"
    override val homeFeaturedSeries = "Featured Series"
    override val homePopularOnTraktSeries = "Popular on Trakt"
    override val homeRecentlyAddedSeries = "Recently Added Series"
    override val homeNothingFound = "No results found"
    override val homeStartupScreen = "Startup Screen"
    override val homeStartupScreenHome = "Home"
    override val homeStartupScreenLiveTv = "Live TV"
    override val homeNoContent = "Your media dashboard will appear here as you watch channels, movies, and series."
    override val quickZap = "Quick Zap"
    override val quickZapFavorites = "Favorites"
    override val quickZapRecent = "Recent"
    override val dockedPlayer = "Mini Player"
    override val closeDockedPlayer = "Close mini player"
    override val comingSoon = "Coming next"
    override val moviesComingSoon = "Your movie library will appear here when a VOD source is configured."
    override val seriesComingSoon = "Your series, seasons and episodes will appear here when a VOD source is configured."
    override val noFavoriteChannels = "Favorite channels will appear here."
    override val addFavorite = "Add to Favorites"
    override val removeFavorite = "Remove from Favorites"
    override val favoriteChannels = "Channels"
    override val recentlyWatched = "Recently Watched"
    override val noRecentlyWatched = "Recently watched channels will appear here."
    override val movieWatchlist = "Movies to watch"
    override val followedSeries = "Followed series"
    override val addToWatchlist = "Add to Watchlist"
    override val removeFromWatchlist = "Remove from Watchlist"
    override val followSeries = "Follow series"
    override val unfollowSeries = "Unfollow series"
    override val noFavorites = "Your favorite channels, movies and series will appear here."
    override val noWatchlistMovies = "Movies added to your Watchlist will appear here."
    override val noFollowedSeries = "Series you follow will appear here."
    override val noMoviesAvailable = "No Movies are available from the enabled Xtream sources."
    override val noSeriesAvailable = "No Series are available from the enabled Xtream sources."
    override val allCategories = "All"
    override val searchMovies = "Search movies"
    override val searchSeries = "Search series"
    override val loadingEpisodes = "Loading episodes..."
    override val loadingMovieDetails = "Loading movie details..."
    override val movieDetailsUnavailable = "More information could not be loaded."
    override val loadingSeriesDetails = "Loading series details and episodes..."
    override val seriesDetailsUnavailable = "Series information could not be loaded."
    override val genre = "Genre"
    override val duration = "Duration"
    override val releaseDate = "Release date"
    override val director = "Director"
    override val cast = "Cast"
    override val country = "Country"
    override val ageRating = "Age rating"
    override val noEpisodesAvailable = "No episodes are available for this Series."
    override fun seasonLabel(season: Int) = "Season $season"
    override fun seasonsCount(count: Int) = if (count == 1) "1 Season" else "$count Seasons"
    override fun episodeLabel(season: Int, episode: Int) = "S${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')}"
    override val markWatched = "Mark as watched"
    override val markUnwatched = "Mark as unwatched"
    override val watched = "Watched"
    override val unwatched = "Unwatched"
    override fun playNextEpisode(season: Int, episode: Int) = "Play Next (S${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')})"
    override val allEpisodesWatched = "All episodes watched"
    override val allAvailableEpisodesWatched = "All available episodes watched"
    override val followedFilter = "Followed"
    override val watchlistFilter = "Watchlist"
    override val restartSeries = "Restart Series"
    override fun unwatchedEpisodesCount(count: Int) = "$count ${if (count == 1) "unwatched" else "unwatched"}"
    override fun seasonUnwatchedCount(count: Int) = "$count ${if (count == 1) "unwatched" else "unwatched"}"
    override val nextUpBadge = "Next Up"
    override val fastScrollJump = "Fast scroll"
    override val jumpToLetter = "Jump to"
    override fun searchInGroup(group: String) = "Search in $group"
    override val searchChannels = "Search channels"
    override val noChannelsMatch = "No channels match your search."
    override fun channelsCount(count: Int) = "$count ${if (count == 1) "channel" else "channels"}"
    override val settings = "Settings"
    override val close = "Close"
    override val clearSearch = "Clear search"
    override val clearRecentChannels = "Clear recently watched channels"
    override val save = "Save"
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val retry = "Retry"
    override val refresh = "Refresh"
    override val edit = "Edit"
    override val editPlaylist = "Edit Playlist"
    override val rename = "Rename"
    override val active = "Active"
    override val enabledSource = "Enabled"
    override fun sourcesCount(count: Int) = "$count ${if (count == 1) "source" else "sources"}"
    override val moveSourceUp = "Move source up"
    override val moveSourceDown = "Move source down"
    override val switchAction = "Switch"
    override val back = "Back"
    override val fullscreen = "Fullscreen"

    override val collapseSidebar = "Collapse sidebar"
    override val expandSidebar = "Expand sidebar"
    override val showChannelFilters = "Show channel filters"
    override val hideChannelFilters = "Hide channel filters"
    override val previousChannel = "Previous channel"
    override val manageCategories = "Manage categories"
    override val changeCategory = "Change Category"
    override val selectCategory = "Select Category"
    override val searchEpg = "Search channel or programme"
    override val liveBadge = "LIVE"
    override val hideCategory = "Hide category"
    override val showCategory = "Show category"
    override val showAllCategories = "Show all"
    override val hideAllCategories = "Hide all"
    override val resetCategoryOrder = "Reset order"
    override val search = "Search"
    override val watchedProgress = "watched"
    override val sortDefault = "Default"
    override val sortByRating = "Top Rated"
    override val sortByYear = "Year"
    override val sortAlphabetical = "A–Z"
    override val switchPlaylist = "Switch playlist"
    override val epgNotConfigured = "EPG not configured"
    override fun epgMatched(matched: Int, total: Int) = "EPG: $matched/$total matched"
    override val unmatchedEpgChannels = "Unmatched EPG Channels"
    override val allEpgMatched = "All channels are matched with EPG"
    override val missingTvgId = "Missing tvg-id in playlist"
    override fun tvgIdNotFound(id: String) = "tvg-id \"$id\" not found in XMLTV"
    override fun unmatchedEpgCount(count: Int) = "$count missing"
    override val viewUnmatchedChannels = "View unmatched channels"
    override val saveChannelsList = "Save Channels List"
    override val shareChannelsList = "Share Channels List"
    override val channelsListSaved = "Channel list saved successfully"
    override val copiedToClipboard = "Copied to clipboard"
    override val xmltvGuide = "XMLTV guide"
    override val now = "NOW"
    override val next = "NEXT"
    override val whatsNext = "WHAT'S NEXT"
    override fun endsAt(time: String) = "Ends at $time"
    override fun until(time: String) = "until $time"
    override val noCurrentProgramme = "No current programme information"
    override val noUpcomingProgramme = "No upcoming programme information"
    override val noEpgId = "No EPG ID"
    override val noEpgData = "No EPG data"
    override val programmeGuide = "Programme guide"
    override fun todayDate(dayOfMonth: Int) = "Today $dayOfMonth"
    override val watchNow = "Watch now"
    override val watchLiveChannel = "Watch live channel"
    override val zoomInTimeline = "Zoom in timeline"
    override val zoomOutTimeline = "Zoom out timeline"
    override val channelList = "Channels"
    override val miniGuide = "Mini-Guide"
    override val aspectRatio = "Aspect ratio"
    override val lockControls = "Lock controls"
    override val unlockControls = "Unlock controls"
    override val brightness = "Brightness"
    override val volume = "Volume"
    override val aspectFill = "Aspect: Fill"
    override val aspectZoom = "Aspect: Zoom"
    override val aspectFit = "Aspect: Fit"
    override val playbackError = "Unable to play this channel."
    override val failoverBackupSwitched = "Switched to backup stream."
    override fun failoverSourceSwitched(sourceName: String) = "Switched to backup source: $sourceName"
    override val sportsHub = "Sports Hub"
    override val liveMatches = "Matches on TV"
    override val manageSports = "Manage"
    override val manageSportsTitle = "Sports & Competitions"
    override val manageSportsDescription = "Choose which sports and supported competitions appear in Matches on TV."
    override val noFollowedSportsMatches = "No matches match your followed competitions."
    override val sportsCategories = "Sports"
    override val supportedCompetitions = "Supported Competitions"
    override val sportFootball = "Football"
    override val sportRugby = "Rugby"
    override val sportMotorsport = "Motorsport"
    override val sportBasketball = "Basketball"
    override val sportTennis = "Tennis"
    override val sportOther = "Other Sports"
    override val watchLive = "Watch Live"
    override val liveNow = "Live Now"
    override fun broadcastOn(channel: String) = "Live on $channel"
    override fun alsoOn(channel: String) = "Also on $channel"
    override val matchDetails = "Match Details"
    override val matchEvents = "Key Events"
    override val noEventsYet = "No goals or key events yet"
    override val matchNotStarted = "Match has not started yet"
    override fun watchLiveOn(channel: String) = "Watch Live on $channel"
    override val topLeaders = "Top Performers"
    override val channelNotDetected = "Channel not found"
    override fun kickoffAt(time: String) = "Kickoff at $time"
    override val customizeHome = "Customize Home"
    override val customizeHomeDescription = "Choose which sections appear and in what order."
    override fun moveHomeSectionUp(section: String) = "Move $section up"
    override fun moveHomeSectionDown(section: String) = "Move $section down"
    override val sectionSports = "Matches & Sports"
    override val sectionContinueWatching = "Continue Watching"
    override val sectionFavoriteChannels = "Favorite Channels Now"
    override val sectionRecentChannels = "Recent Channels"
    override val sectionFollowedSeries = "Followed Series"
    override val sectionRecentMovies = "Recent Movies"
    override val sectionFeaturedSeries = "Featured Series"
    override val sectionTraktPopularMovies = "Popular Movies on Trakt"
    override val sectionTraktPopularSeries = "Popular Series on Trakt"
    override val buildingPlaybackBuffer = "Preparing playback…"
    override val reconnecting = "Reconnecting…"
    override val resumePlayback = "Continue watching?"
    override val resumePlaybackDescription = "Resume from where you stopped or restart from the beginning."
    override val resume = "Resume"
    override val restartPlayback = "Restart"
    override val continueWatching = "Continue watching"
    override val continueWatchingUnavailable = "This item is unavailable from its source."
    override val removeFromContinueWatching = "Remove from Continue watching"
    override val catchup = "Catch-up"
    override val watchCatchup = "Watch Catch-up"
    override val nextEpisode = "Next episode"
    override val playNextEpisode = "Play next"
    override val autoPlayNext = "Auto-Play Next"
    override val autoPlayNextDesc = "Automatically play next episode when current ends"
    override val sleepTimer = "Sleep Timer"
    override val sleepTimerHint = "Pauses playback when the timer finishes and asks whether you are still watching. No response stops playback after 30 seconds."
    override val sleepTimerOff = "Off"
    override val endOfEpisode = "Ask at end of video"
    override val endOfProgramme = "End of current programme"
    override fun sleepTimerSet(minutes: Int) = "Sleep timer set for $minutes minutes"
    override fun nextEpisodeIn(seconds: Int) = "Next episode in ${seconds}s"
    override val playNow = "Play Now"
    override val sleepTimerExpired = "Sleep timer finished. Playback stopped."
    override val areYouStillWatching = "Are you still watching?"
    override fun stillWatchingCountdown(seconds: Int) = "Playback will stop in ${seconds}s if there is no response."
    override val stopPlayback = "Stop playback"
    override val setAnotherTimer = "Set another timer"
    override val continueWithoutTimer = "Continue without timer"
    override val playNextThenStop = "Play next episode, then stop"
    override val audioAndSubtitles = "Audio & Subtitles"
    override val audioTracks = "Audio"
    override val subtitles = "Subtitles"
    override val subtitlesOff = "Off"
    override val noSubtitlesAvailable = "No subtitles available"
    override val noAudioTracksAvailable = "No audio tracks available"
    override val episodesDrawer = "Episodes"
    override fun seasonNumber(season: Int) = "Season $season"
    override val nowPlayingBadge = "Now Playing"

    override val videoQuality = "Video Quality"
    override val qualityAuto = "Auto"
    override val qualityAdaptive = "Adaptive bitrate (ABR)"
    override val goLive = "Go Live"
    override fun liveBehind(seconds: Long) = "${seconds}s behind broadcast"
    override val liveSynchronized = "Synchronized with live broadcast"
    override val streamResilience = "Stream Resilience"

    override val audioAndSubtitlesDefaults = "Audio & Subtitles Defaults"
    override val preferredAudioLanguage = "Preferred Audio"
    override val preferredSubtitleLanguage = "Preferred Subtitles"
    override val subtitleFallbackEnglish = "Fallback to English subtitles"
    override val subtitleFallbackEnglishDesc = "If preferred subtitle is not available, use English when present"
    override val defaultMediaAudio = "Media Default"
    override val langEnglish = "English"
    override val langPortuguese = "Portuguese"
    override val langSpanish = "Spanish"
    override val langFrench = "French"
    override val langGerman = "German"
    override val langItalian = "Italian"

    override val openSubtitles = "OpenSubtitles"
    override val openSubtitlesEnabled = "Enable OpenSubtitles"
    override val openSubtitlesApiKey = "API Key"
    override val openSubtitlesUsername = "OpenSubtitles username"
    override val openSubtitlesPassword = "OpenSubtitles password"
    override val openSubtitlesAutoSearch = "Auto-search missing subtitles"
    override val openSubtitlesAutoSearchDesc = "Download only when the video lacks the preferred subtitle language selected above"
    override val openSubtitlesPreferPtPt = "Prioritize Portugal Portuguese (pt-PT)"
    override val openSubtitlesPreferPtPtDesc = "Show and select pt-PT subtitles before Brazilian Portuguese"
    override val searchOnlineSubtitles = "Search online subtitles"
    override val searchingSubtitles = "Searching subtitles..."
    override val noSubtitlesFound = "No subtitles found"
    override val downloadingSubtitle = "Downloading subtitle..."
    override val subtitleDownloaded = "Subtitle activated"
    override val openSubtitlesTestConnection = "Test Connection"
    override val openSubtitlesConnectionSuccess = "Connection successful"
    override val openSubtitlesConnectionFailed = "Connection failed"
    override val openSubtitlesAccountCredentials = "Username and password are required for downloads. Use your OpenSubtitles username, not your email address."
    override val openSubtitlesApiKeyRequired = "API Key is required by OpenSubtitles.com"
    override val openSubtitlesApiKeyHint = "Get a free API key at opensubtitles.com/consumers"

    override val welcomeTitle = "Secure IPTV Player"
    override val welcomeSubtitle = "Private by default. Hardware-backed AES-256 encrypted storage."
    override val playlistM3u = "Playlist (M3U / M3U8)"
    override val enterM3uUrl = "https://example.com/playlist.m3u"
    override val orSelectLocalFile = "or select a local file"
    override val browseLocalFile = "Browse Local File"
    override val epgGuideXmltv = "Programme Guide (XMLTV) · Optional"
    override val enterXmltvUrl = "https://example.com/epg.xml"
    override val loadPlaylist = "Load Playlist"
    override val loadingPlaylist = "Loading playlist..."
    override val loadingPlaylistAndSettings = "Loading playlist and settings..."
    override fun welcomeUser(name: String) = "Welcome, $name"
    override val loadingSavedData = "Loading saved playlist and programme guide..."
    override val restoringSecureSession = "Restoring secure session..."
    override val selectM3uError = "Please enter an M3U URL or select a local file."

    override val settingsTitle = "Settings"
    override val tabAccount = "Account"
    override val tabProfiles = "Profiles"
    override val profiles = "Profiles"
    override val switchProfile = "Switch Profile"
    override val libraryProfileMenu = "Profile"
    override val closeApp = "Close app"
    override val manageProfiles = "Manage Profiles"
    override val childProfile = "Child profile"
    override val adultProfile = "Adult profile"
    override val pinProtected = "PIN Protected"
    override val importLocalDataTitle = "Import Local Device Data"
    override val importLocalDataMessage = "Would you like to import this device's existing playlists and favorites into your account?"
    override val importLocalDataConfirm = "Import to Account"
    override val importLocalDataDismiss = "Keep Local Only"
    override val tabPlaylists = "Playlists"
    override val tabEpg = "EPG Guide"
    override val tabGeneral = "General"
    override val tabSubtitles = "Audio & Subtitles"
    override val tabApiKeys = "API Keys"
    override val tmdbHeader = "The Movie Database (TMDB)"
    override val tmdbEnabled = "Enable TMDB Metadata"
    override val tmdbApiKey = "TMDB API Key / Access Token"
    override val tmdbApiKeyHint = "Supports API v3 Key or API v4 Read Access Token"
    override val tmdbLanguage = "Metadata Language"
    override val tmdbLanguageDesc = "Preferred language for synopsis, titles and media descriptions"
    override val tmdbLanguageAuto = "Auto (Match Profile)"
    override val tmdbTestConnection = "Test Connection"
    override val tmdbConnectionSuccess = "Connection successful"
    override val tmdbConnectionFailed = "Connection failed"
    override val tmdbInfoHelp = "Obtain a free TMDB API key or Read Access Token at themoviedb.org"
    override val tmdbInfoLink = "https://www.themoviedb.org/settings/api"
    override val omdbHeader = "Open Movie Database (OMDb)"
    override val omdbEnabled = "Enable OMDb Ratings"
    override val omdbApiKey = "OMDb API Key"
    override val omdbApiKeyHint = "Enter your free 8-character OMDb API key"
    override val omdbTestConnection = "Test Connection"
    override val omdbConnectionSuccess = "Connection successful"
    override val omdbConnectionFailed = "Connection failed"
    override val omdbInfoHelp = "Obtain a free personal OMDb API key at omdbapi.com"
    override val omdbInfoLink = "https://www.omdbapi.com/apikey.aspx"
    override val imdbRating = "IMDb"
    override val rottenTomatoes = "Rotten Tomatoes"
    override val metascore = "Metascore"
    override val awards = "Awards"
    override val topRatedFilter = "Top Rated (7.5+)"
    override val freshFilter = "Fresh (60%+)"
    override val skipIntro = "Skip Intro"
    override val skipRecap = "Skip Recap"
    override val skipCredits = "Skip Credits"
    override val skipPreview = "Skip Preview"
    override val introDbHeader = "TheIntroDB (Intro / Recap / Credits)"
    override val introDbEnabled = "Enable Skip Buttons"
    override val introDbEnabledDesc = "Displays Skip Intro, Skip Recap, and Skip Credits buttons during series episodes"
    override val introDbAutoSkipIntro = "Auto-skip Intro"
    override val introDbAutoSkipIntroDesc = "Automatically skips opening intros without asking"
    override val introDbAutoSkipRecap = "Auto-skip Recap"
    override val introDbAutoSkipRecapDesc = "Automatically skips previous episode recaps"
    override val traktHeader = "Trakt.tv"
    override val traktDesc = "Sync watch history, playback progress and scrobble VOD automatically."
    override val traktClientId = "Trakt Client ID"
    override val traktClientIdHint = "Enter your Trakt application Client ID"
    override val traktClientIdLengthWarning = "Trakt Client ID is too short (currently %d characters)"
    override val traktConnect = "Connect with Trakt"
    override val traktDisconnect = "Disconnect"
    override val traktConnectedAs = "Connected as"
    override val apiServiceConnected = "Connected"
    override val apiServiceDisconnected = "Disconnected"
    override val apiServiceConnect = "Connect"
    override val traktVipBadge = "VIP"
    override val traktScrobble = "Scrobble VOD Playback"
    override val traktScrobbleDesc = "Automatically update your Trakt watch status when watching movies and series"
    override val traktPairingTitle = "Connect to Trakt"
    override val traktPairingInstructions = "Scan the QR code with your phone or visit the link below and enter this code:"
    override val traktPairingLink = "https://trakt.tv/activate"
    override val traktPairingGenerating = "Generating pairing code..."
    override val traktPairingWaiting = "Waiting for authorization on Trakt..."
    override val traktPairingExpiresIn = "Code expires in %s"
    override val traktPairingExpired = "Pairing code expired"
    override val traktPairingDenied = "Authorization was denied on Trakt"
    override val traktPairingSuccess = "Successfully connected to Trakt!"
    override val traktPairingRetry = "Try Again"
    override val traktHelp = "Obtain a free Trakt Client ID at trakt.tv/oauth/applications"
    override val traktHelpLink = "https://trakt.tv/oauth/applications"
    override val sectionTraktWatchlist = "Trakt Watchlist"
    override val sectionTraktRecommendations = "Recommended for You"
    override val traktWatchlist = "Trakt Watchlist"
    override val traktWatchlistEmpty = "No items in your Trakt watchlist match your current sources."
    override val traktSyncWatchlist = "Sync Watchlist"
    override val traktSyncingWatchlist = "Syncing with Trakt..."
    override val traktWatchlistSynced = "Watchlist synced (%d items)"
    override val traktAddToWatchlist = "Add to Trakt Watchlist"
    override val traktRemoveFromWatchlist = "Remove from Trakt Watchlist"
    override val traktWatchedHistory = "Watched History"
    override val traktSyncWatchedHistory = "Import from Trakt"
    override val traktSyncingWatchedHistory = "Syncing watched history with Trakt..."
    override val traktWatchedHistorySynced = "Watched history synced (%d items matched)"
    override val traktWatchedHistoryEmpty = "No watched items synced yet."
    override val traktRecommendations = "Recommended for You"
    override val traktRecommendationsEmpty = "No recommendations match your current sources."
    override val traktIncludesTrending = "Includes trending titles from Trakt"
    override val traktSeriesRecommendationsEmpty = "Series recommendations appear when a Trakt pick matches your library."
    override val traktTrendingSeries = "Trending series on Trakt"
    override val traktSyncRecommendations = "Sync Recommendations"
    override val traktSyncingRecommendations = "Syncing recommendations with Trakt..."
    override val traktRecommendationsSynced = "Recommendations synced (%d items)"
    override val traktHideRecommendation = "Hide on Trakt"
    override val traktHideRecommendationConfirmation = "Hide this title from your Trakt recommendations? You can bring it back from Hidden recommendations on Home."
    override val traktHiddenRecommendations = "Hidden recommendations"
    override val traktNoHiddenRecommendations = "You have no hidden Trakt recommendations."
    override val traktRestoreRecommendation = "Restore"
    override val traktRecommendationActionFailed = "Trakt could not update this recommendation. Try again."
    override val traktHiddenRecommendationsFailed = "Could not load hidden recommendations from Trakt."
    override val traktLoadMoreHiddenRecommendations = "Load more"
    override val traktHidingRecommendation = "Hiding recommendation..."
    override val watchTrailer = "Watch Trailer"
    override val openInYouTube = "Open in YouTube"
    override val openTrailerExternally = "Open trailer externally"
    override val trailerUnavailableInApp = "This trailer can’t play in FortressTV."
    override val trailerLinkOpenFailed = "Couldn’t open the trailer link."
    override val genres = "Genres"
    override val tabAbout = "About"
    override val language = "Language"
    override val playback = "Playback"
    override val playbackMode = "Playback stability"
    override val playbackModeDesc = "Choose the balance between live delay, reliability and mobile data use."
    override val playbackModeAuto = "Automatic"
    override val playbackModeStable = "Stable"
    override val playbackModeDataSaver = "Data saver"
    override val playChannelsFullscreen = "Open channels in fullscreen"
    override val playChannelsFullscreenDesc = "Enter fullscreen immediately when you select a channel."
    override val autoPictureInPicture = "Picture-in-Picture"
    override val autoPictureInPictureDesc = "Keep fullscreen video in a floating window when you press Home."
    override val anonymousTelemetry = "Anonymous playback quality data"
    override val anonymousTelemetryDesc = "Helps diagnose stream connection issues and tune playback stability."
    override val healthStable = "Stable"
    override val healthFair = "Fair"
    override val healthUnstable = "Unstable"
    override val healthTesting = "Testing..."
    override val connectionHealth = "Connection Quality"
    override val startupLatency = "Startup latency"
    override val bufferStalls = "Buffer stalls"
    override val testRegion = "Region"
    override val addNewPlaylist = "Add New Playlist"
    override val addPlaylist = "Add Playlist"
    override val playlistName = "Playlist Name"
    override val playlistNamePlaceholder = "e.g. Sports, Family, Movies"
    override val sourceType = "Source Type"
    override val sourceRole = "Library content"
    override val roleAutomatic = "Automatic"
    override val roleLiveTv = "Live TV"
    override val roleMovies = "Movies"
    override val roleSeries = "Series"
    override val roleMoviesAndSeries = "Movies and Series"
    override val roleEverything = "Everything"
    override val remoteUrl = "Remote URL (HTTP/HTTPS)"
    override val localFile = "Local File (M3U/M3U8)"
    override val xtreamCodes = "Xtream Codes"
    override val xtreamServer = "Server URL"
    override val username = "Username"
    override val password = "Password"
    override val connect = "Connect"
    override val selectFile = "Select File"
    override val deletePlaylistConfirmTitle = "Delete Playlist"
    override fun deletePlaylistConfirmMsg(name: String) = "Are you sure you want to delete \"$name\"? This action cannot be undone."
    override fun lastUpdated(time: String) = "Last updated: $time"
    override val updatedJustNow = "Updated just now"
    override fun updatedMinutesAgo(minutes: Long) = "Updated ${minutes}m ago"
    override fun updatedHoursAgo(hours: Long) = "Updated ${hours}h ago"
    override fun updatedAt(time: String) = "Updated $time"
    override val notUpdatedYet = "Not updated yet"
    override val refreshPlaylist = "Refresh playlist"
    override val xmltvSource = "XMLTV Guide Source"
    override val noEpgConfiguredMsg = "No XMLTV guide configured"
    override val configureXmltv = "Configure XMLTV Guide"
    override val changeXmltv = "Change XMLTV Guide"
    override val removeEpg = "Remove EPG"
    override val removeEpgConfirmation = "FortressTV will forget this EPG source and clear its local schedule cache. Playlists will not be affected."
    override val battery = "Battery"
    override val timeShift = "EPG Schedule Correction"
    override val timeShiftDesc = "Adjust this if programme times are offset from your local timezone."
    override val epgScheduleCorrection = "EPG Schedule Correction"
    override val epgScheduleCorrectionDesc = "+1 hour makes programmes appear one hour later."
    override fun epgPreview(baseHour: String, correctedHour: String) =
        "Programme starts at $baseHour → corrected to $correctedHour"
    override val epgSourceTimezone = "EPG Source Timezone"
    override val epgSourceTimezoneDesc = "Used only when XMLTV listings omit a timezone offset."
    override val timezoneAutoDevice = "Automatic (Device timezone)"
    override val directEntryMinutes = "Direct entry (minutes)"
    override val epgManualMapping = "EPG Channel Mapping"
    override val epgManualMappingDesc = "Manually associate an IPTV channel with an XMLTV guide channel."
    override val mapEpgChannel = "Map EPG Channel"
    override val resetToAutomatic = "Reset to Automatic"
    override val mappingUnavailable = "Unavailable"
    override val searchXmltvChannels = "Search XMLTV channels..."
    override val noXmltvChannelsFound = "No matching XMLTV channels found"
    override val mappedTo = "Mapped to"
    override val epgLoadedDays = "EPG Days to Load"
    override val epgLoadedDaysDesc = "Number of calendar days of programme data to retain in memory and cache (1 to 7 days)."
    override fun epgDaysCount(days: Int) = if (days == 1) "1 day" else "$days days"
    override val securityTitle = "FortressTV Security & Privacy"
    override val securityDesc = "Hardware-backed AES-256 encryption via Android KeyStore. All sources and cached stream tokens remain private on this device."
    override val contentDisclaimerTitle = "Content notice"
    override val contentDisclaimer = "FortressTV is a media player. It does not provide, host, sell or distribute playlists, channels, films, series, EPG data or other streaming content. You are responsible for adding only sources you are authorized to access and for complying with applicable laws and provider terms."
    override val openSourceSoftware = "Open-source software"
    override val ffmpegAttribution = "FortressTV uses FFmpeg 6.0 under the LGPL v2.1 or later to decode otherwise unsupported MPEG audio. The corresponding source, licence, and build instructions are available from the public download link below."
    override val viewSourceCode = "View source and licences"
    override val appVersion = "Version 1.0.0"
    override val noSourcesConfigured = "No sources configured"
    override val addSourceGettingStarted = "Add your M3U / M3U8 URL or Xtream Codes server to get started."
    override fun versionLabel(version: String, code: Int) = "Version $version ($code) · Pride1922"
    override fun installedVersion(version: String) = "Installed: v$version"
    override val directUrlPrivacyHint = "Enter a direct HTTP or HTTPS URL. It will be encrypted on this device."

    override val checkForUpdates = "Check for Updates"
    override val checkingForUpdates = "Checking for updates..."
    override val updateAvailable = "Update Available"
    override fun updateAvailableDesc(version: String) = "Version $version is available."
    override val upToDate = "You are using the latest version"
    override val downloadingUpdate = "Downloading update..."
    override val updateDownloaded = "Download complete"
    override val installUpdateHint = "If Android's installer was dismissed, tap Install Update to continue. The update will not be downloaded again."
    override val updateNow = "Update Now"
    override val installUpdate = "Install Update"
    override val later = "Later"
    override val hideAdultContent = "Hide Adult Content"
    override val hideAdultContentSubtitle = "Filter out adult (XXX / 18+) channels, movies, and series"
    override val hideAdultContentChildRestricted = "Adult content is permanently blocked on child profiles"
    override val newUpdateAvailable = "New Update Available"
    override fun updateFailed(error: String) = "Update failed: $error"

    override val parentalControl = "Parental Control"
    override val parentalControlDesc = "Protect adult content and settings with a 4-digit PIN"
    override val setParentalPin = "Set 4-Digit PIN"
    override val changeParentalPin = "Change PIN"
    override val removeParentalPin = "Remove PIN"
    override val enterParentalPin = "Enter 4-digit PIN"
    override val confirmParentalPin = "Confirm 4-digit PIN"
    override val enterNewPin = "Enter new 4-digit PIN"
    override val pinIncorrect = "Incorrect PIN. Try again."
    override val pinsDoNotMatch = "PINs do not match. Try again."
    override val pinSetSuccess = "PIN set successfully"
    override val pinRemovedSuccess = "PIN removed"
    override val unlockAdultContent = "Unlock Adult Content"
    override val unlockSettings = "Unlock Settings"
    override val quickRecents = "Recently Watched"

    override val showPassword = "Show password"
    override val transferScanWithApp = "Scan with your phone to open FortressTV and send a playlist. Both devices must be on the same local network."
    override val hidePassword = "Hide password"

    // Local transfer (QR / phone to TV)
    override val transferSendFromPhone = "Send from Phone or Computer"
    override val transferSendFromPhoneDesc = "Add a source by scanning a QR code with any phone or opening the URL on a computer"
    override val transferDialogTitle = "Add Source from Phone"
    override val transferScanQr = "Scan with your phone camera"
    override val transferOrOpenUrl = "Or open this address in a browser:"
    override val transferPairingCode = "Pairing code:"
    override val transferSessionExpired = "Session expired. Please try again."
    override val transferServerStartFailed = "Could not start local transfer. Check the network connection and try again."
    override val transferWaiting = "Waiting for connection…"
    override val transferReceived = "Source received — confirm below"
    override val transferConfirmTitle = "Confirm source"
    override val transferConfirmDesc = "Review the source below before adding it to your library."
    override val transferSourceType = "Type"
    override val transferSourceServer = "Server"
    override val transferAddSource = "Add Source"
    override val transferSuccessMessage = "Source added successfully!"
    override val transferLockedOut = "Too many failed attempts. Please close and try again."
    override fun transferAttemptsRemaining(remaining: Int) =
        if (remaining == 1) "1 attempt remaining" else "$remaining attempts remaining"
    override val transferNetworkError = "Could not start local server. Check Wi-Fi."
    override val transferCancelSession = "Cancel"
    override val transferSendToTv = "Send to TV"
    override val transferSendToTvDesc = "Choose a source and a nearby FortressTV device on the same local network."
    override val transferChooseSource = "Source to send"
    override val transferAvailableTvs = "Available TVs"
    override val transferConnectedViaQr = "Connected to TV via QR code"
    override val transferSearchingTvs = "Searching for TVs on the local network…"
    override val transferEnterTvCode = "6-digit TV code"
    override val transferEnterTvCodeDesc = "Enter the pairing code displayed on the TV."
    override val transferSendNow = "Send to TV"
    override val transferSending = "Sending…"
    override val transferSentToTv = "Source sent successfully"
    override val transferConfirmOnTv = "Confirm the source on your TV to finish."
    override val transferSendFailed = "Could not send the source. Check the code and ensure both devices are on the same network."
    override val transferNoSendableSources = "No URL or Xtream sources are available to send."
    override val transferSourceTab = "Playlist + EPG"
    override val transferSourceExplanation = "Send only the selected playlist and its associated EPG configuration."
    override val transferQrRequiredForSource = "Sending a source requires scanning the TV's QR code to protect your credentials."
}

object PortugueseStrings : Strings {
    override val languageTag = "pt-PT"
    override val appName = "FortressTV"
    override val allChannels = "Todos os canais"
    override val sources = "Fontes"
    override val categories = "Categorias"
    override val uncategorized = "Sem categoria"
    override val navHome = "Início"
    override val navLiveTv = "TV em direto"
    override val navMovies = "Filmes"
    override val navSeries = "Séries"
    override val navFavorites = "Favoritos"
    override val homeContinueWatching = "Continuar a ver"
    override val homeNextUp = "A seguir"
    override val homeFavoriteChannelsNow = "Favoritos em direto"
    override val homeRecentChannels = "Canais vistos recentemente"
    override val homeUnifiedSearchPlaceholder = "Pesquisar canais, filmes e séries..."
    override val homeChannelsCategory = "Canais"
    override val homeMoviesCategory = "Filmes"
    override val homeSeriesCategory = "Séries"
    override val homeFeaturedMovies = "Filmes adicionados recentemente"
    override val homePopularOnTraktMovies = "Filmes populares no Trakt"
    override val homeFeaturedSeries = "Séries em destaque"
    override val homePopularOnTraktSeries = "Séries populares no Trakt"
    override val homeRecentlyAddedSeries = "Séries adicionadas recentemente"
    override val homeNothingFound = "Nenhum resultado encontrado"
    override val homeStartupScreen = "Ecrã inicial"
    override val homeStartupScreenHome = "Início"
    override val homeStartupScreenLiveTv = "TV em direto"
    override val homeNoContent = "O seu painel aparecerá aqui à medida que vê canais, filmes e séries."
    override val quickZap = "Zapping Rápido"
    override val quickZapFavorites = "Favoritos"
    override val quickZapRecent = "Recentes"
    override val dockedPlayer = "Mini Leitor"
    override val closeDockedPlayer = "Fechar mini leitor"
    override val comingSoon = "Brevemente"
    override val moviesComingSoon = "A sua biblioteca de filmes aparecerá aqui quando configurar uma fonte VOD."
    override val seriesComingSoon = "As suas séries, temporadas e episódios aparecerão aqui quando configurar uma fonte VOD."
    override val noFavoriteChannels = "Os canais favoritos aparecerão aqui."
    override val addFavorite = "Adicionar aos Favoritos"
    override val removeFavorite = "Remover dos Favoritos"
    override val favoriteChannels = "Canais"
    override val recentlyWatched = "Vistos recentemente"
    override val noRecentlyWatched = "Os canais vistos recentemente aparecerão aqui."
    override val movieWatchlist = "Filmes para ver"
    override val followedSeries = "Séries seguidas"
    override val addToWatchlist = "Adicionar à lista para ver"
    override val removeFromWatchlist = "Remover da lista para ver"
    override val followSeries = "Seguir série"
    override val unfollowSeries = "Deixar de seguir série"
    override val noFavorites = "Os seus canais, filmes e séries favoritos aparecerão aqui."
    override val noWatchlistMovies = "Os filmes adicionados à lista para ver aparecerão aqui."
    override val noFollowedSeries = "As séries que segue aparecerão aqui."
    override val noMoviesAvailable = "Não existem Filmes disponíveis nas fontes Xtream ativas."
    override val noSeriesAvailable = "Não existem Séries disponíveis nas fontes Xtream ativas."
    override val allCategories = "Tudo"
    override val searchMovies = "Pesquisar filmes"
    override val searchSeries = "Pesquisar séries"
    override val loadingEpisodes = "A carregar episódios..."
    override val loadingMovieDetails = "A carregar detalhes do filme..."
    override val movieDetailsUnavailable = "Não foi possível carregar mais informações."
    override val loadingSeriesDetails = "A carregar detalhes da série e episódios..."
    override val seriesDetailsUnavailable = "Não foi possível carregar as informações da série."
    override val genre = "Género"
    override val duration = "Duração"
    override val releaseDate = "Data de estreia"
    override val director = "Realização"
    override val cast = "Elenco"
    override val country = "País"
    override val ageRating = "Classificação etária"
    override val noEpisodesAvailable = "Não existem episódios disponíveis para esta Série."
    override fun seasonLabel(season: Int) = "Temporada $season"
    override fun seasonsCount(count: Int) = if (count == 1) "1 Temporada" else "$count Temporadas"
    override fun episodeLabel(season: Int, episode: Int) = "T${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')}"
    override val markWatched = "Marcar como visto"
    override val markUnwatched = "Marcar como não visto"
    override val watched = "Visto"
    override val unwatched = "Não visto"
    override fun playNextEpisode(season: Int, episode: Int) = "Ver seguinte (T${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')})"
    override val allEpisodesWatched = "Todos os episódios vistos"
    override val allAvailableEpisodesWatched = "Todos os episódios disponíveis vistos"
    override val followedFilter = "A seguir"
    override val watchlistFilter = "Para ver"
    override val restartSeries = "Recomeçar série"
    override fun unwatchedEpisodesCount(count: Int) = if (count == 1) "1 por ver" else "$count por ver"
    override fun seasonUnwatchedCount(count: Int) = if (count == 1) "1 por ver" else "$count por ver"
    override val nextUpBadge = "A Seguir"
    override val fastScrollJump = "Deslocamento rápido"
    override val jumpToLetter = "Saltar para"
    override fun searchInGroup(group: String) = "Pesquisar em $group"
    override val searchChannels = "Pesquisar canais"
    override val noChannelsMatch = "Nenhum canal corresponde à sua pesquisa."
    override fun channelsCount(count: Int) = "$count ${if (count == 1) "canal" else "canais"}"
    override val settings = "Definições"
    override val close = "Fechar"
    override val clearSearch = "Limpar pesquisa"
    override val clearRecentChannels = "Limpar canais vistos recentemente"
    override val save = "Guardar"
    override val cancel = "Cancelar"
    override val delete = "Eliminar"
    override val retry = "Tentar novamente"
    override val refresh = "Atualizar"
    override val edit = "Editar"
    override val editPlaylist = "Editar Lista"
    override val rename = "Mudar o nome"
    override val active = "Ativo"
    override val enabledSource = "Ativa"
    override fun sourcesCount(count: Int) = "$count ${if (count == 1) "fonte" else "fontes"}"
    override val moveSourceUp = "Mover fonte para cima"
    override val moveSourceDown = "Mover fonte para baixo"
    override val switchAction = "Mudar"
    override val back = "Voltar"
    override val fullscreen = "Ecrã inteiro"

    override val collapseSidebar = "Ocultar barra lateral"
    override val expandSidebar = "Expandir barra lateral"
    override val showChannelFilters = "Mostrar filtros de canais"
    override val hideChannelFilters = "Ocultar filtros de canais"
    override val previousChannel = "Canal anterior"
    override val manageCategories = "Gerir categorias"
    override val changeCategory = "Mudar categoria"
    override val selectCategory = "Selecionar categoria"
    override val searchEpg = "Pesquisar canal ou programa"
    override val liveBadge = "DIRETO"
    override val hideCategory = "Ocultar categoria"
    override val showCategory = "Mostrar categoria"
    override val showAllCategories = "Mostrar todas"
    override val hideAllCategories = "Ocultar todas"
    override val resetCategoryOrder = "Repor ordem"
    override val search = "Pesquisar"
    override val watchedProgress = "visto"
    override val sortDefault = "Predefinição"
    override val sortByRating = "Melhor avaliados"
    override val sortByYear = "Ano"
    override val sortAlphabetical = "A–Z"
    override val switchPlaylist = "Mudar de lista"
    override val epgNotConfigured = "EPG não configurado"
    override fun epgMatched(matched: Int, total: Int) = "EPG: $matched/$total correspondidos"
    override val unmatchedEpgChannels = "Canais sem EPG correspondente"
    override val allEpgMatched = "Todos os canais têm EPG correspondente"
    override val missingTvgId = "Sem atributo tvg-id na lista"
    override fun tvgIdNotFound(id: String) = "tvg-id \"$id\" não encontrado no XMLTV"
    override fun unmatchedEpgCount(count: Int) = "$count em falta"
    override val viewUnmatchedChannels = "Ver canais sem correspondência"
    override val saveChannelsList = "Guardar lista de canais"
    override val shareChannelsList = "Partilhar lista de canais"
    override val channelsListSaved = "Lista de canais guardada com sucesso"
    override val copiedToClipboard = "Copiado para a área de transferência"
    override val xmltvGuide = "Guia XMLTV"
    override val now = "Agora"
    override val next = "SEGUINTE"
    override val whatsNext = "A SEGUIR"
    override fun endsAt(time: String) = "Termina às $time"
    override fun until(time: String) = "até às $time"
    override val noCurrentProgramme = "Sem informações do programa atual"
    override val noUpcomingProgramme = "Sem informações de programas futuros"
    override val noEpgId = "Sem ID de EPG"
    override val noEpgData = "Sem dados de EPG"
    override val programmeGuide = "Guia de programação"
    override fun todayDate(dayOfMonth: Int) = "Hoje $dayOfMonth"
    override val watchNow = "Ver agora"
    override val watchLiveChannel = "Ver canal em direto"
    override val zoomInTimeline = "Aumentar escala da linha temporal"
    override val zoomOutTimeline = "Diminuir escala da linha temporal"
    override val channelList = "Canais"
    override val miniGuide = "Mini-guia"
    override val aspectRatio = "Proporção de ecrã"
    override val lockControls = "Bloquear controlos"
    override val unlockControls = "Desbloquear controlos"
    override val brightness = "Brilho"
    override val volume = "Volume"
    override val aspectFill = "Proporção: Preencher"
    override val aspectZoom = "Proporção: Zoom"
    override val aspectFit = "Proporção: Ajustar"
    override val playbackError = "Não foi possível reproduzir este canal."
    override val failoverBackupSwitched = "Comutado para emissão alternativa."
    override fun failoverSourceSwitched(sourceName: String) = "Comutado para fonte alternativa: $sourceName"
    override val sportsHub = "Desporto"
    override val liveMatches = "Jogos na TV"
    override val manageSports = "Gerir"
    override val manageSportsTitle = "Desportos e Competições"
    override val manageSportsDescription = "Escolha os desportos e competições suportadas a mostrar em Jogos na TV."
    override val noFollowedSportsMatches = "Nenhum jogo corresponde às competições seguidas."
    override val sportsCategories = "Desportos"
    override val supportedCompetitions = "Competições Suportadas"
    override val sportFootball = "Futebol"
    override val sportRugby = "Râguebi"
    override val sportMotorsport = "Desporto Motorizado"
    override val sportBasketball = "Basquetebol"
    override val sportTennis = "Ténis"
    override val sportOther = "Outros Desportos"
    override val watchLive = "Ver em Direto"
    override val liveNow = "Em Direto"
    override fun broadcastOn(channel: String) = "Em direto na $channel"
    override fun alsoOn(channel: String) = "Também na $channel"
    override val matchDetails = "Detalhes do Jogo"
    override val matchEvents = "Eventos Principais"
    override val noEventsYet = "Ainda sem golos ou cartões"
    override val matchNotStarted = "O jogo ainda não começou"
    override fun watchLiveOn(channel: String) = "Ver em direto na $channel"
    override val topLeaders = "Melhores Marcadores"
    override val channelNotDetected = "Canal não detetado"
    override fun kickoffAt(time: String) = "Início às $time"
    override val customizeHome = "Personalizar Início"
    override val customizeHomeDescription = "Escolha as secções visíveis e a respetiva ordem."
    override fun moveHomeSectionUp(section: String) = "Mover $section para cima"
    override fun moveHomeSectionDown(section: String) = "Mover $section para baixo"
    override val sectionSports = "Jogos e Desporto"
    override val sectionContinueWatching = "Continuar a ver"
    override val sectionFavoriteChannels = "Canais favoritos agora"
    override val sectionRecentChannels = "Canais recentes"
    override val sectionFollowedSeries = "Séries seguidas"
    override val sectionRecentMovies = "Filmes recentes"
    override val sectionFeaturedSeries = "Séries em destaque"
    override val sectionTraktPopularMovies = "Filmes populares no Trakt"
    override val sectionTraktPopularSeries = "Séries populares no Trakt"
    override val buildingPlaybackBuffer = "A preparar a reprodução…"
    override val reconnecting = "A restabelecer ligação…"
    override val resumePlayback = "Continuar a ver?"
    override val resumePlaybackDescription = "Continue a partir de onde parou ou recomece do início."
    override val resume = "Continuar"
    override val restartPlayback = "Recomeçar"
    override val continueWatching = "Continuar a ver"
    override val continueWatchingUnavailable = "Este item não está disponível na respetiva fonte."
    override val removeFromContinueWatching = "Remover de Continuar a ver"
    override val catchup = "Gravações"
    override val watchCatchup = "Ver gravação"
    override val nextEpisode = "Próximo episódio"
    override val playNextEpisode = "Reproduzir próximo"
    override val autoPlayNext = "Reprodução automática"
    override val autoPlayNextDesc = "Reproduzir automaticamente o próximo episódio"
    override val sleepTimer = "Temporizador de suspensão"
    override val sleepTimerHint = "Pausa a reprodução quando o temporizador termina e pergunta se ainda está a ver. Sem resposta, para a reprodução após 30 segundos."
    override val sleepTimerOff = "Desativado"
    override val endOfEpisode = "Perguntar no final do vídeo"
    override val endOfProgramme = "No final do programa atual"
    override fun sleepTimerSet(minutes: Int) = "Temporizador definido para $minutes minutos"
    override fun nextEpisodeIn(seconds: Int) = "Próximo episódio em ${seconds}s"
    override val playNow = "Reproduzir Agora"
    override val sleepTimerExpired = "Temporizador de suspensão terminado. Reprodução parada."
    override val areYouStillWatching = "Ainda está a ver?"
    override fun stillWatchingCountdown(seconds: Int) = "A reprodução será parada dentro de ${seconds}s se não houver resposta."
    override val stopPlayback = "Parar reprodução"
    override val setAnotherTimer = "Definir outro temporizador"
    override val continueWithoutTimer = "Continuar sem temporizador"
    override val playNextThenStop = "Reproduzir o próximo episódio e depois parar"
    override val audioAndSubtitles = "Áudio e Legendas"
    override val audioTracks = "Áudio"
    override val subtitles = "Legendas"
    override val subtitlesOff = "Desativado"
    override val noSubtitlesAvailable = "Nenhuma legenda disponível"
    override val noAudioTracksAvailable = "Nenhuma faixa de áudio disponível"
    override val episodesDrawer = "Episódios"
    override fun seasonNumber(season: Int) = "Temporada $season"
    override val nowPlayingBadge = "A reproduzir"

    override val videoQuality = "Qualidade de vídeo"
    override val qualityAuto = "Automático"
    override val qualityAdaptive = "Taxa de bits adaptativa (ABR)"
    override val goLive = "Ir para Direto"
    override fun liveBehind(seconds: Long) = "${seconds}s atrás da emissão"
    override val liveSynchronized = "Sincronizado com a emissão em direto"
    override val streamResilience = "Resiliência do fluxo"

    override val audioAndSubtitlesDefaults = "Predefinições de Áudio e Legendas"
    override val preferredAudioLanguage = "Áudio preferido"
    override val preferredSubtitleLanguage = "Legendas preferidas"
    override val subtitleFallbackEnglish = "Recorrer a legendas em inglês"
    override val subtitleFallbackEnglishDesc = "Se a legenda preferida não estiver disponível, usar inglês se existir"
    override val defaultMediaAudio = "Predefinido do vídeo"
    override val langEnglish = "Inglês"
    override val langPortuguese = "Português"
    override val langSpanish = "Espanhol"
    override val langFrench = "Francês"
    override val langGerman = "Alemão"
    override val langItalian = "Italiano"

    override val openSubtitles = "OpenSubtitles"
    override val openSubtitlesEnabled = "Ativar OpenSubtitles"
    override val openSubtitlesApiKey = "Chave de API"
    override val openSubtitlesUsername = "Nome de utilizador do OpenSubtitles"
    override val openSubtitlesPassword = "Palavra-passe do OpenSubtitles"
    override val openSubtitlesAutoSearch = "Pesquisa automática de legendas em falta"
    override val openSubtitlesAutoSearchDesc = "Transferir apenas quando o vídeo não tiver legendas no idioma preferido selecionado acima"
    override val openSubtitlesPreferPtPt = "Priorizar português de Portugal (pt-PT)"
    override val openSubtitlesPreferPtPtDesc = "Apresentar e selecionar legendas pt-PT antes de português do Brasil"
    override val searchOnlineSubtitles = "Pesquisar legendas online"
    override val searchingSubtitles = "A pesquisar legendas..."
    override val noSubtitlesFound = "Nenhuma legenda encontrada"
    override val downloadingSubtitle = "A transferir legenda..."
    override val subtitleDownloaded = "Legenda ativada"
    override val openSubtitlesTestConnection = "Testar ligação"
    override val openSubtitlesConnectionSuccess = "Ligação com sucesso"
    override val openSubtitlesConnectionFailed = "Falha na ligação"
    override val openSubtitlesAccountCredentials = "O nome de utilizador e a palavra-passe são obrigatórios para transferências. Use o nome de utilizador do OpenSubtitles, não o endereço de e-mail."
    override val openSubtitlesApiKeyRequired = "A chave de API é obrigatória no OpenSubtitles.com"
    override val openSubtitlesApiKeyHint = "Obtenha uma chave gratuita em opensubtitles.com/consumers"

    override val welcomeTitle = "Reprodutor IPTV Seguro"
    override val welcomeSubtitle = "Privacidade por padrão. Armazenamento encriptado com AES-256."
    override val playlistM3u = "Lista de Reprodução (M3U / M3U8)"
    override val enterM3uUrl = "https://exemplo.com/lista.m3u"
    override val orSelectLocalFile = "ou selecione um ficheiro local"
    override val browseLocalFile = "Procurar Ficheiro Local"
    override val epgGuideXmltv = "Guia de Programação (XMLTV) · Opcional"
    override val enterXmltvUrl = "https://exemplo.com/epg.xml"
    override val loadPlaylist = "Carregar Lista"
    override val loadingPlaylist = "A carregar lista..."
    override val loadingPlaylistAndSettings = "A carregar lista e definições..."
    override fun welcomeUser(name: String) = "Bem-vindo, $name"
    override val loadingSavedData = "A carregar a lista e o guia de programação guardados..."
    override val restoringSecureSession = "A restaurar sessão segura..."
    override val selectM3uError = "Por favor insira um URL M3U ou selecione um ficheiro local."

    override val settingsTitle = "Definições"
    override val tabAccount = "Conta"
    override val tabProfiles = "Perfis"
    override val profiles = "Perfis"
    override val switchProfile = "Mudar de Perfil"
    override val libraryProfileMenu = "Perfil"
    override val closeApp = "Fechar aplicação"
    override val manageProfiles = "Gerir Perfis"
    override val childProfile = "Perfil de criança"
    override val adultProfile = "Perfil de adulto"
    override val pinProtected = "Protegido por PIN"
    override val importLocalDataTitle = "Importar Dados do Dispositivo"
    override val importLocalDataMessage = "Deseja importar as listas e os favoritos existentes deste dispositivo para a sua conta?"
    override val importLocalDataConfirm = "Importar para a Conta"
    override val importLocalDataDismiss = "Manter Apenas Local"
    override val tabPlaylists = "Listas"
    override val tabEpg = "Guia EPG"
    override val tabGeneral = "Geral"
    override val tabSubtitles = "Áudio e legendas"
    override val tabApiKeys = "Chaves de API"
    override val tmdbHeader = "The Movie Database (TMDB)"
    override val tmdbEnabled = "Ativar Metadados TMDB"
    override val tmdbApiKey = "Chave de API / Token TMDB"
    override val tmdbApiKeyHint = "Suporta chave v3 ou Token de Leitura v4"
    override val tmdbLanguage = "Idioma dos Metadados"
    override val tmdbLanguageDesc = "Idioma preferencial para sinopses, títulos e descrições"
    override val tmdbLanguageAuto = "Automático (Seguir Perfil)"
    override val tmdbTestConnection = "Testar Ligação"
    override val tmdbConnectionSuccess = "Ligação com sucesso"
    override val tmdbConnectionFailed = "Falha na ligação"
    override val tmdbInfoHelp = "Obtenha uma chave gratuita da API TMDB ou Token de Leitura em themoviedb.org"
    override val tmdbInfoLink = "https://www.themoviedb.org/settings/api"
    override val omdbHeader = "Open Movie Database (OMDb)"
    override val omdbEnabled = "Ativar Classificações OMDb"
    override val omdbApiKey = "Chave de API OMDb"
    override val omdbApiKeyHint = "Introduza a sua chave gratuita de 8 caracteres da API OMDb"
    override val omdbTestConnection = "Testar Ligação"
    override val omdbConnectionSuccess = "Ligação com sucesso"
    override val omdbConnectionFailed = "Falha na ligação"
    override val omdbInfoHelp = "Obtenha uma chave gratuita pessoal da API OMDb em omdbapi.com"
    override val omdbInfoLink = "https://www.omdbapi.com/apikey.aspx"
    override val imdbRating = "IMDb"
    override val rottenTomatoes = "Rotten Tomatoes"
    override val metascore = "Metascore"
    override val awards = "Prémios"
    override val topRatedFilter = "Mais Votados (7.5+)"
    override val freshFilter = "Frescos (60%+)"
    override val skipIntro = "Saltar Introdução"
    override val skipRecap = "Saltar Resumo"
    override val skipCredits = "Saltar Créditos"
    override val skipPreview = "Saltar Pré-visualização"
    override val introDbHeader = "TheIntroDB (Saltar Introdução / Resumo / Créditos)"
    override val introDbEnabled = "Ativar Botões de Salto"
    override val introDbEnabledDesc = "Apresenta botões para saltar introdução, resumo e créditos em episódios de séries"
    override val introDbAutoSkipIntro = "Saltar Introdução Automaticamente"
    override val introDbAutoSkipIntroDesc = "Avança as introduções automaticamente sem confirmação"
    override val introDbAutoSkipRecap = "Saltar Resumo Automaticamente"
    override val introDbAutoSkipRecapDesc = "Avança os resumos de episódios anteriores automaticamente"
    override val traktHeader = "Trakt.tv"
    override val traktDesc = "Sincronize o histórico, progresso de reprodução e scrobble VOD automaticamente."
    override val traktClientId = "ID de Cliente Trakt"
    override val traktClientIdHint = "Introduza o ID de Cliente da sua aplicação Trakt"
    override val traktClientIdLengthWarning = "O ID de Cliente Trakt é demasiado curto (atualmente %d carateres)"
    override val traktConnect = "Ligar ao Trakt"
    override val traktDisconnect = "Desligar"
    override val traktConnectedAs = "Ligado como"
    override val apiServiceConnected = "Ligado"
    override val apiServiceDisconnected = "Desligado"
    override val apiServiceConnect = "Ligar"
    override val traktVipBadge = "VIP"
    override val traktScrobble = "Scrobble de Reprodução VOD"
    override val traktScrobbleDesc = "Atualiza automaticamente o estado de visualização no Trakt ao ver filmes e séries"
    override val traktPairingTitle = "Ligar ao Trakt"
    override val traktPairingInstructions = "Digitalize o código QR com o telemóvel ou aceda à ligação abaixo e introduza o código:"
    override val traktPairingLink = "https://trakt.tv/activate"
    override val traktPairingGenerating = "A gerar código de emparelhamento..."
    override val traktPairingWaiting = "A aguardar autorização no Trakt..."
    override val traktPairingExpiresIn = "O código expira em %s"
    override val traktPairingExpired = "O código de emparelhamento expirou"
    override val traktPairingDenied = "A autorização foi recusada no Trakt"
    override val traktPairingSuccess = "Ligado ao Trakt com sucesso!"
    override val traktPairingRetry = "Tentar Novamente"
    override val traktHelp = "Obtenha um ID de Cliente Trakt gratuito em trakt.tv/oauth/applications"
    override val traktHelpLink = "https://trakt.tv/oauth/applications"
    override val sectionTraktWatchlist = "Lista do Trakt"
    override val sectionTraktRecommendations = "Recomendados para Si"
    override val traktWatchlist = "Lista do Trakt"
    override val traktWatchlistEmpty = "Nenhum item da sua lista do Trakt corresponde às fontes atuais."
    override val traktSyncWatchlist = "Sincronizar Lista"
    override val traktSyncingWatchlist = "A sincronizar com o Trakt..."
    override val traktWatchlistSynced = "Lista sincronizada (%d itens)"
    override val traktAddToWatchlist = "Adicionar à Lista do Trakt"
    override val traktRemoveFromWatchlist = "Remover da Lista do Trakt"
    override val traktWatchedHistory = "Histórico de Visualizações"
    override val traktSyncWatchedHistory = "Importar do Trakt"
    override val traktSyncingWatchedHistory = "A sincronizar histórico com o Trakt..."
    override val traktWatchedHistorySynced = "Histórico sincronizado (%d itens correspondidos)"
    override val traktWatchedHistoryEmpty = "Nenhum item visto sincronizado ainda."
    override val traktRecommendations = "Recomendados para Si"
    override val traktRecommendationsEmpty = "Nenhuma recomendação corresponde às suas fontes atuais."
    override val traktIncludesTrending = "Inclui títulos em destaque no Trakt"
    override val traktSeriesRecommendationsEmpty = "As recomendações de séries aparecem quando há títulos do Trakt na sua biblioteca."
    override val traktTrendingSeries = "Séries em destaque no Trakt"
    override val traktSyncRecommendations = "Sincronizar Recomendações"
    override val traktSyncingRecommendations = "A sincronizar recomendações com o Trakt..."
    override val traktRecommendationsSynced = "Recomendações sincronizadas (%d itens)"
    override val traktHideRecommendation = "Ocultar no Trakt"
    override val traktHideRecommendationConfirmation = "Ocultar este título das suas recomendações no Trakt? Pode recuperá-lo em Recomendações ocultas, no Início."
    override val traktHiddenRecommendations = "Recomendações ocultas"
    override val traktNoHiddenRecommendations = "Não tem recomendações ocultas no Trakt."
    override val traktRestoreRecommendation = "Restaurar"
    override val traktRecommendationActionFailed = "O Trakt não conseguiu atualizar esta recomendação. Tente novamente."
    override val traktHiddenRecommendationsFailed = "Não foi possível carregar as recomendações ocultas do Trakt."
    override val traktLoadMoreHiddenRecommendations = "Carregar mais"
    override val traktHidingRecommendation = "A ocultar recomendação..."
    override val watchTrailer = "Ver Trailer"
    override val openInYouTube = "Abrir no YouTube"
    override val openTrailerExternally = "Abrir trailer noutra aplicação"
    override val trailerUnavailableInApp = "Não é possível reproduzir este trailer no FortressTV."
    override val trailerLinkOpenFailed = "Não foi possível abrir a ligação do trailer."
    override val genres = "Géneros"
    override val tabAbout = "Sobre"
    override val language = "Idioma"
    override val playback = "Reprodução"
    override val playbackMode = "Estabilidade da reprodução"
    override val playbackModeDesc = "Escolha o equilíbrio entre atraso da emissão, estabilidade e utilização de dados móveis."
    override val playbackModeAuto = "Automático"
    override val playbackModeStable = "Estável"
    override val playbackModeDataSaver = "Poupança de dados"
    override val playChannelsFullscreen = "Abrir canais em ecrã inteiro"
    override val playChannelsFullscreenDesc = "Entrar imediatamente em ecrã inteiro ao selecionar um canal."
    override val autoPictureInPicture = "Imagem em imagem"
    override val autoPictureInPictureDesc = "Manter o vídeo em ecrã inteiro numa janela flutuante ao premir o botão Início."
    override val anonymousTelemetry = "Dados anónimos de qualidade de reprodução"
    override val anonymousTelemetryDesc = "Ajuda a diagnosticar problemas de ligação e a ajustar a estabilidade de transmissão."
    override val healthStable = "Estável"
    override val healthFair = "Moderada"
    override val healthUnstable = "Instável"
    override val healthTesting = "A testar..."
    override val connectionHealth = "Qualidade da ligação"
    override val startupLatency = "Tempo de arranque"
    override val bufferStalls = "Interrupções"
    override val testRegion = "Região"
    override val addNewPlaylist = "Adicionar Nova Lista"
    override val addPlaylist = "Adicionar Lista"
    override val playlistName = "Nome da Lista"
    override val playlistNamePlaceholder = "ex: Desporto, Família, Filmes"
    override val sourceType = "Tipo de Origem"
    override val sourceRole = "Conteúdo da biblioteca"
    override val roleAutomatic = "Automático"
    override val roleLiveTv = "TV em direto"
    override val roleMovies = "Filmes"
    override val roleSeries = "Séries"
    override val roleMoviesAndSeries = "Filmes e Séries"
    override val roleEverything = "Tudo"
    override val remoteUrl = "URL Remoto (HTTP/HTTPS)"
    override val localFile = "Ficheiro Local (M3U/M3U8)"
    override val xtreamCodes = "Xtream Codes"
    override val xtreamServer = "URL do servidor"
    override val username = "Nome de utilizador"
    override val password = "Palavra-passe"
    override val connect = "Ligar"
    override val selectFile = "Selecionar Ficheiro"
    override val deletePlaylistConfirmTitle = "Eliminar Lista"
    override fun deletePlaylistConfirmMsg(name: String) = "Tem a certeza de que deseja eliminar \"$name\"? Esta ação não pode ser anulada."
    override fun lastUpdated(time: String) = "Última atualização: $time"
    override val updatedJustNow = "Atualizado agora"
    override fun updatedMinutesAgo(minutes: Long) = "Atualizado há ${minutes} min"
    override fun updatedHoursAgo(hours: Long) = "Atualizado há ${hours} h"
    override fun updatedAt(time: String) = "Atualizado em $time"
    override val notUpdatedYet = "Ainda não atualizada"
    override val refreshPlaylist = "Atualizar lista"
    override val xmltvSource = "Origem do Guia XMLTV"
    override val noEpgConfiguredMsg = "Nenhum guia XMLTV configurado"
    override val configureXmltv = "Configurar Guia XMLTV"
    override val changeXmltv = "Alterar Guia XMLTV"
    override val removeEpg = "Remover EPG"
    override val removeEpgConfirmation = "A FortressTV irá esquecer esta fonte de EPG e limpar a programação guardada localmente. As listas não serão afetadas."
    override val battery = "Bateria"
    override val timeShift = "Correção de Horário do EPG"
    override val timeShiftDesc = "Ajuste este valor se os horários dos programas não coincidirem com o seu fuso horário local."
    override val epgScheduleCorrection = "Correção de Horário do EPG"
    override val epgScheduleCorrectionDesc = "+1 hora faz com que os programas apareçam uma hora mais tarde."
    override fun epgPreview(baseHour: String, correctedHour: String) =
        "O programa começa às $baseHour → corrigido para as $correctedHour"
    override val epgSourceTimezone = "Fuso Horário da Fonte de EPG"
    override val epgSourceTimezoneDesc = "Utilizado apenas quando a grelha XMLTV omite o fuso horário."
    override val timezoneAutoDevice = "Automático (Fuso horário do dispositivo)"
    override val directEntryMinutes = "Introdução direta (minutos)"
    override val epgManualMapping = "Associação Manual de EPG"
    override val epgManualMappingDesc = "Associe manualmente um canal IPTV a um canal do guia XMLTV."
    override val mapEpgChannel = "Associar Canal EPG"
    override val resetToAutomatic = "Repor Automático"
    override val mappingUnavailable = "Indisponível"
    override val searchXmltvChannels = "Pesquisar canais XMLTV..."
    override val noXmltvChannelsFound = "Nenhum canal XMLTV correspondente encontrado"
    override val mappedTo = "Associado a"
    override val epgLoadedDays = "Dias de EPG a Carregar"
    override val epgLoadedDaysDesc = "Número de dias do guia de programação a reter na memória e cache (1 a 7 dias)."
    override fun epgDaysCount(days: Int) = if (days == 1) "1 dia" else "$days dias"
    override val securityTitle = "Segurança e Privacidade FortressTV"
    override val securityDesc = "Encriptação AES-256 por hardware via Android KeyStore. Todas as origens e tokens de stream permanecem estritamente privados neste dispositivo."
    override val contentDisclaimerTitle = "Aviso sobre conteúdos"
    override val contentDisclaimer = "A FortressTV é um reprodutor multimédia. Não fornece, aloja, vende nem distribui listas, canais, filmes, séries, dados EPG ou qualquer outro conteúdo de streaming. Cada utilizador é responsável por adicionar apenas fontes às quais tenha acesso autorizado e por cumprir a legislação aplicável e os termos dos fornecedores."
    override val openSourceSoftware = "Software de código aberto"
    override val ffmpegAttribution = "O FortressTV utiliza o FFmpeg 6.0 sob a licença LGPL v2.1 ou posterior para descodificar áudio MPEG não suportado pelo dispositivo. O código-fonte correspondente, a licença e as instruções de compilação estão disponíveis através da ligação pública abaixo."
    override val viewSourceCode = "Ver código-fonte e licenças"
    override val appVersion = "Versão 1.0.0"
    override val noSourcesConfigured = "Nenhuma fonte configurada"
    override val addSourceGettingStarted = "Adicione o URL M3U / M3U8 ou o servidor Xtream Codes para começar."
    override fun versionLabel(version: String, code: Int) = "Versão $version ($code) · Pride1922"
    override fun installedVersion(version: String) = "Instalada: v$version"
    override val directUrlPrivacyHint = "Introduza um URL HTTP ou HTTPS direto. Será encriptado neste dispositivo."

    override val checkForUpdates = "Verificar Atualizações"
    override val checkingForUpdates = "A verificar atualizações..."
    override val updateAvailable = "Atualização Disponível"
    override fun updateAvailableDesc(version: String) = "A versão $version está disponível."
    override val upToDate = "Está a utilizar a versão mais recente"
    override val downloadingUpdate = "A transferir atualização..."
    override val updateDownloaded = "Transferência concluída"
    override val installUpdateHint = "Se fechou o instalador do Android, toque em Instalar Atualização para continuar. A atualização não será transferida novamente."
    override val updateNow = "Atualizar Agora"
    override val installUpdate = "Instalar Atualização"
    override val later = "Mais Tarde"
    override val hideAdultContent = "Ocultar Conteúdo para Adultos"
    override val hideAdultContentSubtitle = "Filtrar canais, filmes e séries para adultos (XXX / 18+)"
    override val hideAdultContentChildRestricted = "O conteúdo para adultos está permanentemente bloqueado em perfis infantis"
    override val newUpdateAvailable = "Nova Atualização Disponível"
    override fun updateFailed(error: String) = "Falha na atualização: $error"

    override val parentalControl = "Controlo Parental"
    override val parentalControlDesc = "Proteger conteúdo para adultos e definições com um PIN de 4 dígitos"
    override val setParentalPin = "Definir PIN de 4 dígitos"
    override val changeParentalPin = "Alterar PIN"
    override val removeParentalPin = "Remover PIN"
    override val enterParentalPin = "Introduza o PIN de 4 dígitos"
    override val confirmParentalPin = "Confirme o PIN de 4 dígitos"
    override val enterNewPin = "Introduza o novo PIN de 4 dígitos"
    override val pinIncorrect = "PIN incorreto. Tente novamente."
    override val pinsDoNotMatch = "Os PINs não coincidem. Tente novamente."
    override val pinSetSuccess = "PIN definido com sucesso"
    override val pinRemovedSuccess = "PIN removido com sucesso"
    override val unlockAdultContent = "Desbloquear Conteúdo para Adultos"
    override val unlockSettings = "Desbloquear Definições"

    override val quickRecents = "Canais Recentes"

    override val showPassword = "Mostrar palavra-passe"
    override val transferScanWithApp = "Leia o código com o telemóvel para abrir o FortressTV e enviar uma lista. Ambos os dispositivos têm de estar na mesma rede local."
    override val hidePassword = "Ocultar palavra-passe"

    // Transferência local (QR / telemóvel para TV)
    override val transferSendFromPhone = "Enviar do Telemóvel ou Computador"
    override val transferSendFromPhoneDesc = "Adicione uma fonte lendo o código QR com o telemóvel ou abrindo o endereço num computador"
    override val transferDialogTitle = "Adicionar Fonte pelo Telemóvel"
    override val transferScanQr = "Aponte a câmara do telemóvel ao código QR"
    override val transferOrOpenUrl = "Ou abra este endereço num browser:"
    override val transferPairingCode = "Código de emparelhamento:"
    override val transferSessionExpired = "Sessão expirada. Por favor tente novamente."
    override val transferServerStartFailed = "Não foi possível iniciar a transferência local. Verifique a ligação de rede e tente novamente."
    override val transferWaiting = "À espera de ligação…"
    override val transferReceived = "Fonte recebida — confirme abaixo"
    override val transferConfirmTitle = "Confirmar fonte"
    override val transferConfirmDesc = "Verifique a fonte abaixo antes de a adicionar à sua biblioteca."
    override val transferSourceType = "Tipo"
    override val transferSourceServer = "Servidor"
    override val transferAddSource = "Adicionar Fonte"
    override val transferSuccessMessage = "Fonte adicionada com sucesso!"
    override val transferLockedOut = "Demasiadas tentativas falhadas. Feche e tente novamente."
    override fun transferAttemptsRemaining(remaining: Int) =
        if (remaining == 1) "1 tentativa restante" else "$remaining tentativas restantes"
    override val transferNetworkError = "Não foi possível iniciar o servidor local. Verifique o Wi-Fi."
    override val transferCancelSession = "Cancelar"
    override val transferSendToTv = "Enviar para a TV"
    override val transferSendToTvDesc = "Escolha uma fonte e um dispositivo FortressTV próximo na mesma rede local."
    override val transferChooseSource = "Fonte a enviar"
    override val transferAvailableTvs = "TVs disponíveis"
    override val transferConnectedViaQr = "Ligado à TV através do código QR"
    override val transferSearchingTvs = "A procurar TVs na rede local…"
    override val transferEnterTvCode = "Código de 6 dígitos da TV"
    override val transferEnterTvCodeDesc = "Introduza o código de emparelhamento apresentado na TV."
    override val transferSendNow = "Enviar para a TV"
    override val transferSending = "A enviar…"
    override val transferSentToTv = "Fonte enviada com sucesso"
    override val transferConfirmOnTv = "Confirme a fonte na TV para concluir."
    override val transferSendFailed = "Não foi possível enviar a fonte. Verifique o código e confirme que os dispositivos estão na mesma rede."
    override val transferNoSendableSources = "Não existem fontes por URL ou Xtream disponíveis para enviar."
    override val transferSourceTab = "Lista + EPG"
    override val transferSourceExplanation = "Envie apenas a lista selecionada e a configuração de EPG associada."
    override val transferQrRequiredForSource = "O envio de uma fonte requer a leitura do código QR da TV para proteger as suas credenciais."
}

val LocalAppStrings = staticCompositionLocalOf<Strings> { EnglishStrings }

@Composable
fun getStringsFor(language: AppLanguage): Strings = when (language) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.PORTUGUESE -> PortugueseStrings
}
