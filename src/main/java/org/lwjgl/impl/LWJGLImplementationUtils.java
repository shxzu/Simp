package org.lwjgl.impl;

import org.lwjgl.impl.glfw.GLFWKeyboardImplementation;
import org.lwjgl.impl.glfw.GLFWMouseImplementation;
import org.lwjgl.impl.input.CombinedInputImplementation;
import org.lwjgl.impl.input.InputImplementation;

/**
 * @author Zarzelcow
 * @created 28/09/2022 - 3:12 PM
 */
public class LWJGLImplementationUtils {
    private static InputImplementation _inputImplementation;

    public static InputImplementation getOrCreateInputImplementation() {
        if (_inputImplementation == null) {
            _inputImplementation = createImplementation();
        }
        return _inputImplementation;
    }

    private static InputImplementation createImplementation() {
        return new CombinedInputImplementation(new GLFWKeyboardImplementation(), new GLFWMouseImplementation());
    }

}
