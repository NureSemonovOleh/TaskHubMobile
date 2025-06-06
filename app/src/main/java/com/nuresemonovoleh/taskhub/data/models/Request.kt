package com.nuresemonovoleh.taskhub.data.models

data class Request(
    val id: Int,
    val userId: Int,
    val attachmentId: Int,
    val type: RequestType,
    val message: String,
    val isApplied: Boolean,
    val isRejected: Boolean
)
