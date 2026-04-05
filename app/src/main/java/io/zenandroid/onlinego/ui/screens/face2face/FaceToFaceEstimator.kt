package io.zenandroid.onlinego.ui.screens.face2face

import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.gamelogic.RulesManager

interface FaceToFaceEstimator {
  suspend fun determineTerritory(position: Position): Position
}

class RulesManagerFaceToFaceEstimator : FaceToFaceEstimator {
  override suspend fun determineTerritory(position: Position): Position {
    return RulesManager.determineTerritory(position, false)
  }
}
