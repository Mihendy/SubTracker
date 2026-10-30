package ru.mihendy.subtracker.data

import java.util.UUID

/**
 * Идентификатор подписки.
 *
 * Обёртка над строкой, чтобы компилятор не дал перепутать id с любой другой строкой
 * (названием, кодом валюты). `value class` в скомпилированном коде превращается
 * обратно в обычную String, так что лишних объектов не создаётся.
 *
 * Пустой id создать нельзя: конструктор бросит [IllegalArgumentException].
 * Поэтому код, который читает id извне (JSON), должен проверить строку сам.
 */
@JvmInline
value class SubscriptionId(val value: String) {
    init {
        require(value.isNotBlank()) { "id подписки не может быть пустым" }
    }

    /** В логах и сообщениях — просто сама строка, без `SubscriptionId(value=...)`. */
    override fun toString(): String = value

    companion object {
        /** Новый случайный id (UUID) — для только что созданной подписки. */
        fun random(): SubscriptionId = SubscriptionId(UUID.randomUUID().toString())
    }
}
