precision highp float;
varying vec2 textureCoordinate;
uniform sampler2D u_bg;
uniform vec2 u_factor;
uniform vec2 u_resolution;
//uniform float u_time;


mat3 inverse_mat3(mat3 m)
{
    float Determinant =
    m[0][0] * (m[1][1] * m[2][2] - m[2][1] * m[1][2])
    - m[1][0] * (m[0][1] * m[2][2] - m[2][1] * m[0][2])
    + m[2][0] * (m[0][1] * m[1][2] - m[1][1] * m[0][2]);

    mat3 Inverse;
    Inverse[0][0] = + (m[1][1] * m[2][2] - m[2][1] * m[1][2]);
    Inverse[1][0] = -(m[1][0] * m[2][2] - m[2][0] * m[1][2]);
    Inverse[2][0] = + (m[1][0] * m[2][1] - m[2][0] * m[1][1]);
    Inverse[0][1] = -(m[0][1] * m[2][2] - m[2][1] * m[0][2]);
    Inverse[1][1] = + (m[0][0] * m[2][2] - m[2][0] * m[0][2]);
    Inverse[2][1] = -(m[0][0] * m[2][1] - m[2][0] * m[0][1]);
    Inverse[0][2] = + (m[0][1] * m[1][2] - m[1][1] * m[0][2]);
    Inverse[1][2] = -(m[0][0] * m[1][2] - m[1][0] * m[0][2]);
    Inverse[2][2] = + (m[0][0] * m[1][1] - m[1][0] * m[0][1]);
    Inverse /= Determinant;

    return Inverse;
}

vec2 rotateAxisX(vec2 uv, float angle, float offset) {
    uv -= offset;
    float r = radians(angle);
    vec3 ret = inverse_mat3(mat3(
                            1.0, 0.0, 0.0,
                            0.0, cos(r), -sin(r),
                            0.0, 0.0, 2.0
                            )) * vec3(uv, 2.0);
    return ret.xy / ret.z + offset;
}



vec2 rotateAxisY(vec2 uv, float angle, float offset) {
    uv -= offset;
    float r = radians(angle);
    vec3 ret = inverse_mat3(mat3(cos(r), 0, -sin(r), 0, 1, 0, 0, 0, 2)) * vec3(uv, 2);
    return ret.xy / ret.z + offset;
}



void main() {
    float ratio = (u_resolution.x / u_resolution.y);
    vec2 centeredUV = textureCoordinate - 0.5;  // 以中心为基准
    float scale = 0.95;  // 放大系数
    centeredUV *= scale;
    vec2 scaledUV = centeredUV + 0.5;

    float depthFactor = 0.5;

    vec2 uv1 = rotateAxisX(scaledUV, u_factor.y * 5.0 * (1.0+depthFactor*0.2), 0.5);
    vec2 uv2 = rotateAxisY(uv1, u_factor.x * 5.0 * (1.0+depthFactor*0.2), 0.5);

//    vec2 autoMove = vec2(sin(u_time), cos(u_time * u_resolution.x / u_resolution.y)) * 0.05;

    vec2 offset = vec2(u_factor.x, -u_factor.y) * 0.02 * (1.0+depthFactor*0.015);
    offset.x = offset.x/ratio ;

    vec2 parallaxUV = uv2 + offset;
    parallaxUV = clamp(parallaxUV, 0.0, 1.0);
    vec4 bgColor = texture2D(u_bg, parallaxUV);

    gl_FragColor = bgColor;
}