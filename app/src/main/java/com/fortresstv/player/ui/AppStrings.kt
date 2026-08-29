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
    val appName: String
    val allChannels: String
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
    val rename: String
    val active: String
    val switchAction: String
    val back: String
    val fullscreen: String

    val collapseSidebar: String
    val expandSidebar: String
    val switchPlaylist: String
    val epgNotConfigured: String
    fun epgMatched(matched: Int, total: Int): String
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
    val restoringSecureSession: String
    val selectM3uError: String

    val settingsTitle: String
    val tabPlaylists: String
    val tabEpg: String
    val tabGeneral: String
    val language: String
    val addNewPlaylist: String
    val addPlaylist: String
    val playlistName: String
    val playlistNamePlaceholder: String
    val sourceType: String
    val remoteUrl: String
    val localFile: String
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
    val appVersion: String
    val checkForUpdates: String
    val checkingForUpdates: String
    val updateAvailable: String
    fun updateAvailableDesc(version: String): String
    val upToDate: String
    val downloadingUpdate: String
    val updateNow: String
    val installUpdate: String
    val later: String
    val newUpdateAvailable: String
    fun updateFailed(error: String): String
}

object EnglishStrings : Strings {
    override val appName = "FortressTV"
    override val allChannels = "All channels"
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
    override val rename = "Rename"
    override val active = "Active"
    override val switchAction = "Switch"
    override val back = "Back"
    override val fullscreen = "Fullscreen"

    override val collapseSidebar = "Collapse sidebar"
    override val expandSidebar = "Expand sidebar"
    override val switchPlaylist = "Switch playlist"
    override val epgNotConfigured = "EPG not configured"
    override fun epgMatched(matched: Int, total: Int) = "EPG: $matched/$total matched"
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
    override val restoringSecureSession = "Restoring secure session..."
    override val selectM3uError = "Please enter an M3U URL or select a local file."

    override val settingsTitle = "Settings"
    override val tabPlaylists = "Playlists"
    override val tabEpg = "Programme Guide (EPG)"
    override val tabGeneral = "General & About"
    override val language = "Language"
    override val addNewPlaylist = "Add New Playlist"
    override val addPlaylist = "Add Playlist"
    override val playlistName = "Playlist Name"
    override val playlistNamePlaceholder = "e.g. Sports, Family, Movies"
    override val sourceType = "Source Type"
    override val remoteUrl = "Remote URL (HTTP/HTTPS)"
    override val localFile = "Local File (M3U/M3U8)"
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
    override val appVersion = "Version 1.0.0"

    override val checkForUpdates = "Check for Updates"
    override val checkingForUpdates = "Checking for updates..."
    override val updateAvailable = "Update Available"
    override fun updateAvailableDesc(version: String) = "Version $version is available."
    override val upToDate = "You are using the latest version"
    override val downloadingUpdate = "Downloading update..."
    override val updateNow = "Update Now"
    override val installUpdate = "Install Update"
    override val later = "Later"
    override val newUpdateAvailable = "New Update Available"
    override fun updateFailed(error: String) = "Update failed: $error"
}

object PortugueseStrings : Strings {
    override val appName = "FortressTV"
    override val allChannels = "Todos os canais"
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
    override val rename = "Mudar o nome"
    override val active = "Ativo"
    override val switchAction = "Mudar"
    override val back = "Voltar"
    override val fullscreen = "Ecrã inteiro"

    override val collapseSidebar = "Ocultar barra lateral"
    override val expandSidebar = "Expandir barra lateral"
    override val switchPlaylist = "Mudar de lista"
    override val epgNotConfigured = "EPG não configurado"
    override fun epgMatched(matched: Int, total: Int) = "EPG: $matched/$total correspondidos"
    override val xmltvGuide = "Guia XMLTV"
    override val now = "A DAR"
    override val next = "SEGUINTE"
    override val whatsNext = "A SEGUIR"
    override fun endsAt(time: String) = "Termina às $time"
    override fun until(time: String) = "até às $time"
    override val noCurrentProgramme = "Sem informações do programa atual"
    override val noUpcomingProgramme = "Sem informações de programas futuros"
    override val noEpgId = "Sem ID de EPG"
    override val noEpgData = "Sem dados de EPG"
    override val programmeGuide = "Guia de programação"
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
    override val restoringSecureSession = "A restaurar sessão segura..."
    override val selectM3uError = "Por favor insira um URL M3U ou selecione um ficheiro local."

    override val settingsTitle = "Definições"
    override val tabPlaylists = "Listas de Canais"
    override val tabEpg = "Guia de Programação (EPG)"
    override val tabGeneral = "Geral e Sobre"
    override val language = "Idioma"
    override val addNewPlaylist = "Adicionar Nova Lista"
    override val addPlaylist = "Adicionar Lista"
    override val playlistName = "Nome da Lista"
    override val playlistNamePlaceholder = "ex: Desporto, Família, Filmes"
    override val sourceType = "Tipo de Origem"
    override val remoteUrl = "URL Remoto (HTTP/HTTPS)"
    override val localFile = "Ficheiro Local (M3U/M3U8)"
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
    override val appVersion = "Versão 1.0.0"

    override val checkForUpdates = "Verificar Atualizações"
    override val checkingForUpdates = "A verificar atualizações..."
    override val updateAvailable = "Atualização Disponível"
    override fun updateAvailableDesc(version: String) = "A versão $version está disponível."
    override val upToDate = "Está a utilizar a versão mais recente"
    override val downloadingUpdate = "A transferir atualização..."
    override val updateNow = "Atualizar Agora"
    override val installUpdate = "Instalar Atualização"
    override val later = "Mais Tarde"
    override val newUpdateAvailable = "Nova Atualização Disponível"
    override fun updateFailed(error: String) = "Falha na atualização: $error"
}

val LocalAppStrings = staticCompositionLocalOf<Strings> { EnglishStrings }

@Composable
fun getStringsFor(language: AppLanguage): Strings = when (language) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.PORTUGUESE -> PortugueseStrings
}
