#version 150

// Invasion portal: the End portal's layered parallax starfield, recoloured green.
uniform sampler2D Sampler0; // sky noise
uniform sampler2D Sampler1; // speck layer
uniform float GameTime;

in vec4 screenPos;

out vec4 fragColor;

mat2 rotation(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, -s, s, c);
}

void main() {
    vec2 uv = screenPos.xy / screenPos.w;
    float t = GameTime * 1.5;

    vec3 color = texture(Sampler0, uv).rgb * vec3(0.012, 0.055, 0.02);
    for (int i = 0; i < 15; i++) {
        float f = float(i + 1);
        float scale = 5.5 - f * 0.18;
        float ang = radians((f * f * 4.0 + f * 9.0) * 2.0);
        vec2 p = rotation(ang) * ((uv - 0.5) * scale);
        p += vec2(17.0 / f, (2.0 + f / 1.5) * t);
        vec3 speck = texture(Sampler1, p).rgb;
        float fade = (1.0 - f / 18.0) * 2.6;
        vec3 tint = vec3(0.03 + 0.07 * fract(f * 0.37),
                         0.16 + 0.30 * fract(f * 0.618 + 0.2),
                         0.04 + 0.10 * fract(f * 0.83)) * fade;
        color += speck * tint;
    }
    fragColor = vec4(color, 1.0);
}
