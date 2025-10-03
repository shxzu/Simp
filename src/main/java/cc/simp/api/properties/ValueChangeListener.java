package cc.simp.api.properties;

@FunctionalInterface
public interface ValueChangeListener<T> {

    void onValueChange(T oldValue, T value);

}
