package user11681.plugin.processing;

@SuppressWarnings("unchecked")
public class Container<T> {
    protected T value;

    public Container(T value) {
        this.value = value;
    }

    public T get() {
        return this.value;
    }

    public <R> R cast() {
        return (R) this.value;
    }

    public void set(Object value) {
        this.value = (T) value;
    }
}
