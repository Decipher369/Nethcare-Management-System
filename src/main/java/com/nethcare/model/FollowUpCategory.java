package com.nethcare.model;

public enum FollowUpCategory {
    ROUTINE_REVIEW("Routine review"),
    REASSESSMENT("Reassessment"),
    POST_OPERATIVE("Post-operative surveillance"),
    CORNEAL_ULCER("Corneal ulcer review"),
    PEDIATRIC_AMBLYOPIA("Pediatric amblyopia");

    private final String label;

    FollowUpCategory(String label) { this.label = label; }
    public String label() { return label; }
}
