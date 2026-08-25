package com.memeboo2.haemi.elder.training.domain;

public enum DifficultyLevel {
    LEVEL_1(3, 10, true),
    LEVEL_2(4, 5, false),
    LEVEL_3(4, 3, false);

    private final int choiceCount;
    private final int yearTolerance;
    private final boolean hintProvided;

    DifficultyLevel(int choiceCount, int yearTolerance, boolean hintProvided) {
        this.choiceCount = choiceCount;
        this.yearTolerance = yearTolerance;
        this.hintProvided = hintProvided;
    }

    public int choiceCount() {
        return choiceCount;
    }

    public int yearTolerance() {
        return yearTolerance;
    }

    public boolean hintProvided() {
        return hintProvided;
    }

    public DifficultyLevel raise() {
        return this == LEVEL_3 ? this : values()[ordinal() + 1];
    }

    public DifficultyLevel lower() {
        return this == LEVEL_1 ? this : values()[ordinal() - 1];
    }
}
