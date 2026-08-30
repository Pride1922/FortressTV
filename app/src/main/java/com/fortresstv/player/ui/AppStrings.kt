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
    val navLiveTv: String
    val navMovies: String
    val navSeries: String
    val navFavorites: String
    val comingSoon: String
    val moviesComingSoon: String
    val seriesComingSoon: String
    val noFavoriteChannels: String
    val addFavorite: String
    val removeFavorite: String
    val favoriteChannels: String
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
    val noEpisodesAvailable: String
    fun seasonLabel(season: Int): String
    fun episodeLabel(season: Int, episode: Int): String
    fun searchInGroup(group: String): String
    val searchChannels: String
    val noChannelsMatch: String
    fun channelsCount(count: Int): String
    val settings: String
    val close: String
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
    val aspectRatio: String
    val lockControls: String
    val unlockControls: String
    val brightness: String
    val volume: String
    val aspectFill: String
    val aspectZoom: String
    val aspectFit: String
    val playbackError: String

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
    val loadingSavedData: String
    val restoringSecureSession: String
    val selectM3uError: String

    val settingsTitle: String
    val tabPlaylists: String
    val tabEpg: String
    val tabGeneral: String
    val language: String
    val playback: String
    val playChannelsFullscreen: String
    val playChannelsFullscreenDesc: String
    val autoPictureInPicture: String
    val autoPictureInPictureDesc: String
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
    val notUpdatedYet: String
    val refreshPlaylist: String
    val xmltvSource: String
    val noEpgConfiguredMsg: String
    val configureXmltv: String
    val changeXmltv: String
    val removeEpg: String
    val timeShift: String
    val timeShiftDesc: String
    val securityTitle: String
    val securityDesc: String
    val openSourceSoftware: String
    val ffmpegAttribution: String
    val viewSourceCode: String
    val appVersion: String
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
    val newUpdateAvailable: String
    fun updateFailed(error: String): String
}

object EnglishStrings : Strings {
    override val languageTag = "en-GB"
    override val appName = "FortressTV"
    override val allChannels = "All channels"
    override val sources = "Sources"
    override val categories = "Categories"
    override val uncategorized = "Uncategorized"
    override val navLiveTv = "Live TV"
    override val navMovies = "Movies"
    override val navSeries = "Series"
    override val navFavorites = "Favorites"
    override val comingSoon = "Coming next"
    override val moviesComingSoon = "Your movie library will appear here when a VOD source is configured."
    override val seriesComingSoon = "Your series, seasons and episodes will appear here when a VOD source is configured."
    override val noFavoriteChannels = "Favorite channels will appear here."
    override val addFavorite = "Add to Favorites"
    override val removeFavorite = "Remove from Favorites"
    override val favoriteChannels = "Channels"
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
    override val noEpisodesAvailable = "No episodes are available for this Series."
    override fun seasonLabel(season: Int) = "Season $season"
    override fun episodeLabel(season: Int, episode: Int) = "S${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')}"
    override fun searchInGroup(group: String) = "Search in $group"
    override val searchChannels = "Search channels"
    override val noChannelsMatch = "No channels match your search."
    override fun channelsCount(count: Int) = "$count ${if (count == 1) "channel" else "channels"}"
    override val settings = "Settings"
    override val close = "Close"
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
    override val aspectRatio = "Aspect ratio"
    override val lockControls = "Lock controls"
    override val unlockControls = "Unlock controls"
    override val brightness = "Brightness"
    override val volume = "Volume"
    override val aspectFill = "Aspect: Fill"
    override val aspectZoom = "Aspect: Zoom"
    override val aspectFit = "Aspect: Fit"
    override val playbackError = "Unable to play this channel."

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
    override val loadingSavedData = "Loading saved playlist and programme guide..."
    override val restoringSecureSession = "Restoring secure session..."
    override val selectM3uError = "Please enter an M3U URL or select a local file."

    override val settingsTitle = "Settings"
    override val tabPlaylists = "Playlists"
    override val tabEpg = "Programme Guide (EPG)"
    override val tabGeneral = "General & About"
    override val language = "Language"
    override val playback = "Playback"
    override val playChannelsFullscreen = "Open channels in fullscreen"
    override val playChannelsFullscreenDesc = "Enter fullscreen immediately when you select a channel."
    override val autoPictureInPicture = "Picture-in-Picture"
    override val autoPictureInPictureDesc = "Keep fullscreen video in a floating window when you press Home."
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
    override val notUpdatedYet = "Not updated yet"
    override val refreshPlaylist = "Refresh playlist"
    override val xmltvSource = "XMLTV Guide Source"
    override val noEpgConfiguredMsg = "No XMLTV guide configured"
    override val configureXmltv = "Configure XMLTV Guide"
    override val changeXmltv = "Change XMLTV Guide"
    override val removeEpg = "Remove EPG"
    override val timeShift = "Time Shift (Hours)"
    override val timeShiftDesc = "Adjust this if programme times are offset from your local timezone."
    override val securityTitle = "FortressTV Security & Privacy"
    override val securityDesc = "Hardware-backed AES-256 encryption via Android KeyStore. All sources and cached stream tokens remain private on this device."
    override val openSourceSoftware = "Open-source software"
    override val ffmpegAttribution = "FortressTV uses FFmpeg 6.0 under the LGPL v2.1 or later to decode otherwise unsupported MPEG audio. The corresponding source and build instructions are available in the FortressTV repository."
    override val viewSourceCode = "View source and licences"
    override val appVersion = "Version 1.0.0"

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
    override val newUpdateAvailable = "New Update Available"
    override fun updateFailed(error: String) = "Update failed: $error"
}

object PortugueseStrings : Strings {
    override val languageTag = "pt-PT"
    override val appName = "FortressTV"
    override val allChannels = "Todos os canais"
    override val sources = "Fontes"
    override val categories = "Categorias"
    override val uncategorized = "Sem categoria"
    override val navLiveTv = "TV em direto"
    override val navMovies = "Filmes"
    override val navSeries = "Séries"
    override val navFavorites = "Favoritos"
    override val comingSoon = "Brevemente"
    override val moviesComingSoon = "A sua biblioteca de filmes aparecerá aqui quando configurar uma fonte VOD."
    override val seriesComingSoon = "As suas séries, temporadas e episódios aparecerão aqui quando configurar uma fonte VOD."
    override val noFavoriteChannels = "Os canais favoritos aparecerão aqui."
    override val addFavorite = "Adicionar aos Favoritos"
    override val removeFavorite = "Remover dos Favoritos"
    override val favoriteChannels = "Canais"
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
    override val noEpisodesAvailable = "Não existem episódios disponíveis para esta Série."
    override fun seasonLabel(season: Int) = "Temporada $season"
    override fun episodeLabel(season: Int, episode: Int) = "T${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')}"
    override fun searchInGroup(group: String) = "Pesquisar em $group"
    override val searchChannels = "Pesquisar canais"
    override val noChannelsMatch = "Nenhum canal corresponde à sua pesquisa."
    override fun channelsCount(count: Int) = "$count ${if (count == 1) "canal" else "canais"}"
    override val settings = "Definições"
    override val close = "Fechar"
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
    override val aspectRatio = "Proporção de ecrã"
    override val lockControls = "Bloquear controlos"
    override val unlockControls = "Desbloquear controlos"
    override val brightness = "Brilho"
    override val volume = "Volume"
    override val aspectFill = "Proporção: Preencher"
    override val aspectZoom = "Proporção: Zoom"
    override val aspectFit = "Proporção: Ajustar"
    override val playbackError = "Não foi possível reproduzir este canal."

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
    override val loadingSavedData = "A carregar a lista e o guia de programação guardados..."
    override val restoringSecureSession = "A restaurar sessão segura..."
    override val selectM3uError = "Por favor insira um URL M3U ou selecione um ficheiro local."

    override val settingsTitle = "Definições"
    override val tabPlaylists = "Listas de Canais"
    override val tabEpg = "Guia de Programação (EPG)"
    override val tabGeneral = "Geral e Sobre"
    override val language = "Idioma"
    override val playback = "Reprodução"
    override val playChannelsFullscreen = "Abrir canais em ecrã inteiro"
    override val playChannelsFullscreenDesc = "Entrar imediatamente em ecrã inteiro ao selecionar um canal."
    override val autoPictureInPicture = "Imagem em imagem"
    override val autoPictureInPictureDesc = "Manter o vídeo em ecrã inteiro numa janela flutuante ao premir o botão Início."
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
    override val notUpdatedYet = "Ainda não atualizada"
    override val refreshPlaylist = "Atualizar lista"
    override val xmltvSource = "Origem do Guia XMLTV"
    override val noEpgConfiguredMsg = "Nenhum guia XMLTV configurado"
    override val configureXmltv = "Configurar Guia XMLTV"
    override val changeXmltv = "Alterar Guia XMLTV"
    override val removeEpg = "Remover EPG"
    override val timeShift = "Ajuste de Fuso Horário (Horas)"
    override val timeShiftDesc = "Ajuste este valor se os horários dos programas não coincidirem com o seu fuso horário local."
    override val securityTitle = "Segurança e Privacidade FortressTV"
    override val securityDesc = "Encriptação AES-256 por hardware via Android KeyStore. Todas as origens e tokens de stream permanecem estritamente privados neste dispositivo."
    override val openSourceSoftware = "Software de código aberto"
    override val ffmpegAttribution = "O FortressTV utiliza o FFmpeg 6.0 sob a licença LGPL v2.1 ou posterior para descodificar áudio MPEG não suportado pelo dispositivo. O código-fonte correspondente e as instruções de compilação estão disponíveis no repositório do FortressTV."
    override val viewSourceCode = "Ver código-fonte e licenças"
    override val appVersion = "Versão 1.0.0"

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
    override val newUpdateAvailable = "Nova Atualização Disponível"
    override fun updateFailed(error: String) = "Falha na atualização: $error"
}

val LocalAppStrings = staticCompositionLocalOf<Strings> { EnglishStrings }

@Composable
fun getStringsFor(language: AppLanguage): Strings = when (language) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.PORTUGUESE -> PortugueseStrings
}
