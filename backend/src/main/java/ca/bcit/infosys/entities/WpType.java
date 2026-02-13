package ca.bcit.infosys.entities;

public enum WpType {
    SUMMARY("Summary"),
    LOWEST_LEVEL("Lowest-Level");

    private final String displayName;

    WpType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
