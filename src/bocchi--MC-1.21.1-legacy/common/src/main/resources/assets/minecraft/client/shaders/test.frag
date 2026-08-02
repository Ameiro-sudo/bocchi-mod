#version 330

in vec4 v_Color;

uniform vec2 test;

out vec4 fragColor;

void main() {
    fragColor = v_Color + test.x + test.y;
}