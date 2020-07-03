package com.github.livingwithhippos.unchained.user

import androidx.lifecycle.ViewModel

class UserProfileViewModel : ViewModel() {

    val id : Int = TODO()
    val username : String = TODO()
    val email : String = TODO()
    val points : Int = TODO()
    val locale : String = TODO()
    val avatar : String = TODO()
    val type : String = TODO()
    val premium : Int = TODO()
    val expiration : String = TODO()

    /**
     * JSON API response to /user
    {
        "id": int,
        "username": "string",
        "email": "string",
        "points": int, // Fidelity points
        "locale": "string", // User language
        "avatar": "string", // URL
        "type": "string", // "premium" or "free"
        "premium": int, // seconds left as a Premium user
        "expiration": "string" // jsonDate
    }
    */
}