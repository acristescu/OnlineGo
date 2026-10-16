package io.zenandroid.onlinego.utils

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AndroidNetworkErrorTest {

  @Test
  fun `socket and DNS failures are not reported`() {
    listOf(
      SocketTimeoutException(),
      SocketException(),
      ConnectException(),
      UnknownHostException(),
    ).forEach { assertNull(reportableException(it)) }
  }

  @Test
  fun `a network failure as the cause is not reported`() {
    assertNull(reportableException(Exception("request failed", UnknownHostException())))
  }

  @Test
  fun `a cancelled Google sign-in is not reported`() {
    assertNull(reportableException(ApiException(Status(12501))))
  }

  @Test
  fun `other Google API errors are reported`() {
    val t = ApiException(Status(10))
    assertSame(t, reportableException(t))
  }

  @Test
  fun `other IO errors are reported`() {
    val t = IOException("disk full")
    assertSame(t, reportableException(t))
  }
}
