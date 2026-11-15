#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 screenPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // Screen-space coordinates so the starfield stays fixed to the view and shifts as you move.
    screenPos = vec4((gl_Position.xy + gl_Position.w) * 0.5, gl_Position.zw);
}
