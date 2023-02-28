package io.zenandroid.onlinego.gamelogic

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.User
import io.zenandroid.onlinego.data.ogs.OGSBooleanJsonAdapter
import io.zenandroid.onlinego.data.ogs.OGSInstantJsonAdapter
import io.zenandroid.onlinego.di.allKoinModules
import io.zenandroid.onlinego.utils.formatMillis
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTestRule
import org.threeten.bp.Instant

class RulesManagerTest {
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.DEBUG)
        modules(allKoinModules)
    }

    // https://online-go.com/game/42316872
    val testGameString = "{\"related\":{\"reviews\":\"/api/v1/games/42316872/reviews\"},\"players\":{\"black\":{\"id\":1077471,\"username\":\"ptu540313\",\"country\":\"un\",\"icon\":\"https://secure.gravatar.com/avatar/32c738ec3e7d08dc9a20c5dcbd39db7e?s=32&d=retro\",\"ratings\":{\"version\":5,\"overall\":{\"rating\":1868.5812044017296,\"deviation\":61.23678776651379,\"volatility\":0.05996957875516362}},\"ranking\":29.38976870323232,\"professional\":false,\"ui_class\":\"\"},\"white\":{\"id\":802384,\"username\":\"doge_bot_2\",\"country\":\"_Pirate\",\"icon\":\"https://b0c2ddc39d13e1c0ddad-93a52a5bc9e7cc06050c1a999beb3694.ssl.cf1.rackcdn.com/abbe81c2fa3cd09c20ccc9ce108a8f27-32.png\",\"ratings\":{\"version\":5,\"overall\":{\"rating\":1849.6912914578688,\"deviation\":60.31347491163347,\"volatility\":0.057730252991977246}},\"ranking\":29.154549123518944,\"professional\":false,\"ui_class\":\"bot\"}},\"id\":42316872,\"name\":\"친선 대국\",\"creator\":1077471,\"mode\":\"game\",\"source\":\"play\",\"black\":1077471,\"white\":802384,\"width\":19,\"height\":19,\"rules\":\"chinese\",\"ranked\":true,\"handicap\":0,\"komi\":\"7.50\",\"time_control\":\"byoyomi\",\"black_player_rank\":0,\"black_player_rating\":\"0.000\",\"white_player_rank\":0,\"white_player_rating\":\"0.000\",\"time_per_move\":43,\"time_control_parameters\":\"{\\\"system\\\": \\\"byoyomi\\\", \\\"time_control\\\": \\\"byoyomi\\\", \\\"speed\\\": \\\"live\\\", \\\"pause_on_weekends\\\": false, \\\"main_time\\\": 1200, \\\"period_time\\\": 30, \\\"periods\\\": 5}\",\"disable_analysis\":false,\"tournament\":null,\"tournament_round\":0,\"ladder\":null,\"pause_on_weekends\":false,\"outcome\":\"8.5 points\",\"black_lost\":true,\"white_lost\":false,\"annulled\":false,\"started\":\"2022-03-24T21:00:11.253336-04:00\",\"ended\":\"2022-03-24T21:21:22.012281-04:00\",\"historical_ratings\":{\"black\":{\"id\":1077471,\"ratings\":{\"version\":5,\"overall\":{\"rating\":1880.441650390625,\"deviation\":61.282684326171875,\"volatility\":0.05996885523200035}},\"username\":\"ptu540313\",\"country\":\"un\",\"ranking\":29.38976870323232,\"professional\":false,\"icon\":\"https://secure.gravatar.com/avatar/32c738ec3e7d08dc9a20c5dcbd39db7e?s=32&d=retro\",\"ui_class\":\"\"},\"white\":{\"id\":802384,\"ratings\":{\"version\":5,\"overall\":{\"rating\":1838.1927490234375,\"deviation\":60.3684196472168,\"volatility\":0.057729605585336685}},\"username\":\"doge_bot_2\",\"country\":\"_Pirate\",\"ranking\":29.154549123518944,\"professional\":false,\"icon\":\"https://b0c2ddc39d13e1c0ddad-93a52a5bc9e7cc06050c1a999beb3694.ssl.cf1.rackcdn.com/abbe81c2fa3cd09c20ccc9ce108a8f27-32.png\",\"ui_class\":\"bot\"}},\"gamedata\":{\"white_player_id\":802384,\"black_player_id\":1077471,\"group_ids\":[],\"game_id\":42316872,\"game_name\":\"친선 대국\",\"private\":false,\"pause_on_weekends\":false,\"players\":{\"black\":{\"username\":\"ptu540313\",\"rank\":29.536244070933847,\"professional\":false,\"id\":1077471,\"accepted_stones\":\"gdndodqdrdpebfnfrffgkgahehfhphaifioipiqiajcjfjmjnjojqjrjakekfkgkhklkokrkalglolplqlrlslamgmhmnmpmrmanenfngnhninaobocofoapfpppaqbqcqfqoqcrdrfrirnrcsesfsgshsisjsks\",\"accepted_strict_seki_mode\":false},\"white\":{\"username\":\"doge_bot_2\",\"rank\":28.786970543578875,\"professional\":false,\"id\":802384,\"accepted_stones\":\"gdndodqdrdpebfnfrffgkgahehfhphaifioipiqiajcjfjmjnjojqjrjakekfkgkhklkokrkalglolplqlrlslamgmhmnmpmrmanenfngnhninaobocofoapfpppaqbqcqfqoqcrdrfrirnrcsesfsgshsisjsks\",\"accepted_strict_seki_mode\":false}},\"ranked\":true,\"disable_analysis\":false,\"handicap\":0,\"komi\":7.5,\"width\":19,\"height\":19,\"rules\":\"chinese\",\"rengo\":false,\"rengo_teams\":{\"black\":[],\"white\":[]},\"rengo_casual_mode\":false,\"time_control\":{\"system\":\"byoyomi\",\"time_control\":\"byoyomi\",\"speed\":\"live\",\"pause_on_weekends\":false,\"main_time\":1200,\"period_time\":30,\"periods\":5},\"phase\":\"finished\",\"initial_player\":\"black\",\"moves\":[[3,15,2493],[3,3,1032],[15,15,836],[16,2,1034],[3,5,812],[5,3,631],[2,3,1241],[2,2,1146],[2,4,855],[3,2,2448],[5,5,975],[6,5,1141],[5,4,11196],[6,4,3025],[6,3,743],[5,6,642],[4,6,9012],[5,7,3028],[6,6,3421],[7,6,3033],[6,7,1064],[5,8,1747],[7,7,10084],[8,4,3031],[7,5,3785],[7,4,3984],[8,6,913],[6,2,1139],[3,8,1570],[5,10,1431],[4,9,1867],[5,9,1687],[3,11,9786],[8,9,3667],[7,9,5860],[7,10,3041],[6,9,973],[6,10,834],[8,10,911],[9,9,1136],[9,7,13602],[10,11,3037],[8,11,1773],[7,12,1607],[8,12,1725],[8,13,1548],[9,12,4630],[7,13,3032],[9,13,2333],[9,14,2162],[5,12,844],[5,13,1918],[4,12,14229],[6,12,3031],[10,14,6977],[9,15,3027],[7,11,2179],[6,11,2017],[11,13,868],[11,12,835],[4,14,2729],[12,13,2561],[12,14,2341],[13,14,2174],[10,15,2100],[12,15,1941],[11,14,1522],[13,15,1354],[10,16,2359],[9,16,2201],[9,17,8299],[8,17,3031],[10,17,1438],[7,16,1343],[6,14,3771],[5,14,3048],[6,15,1671],[5,15,1510],[6,16,883],[5,16,1136],[6,17,1573],[5,17,1404],[7,17,11388],[8,18,4099],[8,14,37525],[8,15,3028],[7,14,1543],[6,13,1375],[7,15,8006],[6,18,3042],[8,16,2557],[2,16,2386],[3,16,12613],[3,17,3063],[2,15,825],[1,16,1144],[1,15,879],[0,15,836],[4,17,33013],[2,17,3029],[4,16,1202],[4,18,1341],[0,17,5661],[0,16,3029],[1,13,3465],[4,13,3030],[3,13,2162],[8,8,1990],[8,7,6730],[11,8,3032],[10,8,5968],[10,9,3027],[11,7,965],[10,6,839],[10,7,3105],[12,8,2964],[12,7,1224],[13,8,1158],[13,7,730],[14,7,1036],[14,8,1376],[14,6,1336],[13,5,6128],[13,6,3031],[11,5,769],[12,6,637],[11,6,9226],[12,5,3028],[11,4,804],[12,4,841],[11,3,2745],[12,3,2580],[14,9,12851],[13,10,3030],[14,10,2266],[13,11,2094],[12,2,8987],[13,2,3042],[10,2,780],[12,1,852],[11,2,1335],[16,7,1170],[14,11,3556],[15,13,3054],[8,5,1385],[14,12,2219],[16,11,4261],[16,12,3078],[16,8,2074],[17,8,1907],[16,9,2151],[17,7,1972],[17,12,1994],[17,13,1828],[17,10,2766],[18,12,2591],[18,11,1255],[17,11,1339],[9,4,11927],[4,4,3037],[4,5,2177],[4,7,2373],[3,7,1391],[4,10,2076],[3,10,4072],[2,9,3036],[3,9,12772],[3,4,3039],[2,6,2361],[1,5,2208],[2,5,2715],[1,3,2559],[1,4,1494],[1,2,1341],[1,6,1579],[8,2,2669],[17,12,13087],[18,13,3032],[15,11,33758],[17,11,3043],[14,16,24283],[12,17,3034],[17,12,3789],[16,16,4121],[15,12,1167],[17,11,1340],[13,12,2752],[14,13,2578],[13,9,1315],[12,10,1143],[12,9,9487],[11,9,3033],[17,12,6712],[16,13,3039],[17,9,8984],[17,11,3034],[15,8,20286],[15,10,3031],[17,12,1633],[9,3,1457],[10,3,23831],[17,11,3032],[11,10,15886],[12,11,3037],[17,12,4943],[16,15,3032],[17,11,7361],[18,9,3039],[13,1,11508],[14,1,3034],[13,3,4631],[14,2,3040],[15,7,1568],[15,6,1410],[16,6,1141],[16,5,968],[11,1,27301],[13,0,6463],[8,1,9219],[7,1,3043],[9,1,7039],[8,0,3029],[13,17,27746],[12,16,3033],[11,18,9656],[12,18,3028],[9,0,3637],[7,0,3054],[9,2,13506],[8,3,3037],[11,0,1281],[12,0,1148],[16,3,2605],[17,2,2437],[17,3,4681],[15,3,3037],[15,4,1405],[16,4,1245],[14,3,6698],[15,2,3052],[17,5,1215],[17,6,2102],[0,3,10304],[0,2,3037],[0,4,1449],[10,12,1280],[9,10,10229],[10,10,3036],[11,17,23538],[0,14,3473],[1,12,4111],[1,14,3033],[3,18,53019],[2,18,3032],[2,13,13850],[2,14,3040],[3,14,1310],[0,13,1145],[1,11,1811],[0,12,1650],[1,10,811],[0,11,2036],[1,9,915],[0,10,1144],[1,8,9800],[0,9,3041],[10,13,19966],[0,8,3267],[1,7,10013],[0,7,3031],[7,8,7062],[7,18,3035],[9,8,49238],[5,18,3035],[9,11,2014],[9,18,1859],[11,16,1238],[10,18,1263],[11,15,4848],[-1,-1,3489],[-1,-1,11426]],\"allow_self_capture\":false,\"automatic_stone_removal\":false,\"free_handicap_placement\":true,\"aga_handicap_scoring\":false,\"allow_ko\":false,\"allow_superko\":false,\"superko_algorithm\":\"csk\",\"player_pool\":{\"802384\":{\"username\":\"doge_bot_2\",\"rank\":28.786970543578875,\"professional\":false,\"id\":802384,\"accepted_stones\":\"gdndodqdrdpebfnfrffgkgahehfhphaifioipiqiajcjfjmjnjojqjrjakekfkgkhklkokrkalglolplqlrlslamgmhmnmpmrmanenfngnhninaobocofoapfpppaqbqcqfqoqcrdrfrirnrcsesfsgshsisjsks\",\"accepted_strict_seki_mode\":false},\"1077471\":{\"username\":\"ptu540313\",\"rank\":29.536244070933847,\"professional\":false,\"id\":1077471,\"accepted_stones\":\"gdndodqdrdpebfnfrffgkgahehfhphaifioipiqiajcjfjmjnjojqjrjakekfkgkhklkokrkalglolplqlrlslamgmhmnmpmrmanenfngnhninaobocofoapfpppaqbqcqfqoqcrdrfrirnrcsesfsgshsisjsks\",\"accepted_strict_seki_mode\":false}},\"score_territory\":true,\"score_territory_in_seki\":true,\"score_stones\":true,\"score_handicap\":true,\"score_prisoners\":false,\"score_passes\":true,\"white_must_pass_last\":false,\"opponent_plays_first_after_resume\":false,\"strict_seki_mode\":false,\"initial_state\":{\"black\":\"\",\"white\":\"\"},\"start_time\":1648170011,\"original_disable_analysis\":false,\"clock\":{\"game_id\":42316872,\"current_player\":802384,\"black_player_id\":1077471,\"white_player_id\":802384,\"title\":\"친선 대국\",\"last_move\":1648171281350,\"expiration\":1648172305048,\"black_time\":{\"thinking_time\":258.4450000000002,\"periods\":5,\"period_time\":30},\"white_time\":{\"thinking_time\":873.6980000000011,\"periods\":5,\"period_time\":30},\"pause_delta\":0,\"expiration_delta\":1023698,\"now\":1648171281364,\"paused_since\":1648171281350,\"stone_removal_mode\":true,\"stone_removal_expiration\":1648171581364},\"auto_score\":true,\"pause_control\":{\"stone-removal\":true},\"paused_since\":1648171281350,\"removed\":\"gdndodqdrdpebfnfrffgkgahehfhphaifioipiqiajcjfjmjnjojqjrjakekfkgkhklkokrkalglolplqlrlslamgmhmnmpmrmanenfngnhninaobocofoapfpppaqbqcqfqoqcrdrfrirnrcsesfsgshsisjsks\",\"score\":{\"white\":{\"total\":188.5,\"stones\":79,\"territory\":102,\"prisoners\":0,\"scoring_positions\":\"aabacadaeafagaabbbcbdbebfbgbecfcedoapaqarasapbqbrbsbscqdrdsdreserfsfsgshsinbhcgdhdndodneoepenfofpfqgphoipiqimjnjojpjqjrjokqkrkskolplqlrlslpmrmlkllmmnmnnoopoqorosooppprpspnqoqpqrqsqnrorprqrrrsrnsospsqsrssshaiamanahbmbobacbcccdcgcicncocpcqcrcbdddfdidjdmdpddeeegeheiemeqegfmfqfmgngogpgrgohqhrhiiliminiriijjjkjljsjkkmknkpkklmlnlkmlmomqmsmmnonpnqnrnsnnompnpqpmqqqmrms\",\"handicap\":0,\"komi\":7.5},\"black\":{\"total\":180,\"stones\":101,\"territory\":79,\"prisoners\":0,\"scoring_positions\":\"kakbkejfkfjgkgafbfagfgahehfhaieifigiajfjakekfkgkhkalelflglamgmhmanenfngnhninaobocofoapepfpaqbqcqfqbrcrdrfrirasbscsdsesfsgshsisjsksdghgchcicjckclcmdmjoipjpjqhqjalaibjblbjckclcmcadcdkdldaebecefejelecfdfefffhfiflfbgcgegggiglgbhdhghhhihjhkhlhmhnhbidihijikibjdjejgjhjbkdkikjkbldlhliljlbmemfmimjmbncndnjnknlndoeogohoiokolomobpcpdpgphpkplpdqeqgqiqkqlqarergrhrjrkrlrls\",\"handicap\":0,\"komi\":0}},\"winner\":802384,\"outcome\":\"8.5 points\",\"end_time\":1648171281},\"auth\":null,\"game_chat_auth\":\"36f581574da04990a2870adcfc888c81\",\"rengo\":false}"
    val moshi = Moshi.Builder()
        .add(java.lang.Boolean::class.java, OGSBooleanJsonAdapter())
        .add(Instant::class.java, OGSInstantJsonAdapter().nullSafe())
        .addLast(KotlinJsonAdapterFactory())
        .build()

//    @Test
    fun fullGameTest() {
        val game = Game.fromOGSGame(moshi.adapter(OGSGame::class.java).fromJson(testGameString)!!)

        val pos = RulesManager.replay(game)

        Assert.assertNotNull(pos)
        Assert.assertEquals(128, pos.whiteStones.size)
        Assert.assertEquals(132, pos.blackStones.size)
        Assert.assertEquals(11, pos.blackCaptureCount)
        Assert.assertEquals(8, pos.whiteCaptureCount)
    }

    @Test
    fun whenFormatMillisIsCalled_thenCorrectValueIsReturned() {
        val MILLIS = 1L
        val SECONDS = 1000 * MILLIS
        val MINUTES = 60 * SECONDS
        val HOURS = 60 * MINUTES
        val DAYS = 24 * HOURS
        val WEEKS = 7 * DAYS

        Assert.assertEquals("0.0s", formatMillis(49 * MILLIS))
        Assert.assertEquals("0.1s", formatMillis(51 * MILLIS))
        Assert.assertEquals("0.1s", formatMillis(120 * MILLIS))
        Assert.assertEquals("0.9s", formatMillis(949 * MILLIS))
        Assert.assertEquals("1.0s", formatMillis(951 * MILLIS))
        Assert.assertEquals("1.0s", formatMillis(1 * SECONDS))
        Assert.assertEquals("1.5s", formatMillis(1 * SECONDS + 499 * MILLIS))
        Assert.assertEquals("1.5s", formatMillis(1 * SECONDS + 501 * MILLIS))
        Assert.assertEquals("9.9s", formatMillis(9 * SECONDS + 949 * MILLIS))
        Assert.assertEquals("10.0s", formatMillis(9 * SECONDS + 951 * MILLIS))
        Assert.assertEquals("10.0s", formatMillis(10 * SECONDS))
        Assert.assertEquals("11s", formatMillis(10 * SECONDS + 499 * MILLIS))
        Assert.assertEquals("11s", formatMillis(10 * SECONDS + 501 * MILLIS))
        Assert.assertEquals("1 : 00", formatMillis(59 * SECONDS + 499 * MILLIS))
        Assert.assertEquals("1 : 00", formatMillis(59 * SECONDS + 501 * MILLIS))
        Assert.assertEquals("1 : 00", formatMillis(1 * MINUTES + 0 * SECONDS))
        Assert.assertEquals("1 : 01", formatMillis(1 * MINUTES + 0 * SECONDS + 501 * MILLIS))
        Assert.assertEquals("2 : 00", formatMillis(1 * MINUTES + 59 * SECONDS + 499 * MILLIS))
        Assert.assertEquals("2 : 00", formatMillis(1 * MINUTES + 59 * SECONDS + 501 * MILLIS))
        Assert.assertEquals("2 : 00", formatMillis(2 * MINUTES + 0 * SECONDS + 0 * MILLIS))
        Assert.assertEquals("10 : 00", formatMillis(10 * MINUTES + 0 * SECONDS + 0 * MILLIS))
        Assert.assertEquals("59 : 00", formatMillis(59 * MINUTES + 0 * SECONDS + 0 * MILLIS))
        Assert.assertEquals("1h 00m", formatMillis(59 * MINUTES + 59 * SECONDS + 999 * MILLIS))
        Assert.assertEquals("1h 00m", formatMillis(1 * HOURS + 0 * MINUTES))
        Assert.assertEquals("1h 59m", formatMillis(1 * HOURS + 59 * MINUTES + 59 * SECONDS))
        Assert.assertEquals("2h 00m", formatMillis(2 * HOURS + 0 * MINUTES))
        Assert.assertEquals("23h 59m", formatMillis(23 * HOURS + 59 * MINUTES + 59 * SECONDS))
        Assert.assertEquals("24h", formatMillis(1 * DAYS))
        Assert.assertEquals("47h", formatMillis(1 * DAYS + 23 * HOURS + 59 * MINUTES))
        Assert.assertEquals("48h", formatMillis(2 * DAYS))
        Assert.assertEquals("3 days", formatMillis(3 * DAYS))
        Assert.assertEquals("6d 23h", formatMillis(6 * DAYS + 23 * HOURS + 59 * MINUTES))
        Assert.assertEquals("7 days", formatMillis(1 * WEEKS + 10 * HOURS))
        Assert.assertEquals("7 days", formatMillis(1 * WEEKS))
        Assert.assertEquals("13 days", formatMillis(1 * WEEKS + 6 * DAYS + 23 * HOURS))
        Assert.assertEquals("14 days", formatMillis(2 * WEEKS))
    }

    @Test
    fun testBooleanAsIntWorks() {
        val moshi: Moshi = koinTestRule.koin.get()

        val user = moshi.adapter(User::class.java).fromJson("""
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

        Assert.assertEquals(false, user?.is_moderator)
        Assert.assertEquals(true, user?.supporter)
        Assert.assertEquals(true, user?.pro)
    }
}