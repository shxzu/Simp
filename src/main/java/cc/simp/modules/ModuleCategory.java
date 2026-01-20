package cc.simp.modules;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModuleCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    CLIENT("Client"),
    VISUALS("Visuals"),
    CONFIGS("Configs");
    private final String name;
}
