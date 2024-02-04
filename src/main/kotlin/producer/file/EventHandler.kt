package com.practice.kafka.event

import producer.file.MessageEvent

interface EventHandler<K, V> {
    fun onMessage(messageEvent: MessageEvent<K, V>)
}