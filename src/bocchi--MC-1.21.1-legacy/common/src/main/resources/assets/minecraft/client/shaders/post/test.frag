#version 330 core

in vec2 v_TexCoord;
in vec2 v_OneTexel;

uniform sampler2D InSampler;
uniform sampler2D HistorySampler;
uniform float blurriness;
uniform bool renderRGB;

out vec4 fragColor;

void main() {
    vec4 current = texture(InSampler, v_TexCoord);
    vec4 history = texture(HistorySampler, v_TexCoord);

    vec2 interpolatedTexCoord;
    interpolatedTexCoord.x = mix(v_TexCoord.x, v_TexCoord.x + 0.01, 0.5);
    interpolatedTexCoord.y = mix(v_TexCoord.y, v_TexCoord.y + 0.01, 0.5);

    vec4 blurredColor = mix(history, texture(InSampler, interpolatedTexCoord), blurriness);

    if (renderRGB)
    {
        fragColor = blurredColor;
    }
    else
    {
        float value1 = texture(InSampler, interpolatedTexCoord).r;
        fragColor = mix(history, vec4(value1), blurriness);
    }
}