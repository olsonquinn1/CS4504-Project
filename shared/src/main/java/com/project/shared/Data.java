package com.project.shared;

import java.io.Serializable;

public class Data implements Serializable {
    
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

    public Data(Type type, Serializable payload) {
        this.type = type;
        this.payload = payload;
    }

    public Type getType() {
        return type;
    }

    public Serializable getData() {
        return payload;
    }
}
