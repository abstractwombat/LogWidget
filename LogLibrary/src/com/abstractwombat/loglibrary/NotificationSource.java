package com.abstractwombat.loglibrary;

import android.service.notification.StatusBarNotification;

/**
 * Created by Mike on 5/11/2015.
 */
public interface NotificationSource {
    public boolean addNotification(StatusBarNotification notification);
    public boolean enabled();
    public String getPackage();
}
