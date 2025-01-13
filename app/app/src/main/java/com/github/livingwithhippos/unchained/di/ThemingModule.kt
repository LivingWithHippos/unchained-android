import android.content.SharedPreferences
import com.github.livingwithhippos.unchained.base.ThemingCallback
import org.koin.dsl.module

val themingModule = module {
    single { provideThemingCallback(get()) }
}

fun provideThemingCallback(preferences: SharedPreferences): ThemingCallback {
    return ThemingCallback(preferences)
}