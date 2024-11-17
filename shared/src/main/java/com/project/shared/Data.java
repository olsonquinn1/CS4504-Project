package com.project.shared;

import java.io.Serializable;

/**
 * Represents a data object that can be serialized and sent between different components of the application.
 */
public class Data implements Serializable {
    
    /**
     * Enum representing the type of data.
     */
    public enum Type {
        REQUEST,
        RESPONSE,
        CLOSE,
        TASK_DATA,
        SUBTASK_DATA,
        PROFILING_DATA,
        RESULT_DATA
    }

    private final Type type;

    private final Serializable payload;

    /**
     * Constructs a new Data object with the specified type and payload.
     * 
     * @param type    the type of the data
     * @param payload the payload of the data
     */
    public Data(Type type, Serializable payload) {
        this.type = type;
        this.payload = payload;
    }

    /**
     * Returns the type of the data.
     * 
     * @return the type of the data
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the payload of the data.
     * 
     * @return the payload of the data
     */
    public Serializable getData() {
        return payload;
    }
}
