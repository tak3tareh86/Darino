package com.example.data.model

enum class ReminderType {
    GENERAL,
    INSTALLMENT,
    VEHICLE,
    INSURANCE,
    MAINTENANCE,
    FUEL,
    CUSTOM
}

enum class DeliveryStatus {
    PENDING,
    SCHEDULED,
    SENT,
    DELIVERED,
    FAILED,
    CANCELLED
}

enum class SmsStatus {
    QUEUED,
    SENT,
    DELIVERED,
    FAILED
}

enum class ReminderPriority {
    LOW,
    NORMAL,
    HIGH
}
