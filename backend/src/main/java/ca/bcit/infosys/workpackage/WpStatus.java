package ca.bcit.infosys.workpackage;

public enum WpStatus {
    OPEN_FOR_CHARGES("Open for Charges"),
    CLOSED_FOR_CHARGES("Closed for Charges"),
    COMPLETE("Complete");

    private final String displayName;

    WpStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
