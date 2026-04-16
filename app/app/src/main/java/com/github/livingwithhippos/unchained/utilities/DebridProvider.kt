package com.github.livingwithhippos.unchained.utilities

import android.content.SharedPreferences

enum class DebridProvider(val value: String) {
    RealDebrid("realdebrid"),
    AllDebrid("alldebrid"),
    ;

    companion object {
        fun fromValue(value: String?): DebridProvider =
            entries.firstOrNull { it.value == value } ?: RealDebrid
    }
}

fun SharedPreferences.getDebridProvider(): DebridProvider =
    DebridProvider.fromValue(
        getString(PreferenceKeys.Services.DEBRID_PROVIDER, DebridProvider.RealDebrid.value)
    )

fun SharedPreferences.setDebridProvider(provider: DebridProvider) {
    edit().putString(PreferenceKeys.Services.DEBRID_PROVIDER, provider.value).apply()
}
