package com.smartdeviceny.smsmailbridge.misc;

import android.app.NotificationManager;

public enum NotificationChannels {
    SMSMAIL {
        public String getUniqueID() {
            return "SMS Mail Bridge";
        }

        public String getName() {
            return getUniqueID();
        }

        public String getDescription() {
            return "Notification channel for the SMS Mail Bridge application.";
        }

        public int getImportance() {
            return NotificationManager.IMPORTANCE_LOW;
        }
    },
    DEBUG {
        public String getUniqueID() {
            return "DEBUG";
        }

        public String getName() {
            return "SMS Mail Bridge Debug Channel";
        }

        public String getDescription() {
            return "Debug Notification channel for the SMS Mail Bridge application.";
        }

        public int getImportance() {
            return NotificationManager.IMPORTANCE_LOW;
        }
    };
    abstract public String getUniqueID();

    abstract public String getName();

    abstract public String getDescription();

    abstract public int getImportance();
}
