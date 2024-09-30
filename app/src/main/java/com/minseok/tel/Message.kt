package com.minseok.tel

data class Message(
    var id: String? = null,
    var sender: String? = null,
    var phone: String? = null,
    var message: String = "",
    var timestamp: Long = 0
)
