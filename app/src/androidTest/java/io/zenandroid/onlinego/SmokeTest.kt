package io.zenandroid.onlinego;

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import io.zenandroid.onlinego.ui.screens.login.LoginActivity
import io.zenandroid.onlinego.utils.CountingIdlingResource
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module


@RunWith(AndroidJUnit4::class)
@LargeTest
class SmokeTest {
    @get:Rule
    var activityRule: ActivityTestRule<LoginActivity>
            = ActivityTestRule(LoginActivity::class.java)

    private val idlingResource = EspressoIdlingResource()

    @Before
    fun setup() {
        IdlingRegistry.getInstance().register(idlingResource)

        loadKoinModules(
                module{
                    single<CountingIdlingResource>(override = true) { idlingResource }
                }
        )
    }

    @Test
    fun smokeTest() {
        onView(withId(R.id.username))
                .check(matches(isDisplayed()))
        onView(withId(R.id.password))
                .check(matches(isDisplayed()))
        onView(withId(R.id.email))
                .check(matches(not(isDisplayed())))
        onView(withId(R.id.loginButton))
                .check(matches(isDisplayed()))
                .check(matches(not(isEnabled())))

        onView(withId(R.id.username))
                .perform(typeText("MrAlex-espresso-test"))

        onView(withId(R.id.loginButton))
                .check(matches(not(isEnabled())))

        onView(withId(R.id.password))
                .perform(typeText("espresso"))

        onView(withId(R.id.loginButton))
                .check(matches(isEnabled()))
                .perform(click())

        onView(withId(R.id.gamesRecycler))
                .check(matches(hasChildCount(6)))

        onView(withText("MrAlex-test"))
                .perform(click())

        onView(withText("mralex-espresso-test"))
                .check(matches(isDisplayed()))
    }


}