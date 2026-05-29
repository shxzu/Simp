package cc.simp.utils.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BuildType {

    RELEASE("Release"),
    ALPHA("Alpha"),
    BETA("Beta"),
    DEV("Development");

    private final String name;

}
