package model

import java.time.LocalDateTime

data class OrderModel(
    val id: Long? = 0L,
    val orderId: String,
    val shopId: String,
    val menuName: String,
    val userName: String,
    val phoneNumber: String,
    val address: String,
    val orderTime: LocalDateTime,
)