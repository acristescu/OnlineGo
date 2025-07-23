package io.zenandroid.onlinego.data.repositories

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.ui.screens.settings.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.settingsDataStore by preferencesDataStore(
  name = "settings",
  produceMigrations = { context ->
    listOf(
      SharedPreferencesMigration(
        context = context,
        sharedPreferencesName = "io.zenandroid.onlinego_preferences"
      )
    )
  }
)

class SettingsRepository(
  private val context: Context,
  private val applicationScope: CoroutineScope
) {

  private val dataStore = context.settingsDataStore
  var cachedUserSettings: UserSettings = UserSettings()
    private set

  companion object {
    private val APP_THEME = stringPreferencesKey("app_theme")
    private val BOARD_THEME = stringPreferencesKey("board_theme")
    private val SHOW_RANKS = booleanPreferencesKey("show_ranks")
    private val SHOW_COORDINATES = booleanPreferencesKey("show_coordinates")
    private val SOUND = booleanPreferencesKey("sound")
    private val GRAPH_BY_GAMES = booleanPreferencesKey("graph_by_games")

    private val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")

    private val SEARCH_GAME_SMALL = booleanPreferencesKey("SEARCH_GAME_SMALL")
    private val SEARCH_GAME_MEDIUM = booleanPreferencesKey("SEARCH_GAME_MEDIUM")
    private val SEARCH_GAME_LARGE = booleanPreferencesKey("SEARCH_GAME_LARGE")
    private val SEARCH_GAME_SPEEDS = stringPreferencesKey("SEARCH_GAME_SPEEDS")

    private val COMPLETED_TUTORIALS_KEY = stringSetPreferencesKey("COMPLETED_TUTORIALS_KEY")
    private val AI_GAME_STATE_KEY = stringPreferencesKey("AIGAME_STATE_KEY")
    private val OGS_EBI_KEY = stringPreferencesKey("OGS_EBI")
    private val NEW_CHALLENGE_PARAMS_KEY = stringPreferencesKey("PARAMS")
    private val FACE_TO_FACE_HISTORY_KEY = stringPreferencesKey("FACE_TO_FACE_HISTORY_KEY")
    private val FACE_TO_FACE_BOARD_SIZE_KEY = stringPreferencesKey("FACE_TO_FACE_BOARD_SIZE_KEY")
    private val FACE_TO_FACE_HANDICAP_KEY = intPreferencesKey("FACE_TO_FACE_HANDICAP_KEY")
  }

  init {
    applicationScope.launch(Dispatchers.IO) {
      val prefs = dataStore.data.first()

      cachedUserSettings = UserSettings(
        theme = prefs[APP_THEME] ?: "System default",
        boardTheme = BoardTheme.entries.find {
          it.name == (prefs[BOARD_THEME] ?: BoardTheme.WOOD.name)
        }
          ?: BoardTheme.WOOD,
        showRanks = prefs[SHOW_RANKS] ?: true,
        showCoordinates = prefs[SHOW_COORDINATES] ?: false,
        soundEnabled = prefs[SOUND] ?: true,
        graphByGames = prefs[GRAPH_BY_GAMES] ?: false
      )
    }
  }

  val appThemeFlow: Flow<String> = dataStore.data
    .map { prefs -> prefs[APP_THEME] ?: "System default" }

  suspend fun setAppTheme(value: String) {
    dataStore.edit { it[APP_THEME] = value }
  }

  val boardThemeFlow: Flow<BoardTheme> = dataStore.data
    .map { prefs ->
      val raw = prefs[BOARD_THEME] ?: BoardTheme.WOOD.name
      BoardTheme.entries.find { it.name == raw } ?: BoardTheme.WOOD
    }

  suspend fun setBoardTheme(theme: BoardTheme) {
    dataStore.edit { it[BOARD_THEME] = theme.name }
  }

  val showRanksFlow: Flow<Boolean> = dataStore.data
    .map { prefs -> prefs[SHOW_RANKS] ?: true }

  suspend fun setShowRanks(value: Boolean) {
    dataStore.edit { it[SHOW_RANKS] = value }
  }

  val showCoordinatesFlow: Flow<Boolean> = dataStore.data
    .map { prefs -> prefs[SHOW_COORDINATES] ?: false }

  suspend fun setShowCoordinates(value: Boolean) {
    dataStore.edit { it[SHOW_COORDINATES] = value }
  }

  val soundFlow: Flow<Boolean> = dataStore.data
    .map { prefs -> prefs[SOUND] ?: true }

  suspend fun setSound(value: Boolean) {
    dataStore.edit { it[SOUND] = value }
  }

  val graphByGamesFlow: Flow<Boolean> = dataStore.data
    .map { prefs -> prefs[GRAPH_BY_GAMES] ?: false }

  suspend fun setGraphByGames(value: Boolean) {
    dataStore.edit { it[GRAPH_BY_GAMES] = value }
  }

  val hasCompletedOnboardingFlow: Flow<Boolean> = dataStore.data
    .map { prefs -> prefs[HAS_COMPLETED_ONBOARDING] ?: false }

  suspend fun setHasCompletedOnboarding(value: Boolean) {
    dataStore.edit { it[HAS_COMPLETED_ONBOARDING] = value }
  }

  val searchGameSmall: Flow<Boolean> = dataStore.data
    .map { prefs -> prefs[SEARCH_GAME_SMALL] ?: true }

  val searchGameMedium: Flow<Boolean> = dataStore.data
    .map { prefs -> prefs[SEARCH_GAME_MEDIUM] ?: false }

  val searchGameLarge: Flow<Boolean> = dataStore.data
    .map { prefs -> prefs[SEARCH_GAME_LARGE] ?: false }

  val searchGameSpeeds: Flow<List<Speed>> = dataStore.data
    .map { prefs -> prefs[SEARCH_GAME_SPEEDS] ?: "BLITZ,RAPID,LIVE" }
    .map { list ->
      list.split(",")
        .filter { it.isNotBlank() }
        .map {
          Speed.valueOf(it.trim())
        }
    }

  suspend fun setSearchGameSmall(value: Boolean) {
    dataStore.edit { it[SEARCH_GAME_SMALL] = value }
  }

  suspend fun setSearchGameMedium(value: Boolean) {
    dataStore.edit { it[SEARCH_GAME_MEDIUM] = value }
  }

  suspend fun setSearchGameLarge(value: Boolean) {
    dataStore.edit { it[SEARCH_GAME_LARGE] = value }
  }

  suspend fun setSearchGameSpeeds(speeds: List<Speed>) {
    dataStore.edit { it[SEARCH_GAME_SPEEDS] = speeds.joinToString { it.toString() } }
  }

  val completedTutorialsFlow: Flow<Set<String>> = dataStore.data
    .map { prefs ->
      prefs[COMPLETED_TUTORIALS_KEY] ?: emptySet()
    }

  suspend fun setCompletedTutorials(completed: Set<String>) {
    dataStore.edit { it[COMPLETED_TUTORIALS_KEY] = completed }
  }

  val aiGameStateFlow: Flow<String?> = dataStore.data.map { prefs -> prefs[AI_GAME_STATE_KEY] }

  suspend fun setAiGameState(value: String) {
    dataStore.edit { it[AI_GAME_STATE_KEY] = value }
  }

  val ogsEbiFlow: Flow<String?> = dataStore.data.map { prefs -> prefs[OGS_EBI_KEY] }

  suspend fun setOgsEbi(value: String) {
    dataStore.edit { it[OGS_EBI_KEY] = value }
  }

  val newChallengeParamsFlow: Flow<String?> = dataStore.data.map { prefs -> prefs[NEW_CHALLENGE_PARAMS_KEY] }

  suspend fun setNewChallengeParams(value: String) {
    dataStore.edit { it[NEW_CHALLENGE_PARAMS_KEY] = value }
  }

  val faceToFaceHistoryFlow: Flow<String?> = dataStore.data.map { prefs -> prefs[FACE_TO_FACE_HISTORY_KEY] }
  val faceToFaceBoardSizeFlow: Flow<String?> = dataStore.data.map { prefs -> prefs[FACE_TO_FACE_BOARD_SIZE_KEY] }
  val faceToFaceHandicapFlow: Flow<Int?> = dataStore.data.map { prefs -> prefs[FACE_TO_FACE_HANDICAP_KEY] }

  suspend fun setFaceToFaceHistory(value: String) {
    dataStore.edit { it[FACE_TO_FACE_HISTORY_KEY] = value }
  }
  suspend fun setFaceToFaceBoardSize(value: String) {
    dataStore.edit { it[FACE_TO_FACE_BOARD_SIZE_KEY] = value }
  }
  suspend fun setFaceToFaceHandicap(value: Int) {
    dataStore.edit { it[FACE_TO_FACE_HANDICAP_KEY] = value }
  }
}
