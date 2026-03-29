package io.zenandroid.onlinego.ui.screens.face2face.session

enum class FaceToFaceSyncRecoveryAction {
  REQUEST_REMOTE_STATE,
  PUSH_LOCAL_STATE,
}

fun resolveOutOfSyncRecovery(
  localMoveCount: Int,
  incomingMoveNumber: Int,
): FaceToFaceSyncRecoveryAction {
  return if (incomingMoveNumber > localMoveCount + 1) {
    FaceToFaceSyncRecoveryAction.REQUEST_REMOTE_STATE
  } else {
    FaceToFaceSyncRecoveryAction.PUSH_LOCAL_STATE
  }
}
