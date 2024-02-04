package model

enum class CommitMode(
    val value: String,
) {
    ASYNC("async"),
    SYNC("sync"),
}