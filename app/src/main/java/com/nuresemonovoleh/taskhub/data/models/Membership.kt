package com.nuresemonovoleh.taskhub.data.models

data class Membership(
    val userId: Int,
    val taskGroupId: Int,
    val membershipLevel: MembershipLevel
)
