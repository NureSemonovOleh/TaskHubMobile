package com.nuresemonovoleh.taskhub.data.models

data class Attachment(
    val type: AttachmentType,
    val data: ByteArray,
    val isPrivate: Boolean,
    val description: String
)
