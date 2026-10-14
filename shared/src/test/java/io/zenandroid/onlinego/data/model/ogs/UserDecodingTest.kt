package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.utils.appJson
import org.junit.Assert.assertEquals
import org.junit.Test

class UserDecodingTest {

  @Test
  fun testBooleanAsIntWorks() {
    val user = appJson.decodeFromString<User>(
      """
            {
              "anonymous":false,
              "id":1,
              "username":"aaa",
              "registration_date":"2014-08-02 18:13:19.269649+00:00",
              "ratings": {
                 "correspondence-9x9":{
                    "rating":1520.3359,
                    "deviation":70.7831,
                    "volatility":0.06
                 },
                 "correspondence-13x13":{
                    "rating":1594.28,
                    "deviation":175.7906,
                    "volatility":0.06
                 },
                 "correspondence-19x19":{
                    "rating":1779.1504,
                    "deviation":91.4745,
                    "volatility":0.06
                 }
              },
              "country":"gb",
              "professional":false,
              "ranking":23,
              "provisional":0,
              "pro": 1,
              "can_create_tournaments":true,
              "is_moderator":0,
              "is_superuser":false,
              "is_tournament_moderator":false,
              "supporter":true,
              "supporter_level":4,
              "tournament_admin":false,
              "ui_class":"supporter",
              "icon":"sss",
              "email":"aaa",
              "email_validated":true,
              "is_announcer":false
           }
        """.trimIndent()
    )

    assertEquals(false, user?.is_moderator)
    assertEquals(true, user?.supporter)
    assertEquals(true, user?.pro)
  }
}
