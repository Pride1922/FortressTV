package com.fortresstv.player

import android.content.Context
import android.os.Trace
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.fortresstv.player.account.AccountRepository
import com.fortresstv.player.epg.EpgMappingStore
import com.fortresstv.player.introdb.TheIntroDbSettings
import com.fortresstv.player.introdb.TheIntroDbSettingsStore
import com.fortresstv.player.library.CloudLibrarySyncService
import com.fortresstv.player.library.UserLibraryStore
import com.fortresstv.player.library.VodProgressStore
import com.fortresstv.player.library.profileProgressNamespace
import com.fortresstv.player.omdb.OmdbSettings
import com.fortresstv.player.omdb.OmdbSettingsStore
import com.fortresstv.player.profile.HouseholdProfile
import com.fortresstv.player.profile.ProfileRepository
import com.fortresstv.player.source.CloudSourceSyncService
import com.fortresstv.player.source.SourceSettingsStore
import com.fortresstv.player.sports.SportsRepository
import com.fortresstv.player.subtitles.OpenSubtitlesSettings
import com.fortresstv.player.subtitles.OpenSubtitlesSettingsStore
import com.fortresstv.player.telemetry.ChannelHealthRepository
import com.fortresstv.player.tmdb.TmdbSettings
import com.fortresstv.player.tmdb.TmdbSettingsStore
import com.fortresstv.player.trakt.TraktRecommendationsStore
import com.fortresstv.player.trakt.TraktSettingsStore
import com.fortresstv.player.trakt.TraktWatchlistStore
import com.fortresstv.player.ui.AppLanguage
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.getStringsFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Values that need an Android Keystore round trip to read. They are decrypted once on the IO
 * thread and handed to [FortressTVApp] as the initial value of the matching state, so the first
 * composition does not repeat the decryption on the main thread.
 *
 * A null settings field means "not read here" (the step failed): the composable then reads the
 * store itself, exactly as it did before this class existed.
 */
internal class StartupWarmupState(
    val openSubtitlesSettings: OpenSubtitlesSettings? = null,
    val tmdbSettings: TmdbSettings? = null,
    val introDbSettings: TheIntroDbSettings? = null,
    val omdbSettings: OmdbSettings? = null,
    /** False when the PIN could not be read here; [parentalPin] is then meaningless. */
    val parentalPinRead: Boolean = false,
    val parentalPin: String? = null
)

/**
 * Does, on a background thread, the disk work the first composition of [FortressTVApp] used to do
 * on the main thread.
 *
 * Nothing here changes what is stored or how it is read: it only makes sure that each
 * SharedPreferences file the start-up path touches is opened and fully loaded, and each
 * process-wide singleton is created, before the UI asks for them. Android keeps one
 * SharedPreferences object per file for the whole process, so once a file is loaded here the
 * later reads on the main thread are served from memory.
 *
 * Every step is isolated: if one throws, it is logged and skipped, and the UI falls back to
 * reading that store itself the way it always did. [run] never throws.
 */
internal object StartupWarmup {
    private const val TAG = "FortressTV"

    /** Preferences file of the one-off "local library to first profile" migration in [FortressTVApp]. */
    const val LEGACY_MIGRATION_PREFS = "fortress_legacy_migration"

    /** Preferences file of [com.fortresstv.player.account.LocalDataMigrationService]. */
    private const val DATA_MIGRATION_PREFS = "fortress_data_migrations"

    private inline fun <T> step(name: String, block: () -> T): T? = try {
        block()
    } catch (failure: Throwable) {
        Log.w(TAG, "Startup warm-up step '$name' failed; the UI will load it itself", failure)
        null
    }

    fun run(appContext: Context): StartupWarmupState {
        Trace.beginSection("FortressTV.startupWarmup")
        try {
            // Account session: preferences file plus a Keystore decryption when signed in.
            val userId = step("account") { AccountRepository.get(appContext).currentUserId() }

            // Household profiles and the parental PIN store (both are opened by the constructor).
            val profileRepository = step("profiles") { ProfileRepository.get(appContext) }
            val profileId = profileRepository?.activeProfile?.value?.id
            step("pin status") { profileRepository?.getPinStatus() }

            // fortress_sources: start destination, home layout, languages, parental settings.
            val settings = step("settings") { SourceSettingsStore(appContext) }
            step("settings load") { settings?.telemetryEnabled() }
            var parentalPinRead = false
            val parentalPin = step("parental pin") {
                settings?.let { store -> store.getParentalPin().also { parentalPinRead = true } }
            }

            // Library and watch progress of the profile that is about to be shown.
            val namespace = profileId?.let { profileProgressNamespace(userId, it) }
            if (namespace != null) {
                step("library") { UserLibraryStore(appContext, namespace).favoriteChannelIds() }
                step("progress") { VodProgressStore(appContext, namespace).isDismissed("startup-warmup") }
            }

            // API keys: each read is a preferences file plus a Keystore decryption.
            val openSubtitles = step("opensubtitles") { OpenSubtitlesSettingsStore(appContext).loadSettings() }
            val tmdb = step("tmdb") { TmdbSettingsStore(appContext).loadSettings() }
            val introDb = step("introdb") { TheIntroDbSettingsStore(appContext).loadSettings() }
            val omdb = step("omdb") { OmdbSettingsStore(appContext).loadSettings() }

            step("epg mappings") { EpgMappingStore(appContext).getAllMappings() }

            // Cloud sync singletons (sync outbox and telemetry queue files).
            step("cloud sources") { CloudSourceSyncService.get(appContext) }
            step("cloud library") { CloudLibrarySyncService.get(appContext) }

            // Flags of the two one-off migrations checked right after the first frame. Only the
            // files are loaded here; the migrations themselves still run where they always did.
            val legacyMigrationPending = step("legacy migration flag") {
                val flags = appContext.getSharedPreferences(LEGACY_MIGRATION_PREFS, Context.MODE_PRIVATE)
                profileId != null && !flags.getBoolean("legacy_local_migrated_v1_$profileId", false)
            } ?: false
            step("data migration flags") {
                appContext.getSharedPreferences(DATA_MIGRATION_PREFS, Context.MODE_PRIVATE).all
            }
            if (legacyMigrationPending && namespace != null && namespace != "local") {
                // The legacy migration may still have to copy the pre-profile ("local") library.
                step("local library") { UserLibraryStore(appContext, "local").favoriteChannelIds() }
                step("local progress") { VodProgressStore(appContext, "local").isDismissed("startup-warmup") }
            }

            // Opened by the Home screen a moment later. Both singletons read their file when
            // they are created; the Trakt files are loaded with a plain (not encrypted) read.
            step("sports") { SportsRepository.get(appContext) }
            step("channel health") { ChannelHealthRepository.get(appContext) }
            if (namespace != null) {
                step("trakt settings") { TraktSettingsStore(appContext, namespace).isScrobbleEnabled() }
                step("trakt watchlist") { TraktWatchlistStore(appContext, namespace).lastSyncedEpochMs() }
                step("trakt recommendations") {
                    TraktRecommendationsStore(appContext, namespace).lastSyncedEpochMs()
                }
            }

            return StartupWarmupState(
                openSubtitlesSettings = openSubtitles,
                tmdbSettings = tmdb,
                introDbSettings = introDb,
                omdbSettings = omdb,
                parentalPinRead = parentalPinRead,
                parentalPin = parentalPin
            )
        } catch (failure: Throwable) {
            // Defensive only: every step above already catches its own failure.
            Log.w(TAG, "Startup warm-up failed; the UI will load everything itself", failure)
            return StartupWarmupState()
        } finally {
            Trace.endSection()
        }
    }
}

/**
 * Root of the UI. Shows the normal loading screen while [StartupWarmup] runs on the IO thread,
 * then hands over to [FortressTVApp].
 *
 * The app is never composed before the account session and the active profile are in memory, so
 * a signed-in user cannot see the sign-in screen, and nobody sees another profile's content,
 * while the data is still loading.
 */
@Composable
internal fun FortressTVStartup(
    transferDeepLink: String?,
    onTransferDeepLinkConsumed: () -> Unit,
    foregroundGeneration: Int,
    inactivityLockTriggered: Boolean,
    onInactivityLockConsumed: () -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val warmup by produceState<StartupWarmupState?>(null, appContext) {
        value = withContext(Dispatchers.IO) { StartupWarmup.run(appContext) }
    }
    val ready = warmup
    if (ready == null) {
        // Same screen, language rule and text FortressTVApp shows while sources load. The profile
        // is not known yet, so the neutral placeholder is used: it shows the app name.
        val strings = getStringsFor(AppLanguage.fromSystemLocale())
        CompositionLocalProvider(LocalAppStrings provides strings) {
            ProfileLoadingScreen(
                profile = HouseholdProfile.restrictedDefault(),
                statusText = strings.loadingPlaylistAndSettings
            )
        }
    } else {
        FortressTVApp(
            transferDeepLink = transferDeepLink,
            onTransferDeepLinkConsumed = onTransferDeepLinkConsumed,
            foregroundGeneration = foregroundGeneration,
            inactivityLockTriggered = inactivityLockTriggered,
            onInactivityLockConsumed = onInactivityLockConsumed,
            startupWarmup = ready
        )
    }
}
