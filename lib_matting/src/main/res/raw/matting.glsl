precision highp float;
uniform float keyColorR;
uniform float keyColorG;
uniform float keyColorB;

uniform sampler2D inputImageTexture;
uniform sampler2D inputImageTexture2;
varying highp vec2 textureCoordinate;
varying highp vec2 textureCoordinate2;



// 色度的相似度阈值
uniform float similarity;
// 透明度的平滑度计算
float smoothness = 0.01;
// 降低绿幕饱和度，提高抠图准确度
float spill = 0.1;

vec2 RGBtoUV(vec3 rgb) {
    return vec2(
    rgb.r * -0.169 + rgb.g * -0.331 + rgb.b *  0.5    + 0.5,
    rgb.r *  0.5   + rgb.g * -0.419 + rgb.b * -0.081  + 0.5
    );
}



void main(){
    vec4 rgba = texture2D(inputImageTexture, textureCoordinate);
    vec4 bg = texture2D(inputImageTexture2, vec2(textureCoordinate2.x,1.0-textureCoordinate2.y));
    // 计算当前像素与绿幕像素的色度差值
    vec2 chromaVec = RGBtoUV(rgba.rgb) - RGBtoUV(vec3(keyColorR, keyColorG, keyColorB));
    // 计算当前像素与绿幕像素的色度距离（向量长度）, 越相似则色度距离越小
    float chromaDist = sqrt(dot(chromaVec, chromaVec));
    // 设置了一个相似度阈值，baseMask < 0，则像素是绿幕，> 0 则像素点可能属于前景（比如人物）
    float baseMask = chromaDist - similarity;
    // 与平滑度参数计算，将 baseMask 转换成 alpha 通道值，越大越不透明
    float fullMask = pow(clamp(baseMask / smoothness, 0., 1.), 1.5);
    rgba.a = fullMask;
    // 如果 baseMask < 0，spillVal 等于 0；baseMask 越小，像素点饱和度越低
    if (baseMask < 0.0) {
        float spillVal = pow(clamp(baseMask / spill, 0., 1.), 1.5);
        // 计算当前像素的灰度值
        float desat = clamp(rgba.r * 0.2126 + rgba.g * 0.7152 + rgba.b * 0.0722, 0., 1.);
        rgba.rgb = mix(vec3(desat, desat, desat), rgba.rgb, spillVal);
    }


    gl_FragColor = mix(bg, rgba, fullMask);
}