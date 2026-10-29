package io.zenandroid.onlinego.data.ogs

import kotlin.test.Test
import kotlin.test.assertEquals

class OgsEnvironmentTest {

  @Test
  fun `committed builds talk to production`() {
    assertEquals(OgsEnvironment.PRODUCTION, OGS_ENVIRONMENT)
  }
}
