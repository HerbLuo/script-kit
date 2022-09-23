package cn.cloudself.script.util;

public class Ref<T> {
    private final T value;
    public Ref(T value) {
        this.value = value;
    }

    public static <T> Ref<T> of(T value) {
        return new Ref<>(value);
    }

    public T getValue() {
        return value;
    }
}
