package com.sally.keyboard

object Native {
    init { System.loadLibrary("sallykeyboard") }
    external fun suggest(prefix: String): Array<String>
}
