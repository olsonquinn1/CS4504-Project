package com.project.client;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TableRowData {
    private final SimpleStringProperty size;
    private final SimpleStringProperty thread1;
    private final SimpleStringProperty thread3;
    private final SimpleStringProperty thread7;
    private final SimpleStringProperty thread15;
    private final SimpleStringProperty thread31;

    public TableRowData(String size, String thread1, String thread3, String thread7, String thread15, String thread31) {
        this.size = new SimpleStringProperty(size);
        this.thread1 = new SimpleStringProperty(thread1);
        this.thread3 = new SimpleStringProperty(thread3);
        this.thread7 = new SimpleStringProperty(thread7);
        this.thread15 = new SimpleStringProperty(thread15);
        this.thread31 = new SimpleStringProperty(thread31);
    }

    // Getters for each property, e.g., getSizeProperty() for binding
    public StringProperty sizeProperty() { return size; }
    public StringProperty thread1Property() { return thread1; }
    public StringProperty thread3Property() { return thread3; }
    public StringProperty thread7Property() { return thread7; }
    public StringProperty thread15Property() { return thread15; }
    public StringProperty thread31Property() { return thread31; }
}

