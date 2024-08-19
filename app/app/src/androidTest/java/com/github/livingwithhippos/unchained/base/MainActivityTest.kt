import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.MainActivity
import com.github.livingwithhippos.unchained.base.waitForView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test setup: run this once and then edit the generated configuration:
 * 1. Add your private token as an Instrumentation argument called TOKEN (maybe don't use your main
 *    account)
 * 2. Add in the before launch an adb clear data to remove previous logins etc. Select adb as
 *    executable and add the parameters "shell pm clear com.github.livingwithhippos.unchained.debug"
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule @JvmField var rule = ActivityScenarioRule(MainActivity::class.java)

    /*

    @Test
    fun mainActivityTest() {
            launchActivity<MainActivity>().use { scenario ->
                scenario.moveToState(Lifecycle.State.CREATED)
            }
        }

     */

    @Test
    fun loginTest() {
        // if the app is already logged in this is going to fail, clear it with adb before launching
        // set the TOKEN arguments editing the MainActivityTest Configuration
        val args = InstrumentationRegistry.getArguments().getString("TOKEN")
        assert(args != null)
        onView(withId(R.id.tiPrivateCode)).perform(replaceText(args!!))
        onView(withId(R.id.bInsertPrivate)).perform(click())
        onView(isRoot()).perform(waitForView(R.id.tvMail, 5000))
        onView(withId(R.id.tvMail)).check(matches(isDisplayed()))
        navigateDownloadListTest()
    }

    fun navigateDownloadListTest() {
        onView(isRoot()).perform(waitForView(R.id.tvMail, 5000))
        onView(withId(R.id.navigation_lists)).perform(click())
        onView(withId(R.id.tabs)).check(matches(isDisplayed()))
    }
}
