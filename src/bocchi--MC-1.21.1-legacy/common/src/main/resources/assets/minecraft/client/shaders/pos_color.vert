#version 330

layout(location = 0) in vec3 pos;
layout(location = 1) in vec4 color;

//layout (std140) uniform MatrixBlock {
//    mat4 ModelMat;
//    mat4 ProjMat;
//    vec2 ScreenSize;
//};

uniform mat4 ModelMat;
uniform mat4 ProjMat;
uniform vec2 ScreenSize;

out vec4 v_Color;
out vec2 v_ScreenSize;

void main() {
    gl_Position = ProjMat * ModelMat * vec4(pos, 1.0);

    v_ScreenSize = ScreenSize;
    v_Color = color;
}
