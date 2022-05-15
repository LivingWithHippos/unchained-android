import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.MainActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var rule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun mainActivityTest() {
        val checkBox = onView(
            allOf(
                withId(R.id.cbLink),
                isDisplayed()
            )
        )
        checkBox.check(matches(isDisplayed()))
    }
}
