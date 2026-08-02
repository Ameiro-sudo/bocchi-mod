#version 330 core

layout(location = 0) in vec3 pos;

uniform mat4 ProjMat;
uniform vec2 Size;

out vec2 v_TexCoord;
out vec2 v_OneTexel;

void main() {
    vec4 outPos = ProjMat * vec4(pos.xy * Size, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);

    v_TexCoord = pos.xy;
    v_OneTexel = 1.0 / Size;
}
