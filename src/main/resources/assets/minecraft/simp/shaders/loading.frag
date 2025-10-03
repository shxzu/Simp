#version 120

uniform vec2 iResolution;
uniform float iTime;

vec3 colorGradient(float t) {
    vec3 a = vec3(0.5, 0.8, 1.0);  // Light blue
    vec3 b = vec3(0.9, 0.3, 0.8);  // Pink
    return mix(a, b, t);
}

float pulsingRing(vec2 p) {
    // Main ring animation
    float mainPulse = sin(iTime * 2.0) * 0.1;
    float secondaryPulse = sin(iTime * 4.0 + 1.0) * 0.05;
    float radius = 0.4 + mainPulse + secondaryPulse;

    float innerRadius = radius - 0.05;
    float outerRadius = radius + 0.05;

    float ring = length(p);
    float mainRing = smoothstep(0.06, 0.0, abs(ring - radius));

    float echoRing = smoothstep(0.03, 0.0, abs(ring - (radius + 0.15))) * 0.5;
    echoRing += smoothstep(0.03, 0.0, abs(ring - (radius - 0.15))) * 0.5;

    float angle = atan(p.y, p.x);
    float rotationEffect = sin(angle * 6.0 + iTime * 3.0) * 0.1;

    float finalRing = mainRing + echoRing * 0.3;
    finalRing *= 1.0 + rotationEffect;

    return finalRing;
}

void main() {
    vec2 fragCoord = gl_TexCoord[0].st * iResolution;
    vec2 uv = (fragCoord - iResolution.xy/2.0) / iResolution.y * 2.0;

    float ring = pulsingRing(uv);

    float colorShift = sin(iTime) * 0.5 + 0.5;
    vec3 ringColor = colorGradient(colorShift);

    float glow = ring * 1.2;
    glow += smoothstep(1.0, 0.0, length(uv)) * 0.1;

    vec3 finalColor = ringColor * glow;
    finalColor += vec3(ring) * 0.8;  // Add white highlight

    gl_FragColor = vec4(finalColor, 1.0);
}