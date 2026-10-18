package com.bookmyshow.enums;

/**
 * Delivery channels for notifications.
 * Used by the Observer pattern implementations to route messages.
 *
 * DESIGN PATTERN: Observer – each NotificationChannel corresponds to
 * a concrete observer (EmailNotificationObserver, SMSNotificationObserver, etc.)
 */
public enum NotificationChannel {
    EMAIL,   // Transactional email (confirmation, cancellation, refund)
    SMS,     // SMS via Twilio / AWS SNS
    PUSH     // Mobile push notification via FCM / APNs
}

