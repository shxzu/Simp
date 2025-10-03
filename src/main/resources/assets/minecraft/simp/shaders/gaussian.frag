#version 120

uniform sampler2D textureIn;
uniform vec2 texelSize, direction;
uniform float radius;

const float weights[5] = float[5](0.2270270, 0.1945945, 0.1216216, 0.0540540, 0.0162162);

void main() {
    vec3 blr = texture2D(textureIn, gl_TexCoord[0].st).rgb * weights[0];

    for (int f = 1; f < 5; f++) {
        float offsetScale = float(f);
        vec2 currentOffset = offsetScale * texelSize * direction;

        blr += texture2D(textureIn, gl_TexCoord[0].st + currentOffset).rgb * weights[f];
        blr += texture2D(textureIn, gl_TexCoord[0].st - currentOffset).rgb * weights[f];
    }

    gl_FragColor = vec4(blr, 1.0);
}