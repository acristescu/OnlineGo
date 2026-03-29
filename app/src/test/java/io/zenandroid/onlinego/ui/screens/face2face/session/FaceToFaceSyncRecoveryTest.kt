package io.zenandroid.onlinego.ui.screens.face2face.session

import org.junit.Assert.assertEquals
import org.junit.Test

class FaceToFaceSyncRecoveryTest {
  @Test
  fun `requests remote state when the incoming move is ahead of local history`() {
    assertEquals(
      FaceToFaceSyncRecoveryAction.REQUEST_REMOTE_STATE,
      resolveOutOfSyncRecovery(localMoveCount = 1, incomingMoveNumber = 3),
    )
  }

  @Test
  fun `pushes local state when the incoming move is stale or duplicated`() {
    assertEquals(
      FaceToFaceSyncRecoveryAction.PUSH_LOCAL_STATE,
      resolveOutOfSyncRecovery(localMoveCount = 3, incomingMoveNumber = 3),
    )
    assertEquals(
      FaceToFaceSyncRecoveryAction.PUSH_LOCAL_STATE,
      resolveOutOfSyncRecovery(localMoveCount = 4, incomingMoveNumber = 2),
    )
  }
}
