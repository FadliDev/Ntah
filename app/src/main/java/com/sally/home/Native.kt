package com.sally.home

object Native {
    init { System.loadLibrary("sallyhome") }
    external fun score(query: String, label: String): Int
}
