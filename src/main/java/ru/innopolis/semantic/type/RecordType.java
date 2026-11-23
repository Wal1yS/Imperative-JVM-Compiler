package ru.innopolis.semantic.type;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RecordType extends Type {
    private final String name;
    private final String declarationName;
    private final Map<String, Type> fields = new LinkedHashMap<>();

    public RecordType(String name) {
        this(name, name);
    }

    RecordType(String name, String declarationName) {
        this.name = name;
        this.declarationName = declarationName;
    }

    public String getDeclarationName() {
        return declarationName;
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
