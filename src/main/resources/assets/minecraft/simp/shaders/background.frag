#ifdef GL_ES
precision mediump float;
#endif

#extension GL_OES_standard_derivatives : enable

uniform vec2 resolution;
uniform float time;

// Customizable colors
vec3 color1 = vec3(0.27, 0.15, 0.47);  // Deep space purple
vec3 color2 = vec3(0.58, 0.35, 0.95);  // Bright cosmic violet

// Random noise generator
float noise(vec2 pos) {
    return fract(sin(dot(pos, vec2(12.9898, 78.233))) * 43758.5453123);
}

// Smooth noise interpolation
float smoothNoise(vec2 pos) {
    vec2 i = floor(pos);
    vec2 f = fract(pos);

    float a = noise(i);
    float b = noise(i + vec2(1.0, 0.0));
    float c = noise(i + vec2(0.0, 1.0));
    float d = noise(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

// Chaotic distortion for UV coordinates
vec2 chaoticDistortion(vec2 uv, float t) {
    vec2 offset = vec2(sin(t * 0.5), cos(t * 0.7)) * 0.2;
    uv += smoothNoise(uv * 3.0 + offset + t * 0.1) * 0.2;
    uv += smoothNoise(uv * 5.0 - offset - t * 0.15) * 0.1;
    return uv;
}

// Create morphing organic patterns
float morphPatterns(vec2 uv, float t) {
    uv = chaoticDistortion(uv, t);
    float pattern = sin(uv.x * 6.0 + t) * cos(uv.y * 6.0 - t);
    pattern += smoothNoise(uv * 8.0 + t * 0.5) * 0.5;
    return pattern;
}

void main() {
    // Normalized UV coordinates with aspect ratio adjustment
    vec2 uv = (gl_FragCoord.xy - resolution.xy * 0.5) / resolution.y;

    // Time-based movement for noise fields
    float t = time * 0.5;

    // Morphing patterns
    float pattern = morphPatterns(uv, t);

    // Add layered distortions for depth
    float layer1 = morphPatterns(uv * 1.2, t * 1.1);
    float layer2 = morphPatterns(uv * 0.8, -t * 0.8);

    // Combine layers with randomization
    float combined = pattern * 0.6 + layer1 * 0.3 + layer2 * 0.1;

    // Make dark areas smoothly transition into purple
    float darkFactor = smoothNoise(uv * 3.0 + t) * 0.5; // Dynamic dark zone generation
    darkFactor = smoothstep(0.0, 0.5, darkFactor);      // Smooth out the transition

    // Blend dark areas with the main colors
    vec3 darkColor = mix(color1 * 0.3, color1, darkFactor); // Subdued version of color1
    vec3 vibrantColor = mix(color1, color2, combined * 0.5 + 0.5);

    // Smooth blending between dark areas and vibrant areas
    vec3 finalColor = mix(darkColor, vibrantColor, darkFactor);

    // Add subtle flickers using random noise
    float flicker = smoothNoise(uv * 10.0 + t) * 0.05;
    finalColor += flicker;

    // No vignette, so colors remain uniform across the screen
    gl_FragColor = vec4(finalColor, 1.0);
}
