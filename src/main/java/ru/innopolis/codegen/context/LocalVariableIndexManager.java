package ru.innopolis.codegen.context;

import ru.innopolis.semantic.symbol.Symbol;
import ru.innopolis.semantic.type.PrimitiveType;
import ru.innopolis.semantic.type.Type;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages allocation and resolution of JVM local variable slots for a routine.
 * Handles parameters, local variables, and 64-bit slot sizing (real/double takes 2 slots).
 * Part of Person 1 (Valerii Tiniakov) foundational infrastructure.
 */
public class LocalVariableIndexManager {
    private int nextSlotIndex = 0;
    private final Map<Symbol, Integer> symbolSlots = new HashMap<>();
    private final Deque<Integer> scopeStarts = new ArrayDeque<>();

    public LocalVariableIndexManager() {
        this(0); // static methods have no 'this' pointer, slot 0 is the first parameter
    }

    public LocalVariableIndexManager(int initialSlot) {
        this.nextSlotIndex = initialSlot;
    }

    /**
     * Allocates a local variable slot for the given symbol and type.
     * If type is PrimitiveType.REAL (double), increments next slot by 2.
     * Otherwise increments next slot by 1.
     */
    public int allocateSlot(Symbol symbol, Type type) {
        if (symbolSlots.containsKey(symbol)) {
            return symbolSlots.get(symbol);
        }

        int slot = allocateTemporarySlot(type);
        symbolSlots.put(symbol, slot);
        return slot;
    }

    public int allocateTemporarySlot(Type type) {
        int slot = nextSlotIndex;
        if (type == PrimitiveType.REAL) {
            nextSlotIndex += 2; // double occupies 2 32-bit JVM local slots
        } else {
            nextSlotIndex += 1;
        }

        return slot;
    }

    /**
     * Gets the allocated slot for the given symbol.
     */
    public int getSlot(Symbol symbol) {
        Integer slot = symbolSlots.get(symbol);
        if (slot == null) {
            throw new IllegalStateException("Local slot not allocated for symbol: " + symbol.getName());
        }
        return slot;
    }

    public void enterScope() {
        scopeStarts.push(nextSlotIndex);
    }

    public void exitScope() {
        if (scopeStarts.isEmpty()) {
            throw new IllegalStateException("exitScope() called without a matching enterScope()");
        }
        int scopeStart = scopeStarts.pop();
        symbolSlots.values().removeIf(slot -> slot >= scopeStart);
        nextSlotIndex = scopeStart;
    }

    public boolean hasSlot(Symbol symbol) {
        return symbolSlots.containsKey(symbol);
    }

    public int getNextSlotIndex() {
        return nextSlotIndex;
    }
}
