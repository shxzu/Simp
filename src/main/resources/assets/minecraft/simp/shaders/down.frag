#version 120

uniform sampler2D inTexture;
uniform vec2 offset, halfpixel, iResolution;

const vec4 weights = vec4(0.4, 0.15, 0.15, 0.15);

void main() {
    vec2 uv = gl_FragCoord.xy / iResolution;

    vec4 color = texture2D(inTexture, gl_TexCoord[0].st) * weights.x;
    color.rgb *= color.a;

    vec4 offsetX = vec4(-1.0, 1.0, 1.0, -1.0) * halfpixel.x * offset.x * 0.8;
    vec4 offsetY = vec4(-1.0, 1.0, -1.0, 1.0) * halfpixel.y * offset.y * 0.8;

    for (int i = 0; i < 4; i++) {
        vec2 sampleOffset = vec2(offsetX[i], offsetY[i]);
        vec4 sample = texture2D(inTexture, uv + sampleOffset);
        sample.rgb *= sample.a;
        color += sample * weights.y;
    }

    gl_FragColor = vec4(color.rgb / max(color.a, 0.001), color.a);
}
