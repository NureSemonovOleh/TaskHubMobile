package com.nuresemonovoleh.taskhub.data.models

data class Access(
    val userId: Int,
    val taskId: Int,
    val accessLevel: AccessLevel
)
