package ru.innopolis.semantic.type;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RecordType extends Type {
    private final String name;
    private final Map<String, Type> fields = new LinkedHashMap<>();

    public RecordType(String name) {
        this.name = name;
    }

    void addField(String fieldName, Type type) {
        fields.put(fieldName, type);
    }

    public Type getField(String fieldName) {
        return fields.get(fieldName);
    }

    public Map<String, Type> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    @Override
    public String getName() {
        return name;
    }
}
