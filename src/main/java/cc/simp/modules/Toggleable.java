package cc.simp.modules;

public interface Toggleable {

    void toggle();

    void setEnabled(boolean enabled);

    boolean isEnabled();

    void onEnable();

    void onDisable();
}
