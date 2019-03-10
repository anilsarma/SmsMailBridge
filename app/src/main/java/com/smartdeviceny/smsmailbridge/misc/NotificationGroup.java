package com.smartdeviceny.smsmailbridge.misc;

public enum NotificationGroup {
    BRIDGE {
        public NotificationChannels getChannel() { return NotificationChannels.SMSMAIL;}
        public int  getID() {
            return 1000;
        }
        public String getGroupID() {
            return "Bridge";
        }
        public String getName() {
            return "Bridge";
        }
        public String getDescription() {
            return "Bridge";
        }

    };
    abstract public NotificationChannels getChannel();
    abstract public int  getID();
    public String getUniqueID() { return getChannel().getUniqueID() + "." + getGroupID() + "." + getID();}
    abstract public String getGroupID();
    abstract public String getName();
    abstract public String getDescription();

}
