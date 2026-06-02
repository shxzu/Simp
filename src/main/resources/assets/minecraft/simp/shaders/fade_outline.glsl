#version 120

uniform sampler2D u_diffuse_sampler;
uniform vec2 u_texel_size;
uniform int u_fading;
uniform int u_radius;
uniform vec4 u_color; // <-- Add this new uniform

void main() {
    vec2 uv = gl_TexCoord[0].st;
    vec4 base_color = texture2D(u_diffuse_sampler, uv);

    if (base_color.a != 0.0) {
        gl_FragColor = base_color;
        return;
    }

    float alpha = 0.0;
    bool has_neighbor = false;

    for (int x = -u_radius; x <= u_radius; ++x) {
        for (int y = -u_radius; y <= u_radius; ++y) {
            vec4 next_color = texture2D(u_diffuse_sampler, uv + vec2(x, y) * u_texel_size);

            if (next_color.a == 0.0) continue;

            alpha += max(0.0, float(u_radius) - sqrt(float(x * x + y * y)));
            has_neighbor = true;
        }
    }

    if (has_neighbor) {
        // Use the passed color's RGB, and multiply its alpha by the calculated dropoff
        gl_FragColor = vec4(u_color.rgb, (alpha * u_color.a) / float(u_fading));
    } else {
        gl_FragColor = vec4(0.0);
    }
}