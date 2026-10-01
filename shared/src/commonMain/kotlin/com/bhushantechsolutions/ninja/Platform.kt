package com.bhushantechsolutions.ninja

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform