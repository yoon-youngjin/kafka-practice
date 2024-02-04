package consumer

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Timestamp
import java.time.LocalDateTime
import model.OrderModel
import org.slf4j.LoggerFactory


private val logger = LoggerFactory.getLogger(OrderDBHandler::class.java)

class OrderDBHandler(
    url: String,
    user: String,
    password: String,
) {
    private var connection: Connection
    private var insertPrepared: PreparedStatement

    private val INSERT_ORDER_SQL = "INSERT INTO orders " +
        "(orderId, shopId, menuName, userName, phoneNumber, address, orderTime, createdAt) " +
        "values (?, ?, ?, ?, ?, ?, ?, ?)"

    init {
        connection = DriverManager.getConnection(url, user, password)
        insertPrepared = connection.prepareStatement(INSERT_ORDER_SQL)
    }

    fun insertOrder(orderModel: OrderModel, now: LocalDateTime) {
        try {
            val pstmt = connection.prepareStatement(INSERT_ORDER_SQL)
            pstmt.setString(1, orderModel.orderId)
            pstmt.setString(2, orderModel.shopId)
            pstmt.setString(3, orderModel.menuName)
            pstmt.setString(4, orderModel.userName)
            pstmt.setString(5, orderModel.phoneNumber)
            pstmt.setString(6, orderModel.address)
            pstmt.setTimestamp(7, Timestamp.valueOf(orderModel.orderTime))
            pstmt.setTimestamp(8, Timestamp.valueOf(now))
            pstmt.executeUpdate()
        } catch (e: SQLException) {
            logger.error(e.message)
        }
    }

    fun insertOrders(orders: List<OrderModel>) {
        try {
            val pstmt = connection.prepareStatement(INSERT_ORDER_SQL)
            for (orderDTO in orders) {
                pstmt.setString(1, orderDTO.orderId)
                pstmt.setString(2, orderDTO.shopId)
                pstmt.setString(3, orderDTO.menuName)
                pstmt.setString(4, orderDTO.userName)
                pstmt.setString(5, orderDTO.phoneNumber)
                pstmt.setString(6, orderDTO.address)
                pstmt.setTimestamp(7, Timestamp.valueOf(orderDTO.orderTime))
                pstmt.addBatch()
            }
            pstmt.executeUpdate()
        } catch (e: SQLException) {
            logger.info(e.message)
        }
    }

    fun close() {
        try {
            logger.info("###### OrderDBHandler is closing")
            insertPrepared.close()
            connection.close()
        } catch (e: SQLException) {
            logger.error(e.message)
        }
    }
}
