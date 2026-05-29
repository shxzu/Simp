package cc.simp.utils.render.shaders;

import cc.simp.utils.Util;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

import java.io.*;

import static org.lwjgl.opengl.GL20.*;

public class ShaderUtils extends Util {
    private final int programID;

    public ShaderUtils(String fragmentShaderLoc, String vertexShaderLoc) {
        int program = glCreateProgram();
        try {
            int fragmentShaderID = switch (fragmentShaderLoc) {
                case "shadow" -> createShader(new ByteArrayInputStream(shadow.getBytes()), GL_FRAGMENT_SHADER);
                case "roundRectTexture" ->
                        createShader(new ByteArrayInputStream(roundRectTexture.getBytes()), GL_FRAGMENT_SHADER);
                case "roundRectOutline" ->
                        createShader(new ByteArrayInputStream(roundRectOutline.getBytes()), GL_FRAGMENT_SHADER);
                case "roundedRect" ->
                        createShader(new ByteArrayInputStream(roundedRect.getBytes()), GL_FRAGMENT_SHADER);
                case "roundedRectGradient" ->
                        createShader(new ByteArrayInputStream(roundedRectGradient.getBytes()), GL_FRAGMENT_SHADER);
                case "gradient" -> createShader(new ByteArrayInputStream(gradient.getBytes()), GL_FRAGMENT_SHADER);
                case "mainmenu" -> createShader(new ByteArrayInputStream(mainmenu.getBytes()), GL_FRAGMENT_SHADER);
                case "kawaseUp" -> createShader(new ByteArrayInputStream(kawaseUp.getBytes()), GL_FRAGMENT_SHADER);
                case "kawaseDown" -> createShader(new ByteArrayInputStream(kawaseDown.getBytes()), GL_FRAGMENT_SHADER);
                case "kawaseUpBloom" ->
                        createShader(new ByteArrayInputStream(kawaseUpBloom.getBytes()), GL_FRAGMENT_SHADER);
                case "kawaseDownBloom" ->
                        createShader(new ByteArrayInputStream(kawaseDownBloom.getBytes()), GL_FRAGMENT_SHADER);
                case "gaussianBlur" ->
                        createShader(new ByteArrayInputStream(gaussianBlur.getBytes()), GL_FRAGMENT_SHADER);
                case "cape" -> createShader(new ByteArrayInputStream(cape.getBytes()), GL_FRAGMENT_SHADER);
                case "outline" -> createShader(new ByteArrayInputStream(outline.getBytes()), GL_FRAGMENT_SHADER);
                case "glow" -> createShader(new ByteArrayInputStream(glow.getBytes()), GL_FRAGMENT_SHADER);
                default ->
                        createShader(mc.getResourceManager().getResource(new ResourceLocation(fragmentShaderLoc)).getInputStream(), GL_FRAGMENT_SHADER);
            };
            glAttachShader(program, fragmentShaderID);

            int vertexShaderID = createShader(mc.getResourceManager().getResource(new ResourceLocation(vertexShaderLoc)).getInputStream(), GL_VERTEX_SHADER);
            glAttachShader(program, vertexShaderID);


        } catch (IOException e) {
            e.printStackTrace();
        }

        glLinkProgram(program);
        int status = glGetProgrami(program, GL_LINK_STATUS);

        if (status == 0) {
            throw new IllegalStateException("Shader failed to link!");
        }
        this.programID = program;
    }

    public ShaderUtils(String fragmentShaderLoc) {
        this(fragmentShaderLoc, "simp/shaders/vertex.vsh");
    }

    public void init() {
        glUseProgram(programID);
    }

    public void unload() {
        glUseProgram(0);
    }

    public int getUniform(String name) {
        return glGetUniformLocation(programID, name);
    }

    public void setUniformf(String name, float... args) {
        int loc = glGetUniformLocation(programID, name);
        switch (args.length) {
            case 1:
                glUniform1f(loc, args[0]);
                break;
            case 2:
                glUniform2f(loc, args[0], args[1]);
                break;
            case 3:
                glUniform3f(loc, args[0], args[1], args[2]);
                break;
            case 4:
                glUniform4f(loc, args[0], args[1], args[2], args[3]);
                break;
        }
    }

    public void setUniformi(String name, int... args) {
        int loc = glGetUniformLocation(programID, name);
        if (args.length > 1) glUniform2i(loc, args[0], args[1]);
        else glUniform1i(loc, args[0]);
    }

    public static void drawQuads(float x, float y, float width, float height) {
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2f(x, y);
        glTexCoord2f(0, 1);
        glVertex2f(x, y + height);
        glTexCoord2f(1, 1);
        glVertex2f(x + width, y + height);
        glTexCoord2f(1, 0);
        glVertex2f(x + width, y);
        glEnd();
    }

    public static void drawQuads() {
        ScaledResolution sr = new ScaledResolution(mc);
        float width = (float) sr.getScaledWidth_double();
        float height = (float) sr.getScaledHeight_double();
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1);
        glVertex2f(0, 0);
        glTexCoord2f(0, 0);
        glVertex2f(0, height);
        glTexCoord2f(1, 0);
        glVertex2f(width, height);
        glTexCoord2f(1, 1);
        glVertex2f(width, 0);
        glEnd();
    }

    public static void drawQuads(float width, float height) {
        drawQuads(0.0f, 0.0f, width, height);
    }

    public static void drawFixedQuads() {
        ScaledResolution sr = new ScaledResolution(mc);
        drawQuads((float) (mc.displayWidth / sr.getScaleFactor()), (float) mc.displayHeight / sr.getScaleFactor());
    }

    private int createShader(InputStream inputStream, int shaderType) {
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, readInputStream(inputStream));
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            System.out.println(glGetShaderInfoLog(shader, 4096));
            throw new IllegalStateException(String.format("Shader (%s) failed to compile!", shaderType));
        }

        return shader;
    }

    public static String readInputStream(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line).append('\n');

        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

            private final String shadow = """
            #version 120
            
            uniform sampler2D inTexture;
            uniform sampler2D textureToCheck;
            uniform vec2 texelSize;
            uniform vec2 direction;
            uniform float radius;
              uniform float strength;
            uniform float weights[256];
            
            void main() {
                vec2 uv = gl_TexCoord[0].st;
            
                if (direction.y > 0.0 && texture2D(textureToCheck, uv).a != 0.0) {
                    discard;
                }
            
                float alpha = texture2D(inTexture, uv).a * weights[0];
                float weightSum = weights[0];
            
                for (int i = 1; i <= int(radius); ++i) {
                    vec2 offset = texelSize * direction * float(i);
                    float w = weights[i];
            
                    alpha += texture2D(inTexture, clamp(uv + offset, vec2(0.0), vec2(1.0))).a * w;
                    alpha += texture2D(inTexture, clamp(uv - offset, vec2(0.0), vec2(1.0))).a * w;
                    weightSum += 2.0 * w;
                }
            
                alpha = (alpha / weightSum) * strength;
                gl_FragColor = vec4(0.0, 0.0, 0.0, alpha);
            }
            """;

    private final String roundRectTexture = """
            #version 120
            
            uniform vec2 location, rectSize;
            uniform sampler2D textureIn;
            uniform float radius, alpha;
            
            float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {
                return length(max(abs(centerPos) -size, 0.)) - radius;
            }
            
            void main() {
                float distance = roundedBoxSDF((rectSize * .5) - (gl_TexCoord[0].st * rectSize), (rectSize * .5) - radius - 1., radius);
                float smoothedAlpha =  (1.0-smoothstep(0.0, 2.0, distance)) * alpha;
                gl_FragColor = vec4(texture2D(textureIn, gl_TexCoord[0].st).rgb, smoothedAlpha);
            }""";

    private final String roundRectOutline = """
            #version 120
            
            uniform vec2 location, rectSize;
            uniform vec4 color, outlineColor;
            uniform float radius, outlineThickness;
            
            float roundedSDF(vec2 centerPos, vec2 size, float radius) {
                return length(max(abs(centerPos) - size + radius, 0.0)) - radius;
            }
            
            void main() {
                float distance = roundedSDF(gl_FragCoord.xy - location - (rectSize * .5), (rectSize * .5) + (outlineThickness *.5) - 1.0, radius);
            
                float blendAmount = smoothstep(0., 2., abs(distance) - (outlineThickness * .5));
            
                vec4 insideColor = (distance < 0.) ? color : vec4(outlineColor.rgb,  0.0);
                gl_FragColor = mix(outlineColor, insideColor, blendAmount);
            
            }""";

    private final String roundedRectGradient = """
            #version 120
            
            uniform vec2 location, rectSize;
            uniform vec4 color1, color2, color3, color4;
            uniform float radius;
            
            #define NOISE .5/255.0
            
            float roundSDF(vec2 p, vec2 b, float r) {
                return length(max(abs(p) - b , 0.0)) - r;
            }
            
            vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4){
                vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);
                //Dithering the color
                // from https://shader-tutorial.dev/advanced/color-banding-dithering/
                color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
                return color;
            }
            
            void main() {
                vec2 st = gl_TexCoord[0].st;
                vec2 halfSize = rectSize * .5;
               \s
               // use the bottom leftColor as the alpha
                float smoothedAlpha =  (1.0-smoothstep(0.0, 2., roundSDF(halfSize - (gl_TexCoord[0].st * rectSize), halfSize - radius - 1., radius)));
                vec4 gradient = createGradient(st, color1, color2, color3, color4);    gl_FragColor = vec4(gradient.rgb, gradient.a * smoothedAlpha);
            }""";


    private final String roundedRect = """
            #version 120
            
            uniform vec2 location, rectSize;
            uniform vec4 color;
            uniform float radius;
            uniform bool blur;
            
            float roundSDF(vec2 p, vec2 b, float r) {
                return length(max(abs(p) - b, 0.0)) - r;
            }
            
            void main() {
                vec2 rectHalf = rectSize * 0.5;
                gl_FragColor = vec4(color.rgb, (1.0-smoothstep(0.0, 1.0, roundSDF(rectHalf - (gl_TexCoord[0].st * rectSize), rectHalf - radius - 1.0, radius))) * color.a);
            }""";
    private final String kawaseUpBloom = """
            #version 120
            
            uniform sampler2D inTexture, textureToCheck;
            uniform vec2 halfpixel, offset, iResolution;
            uniform int check;
            uniform float strength;
            
            void main() {
              //  if(check && texture2D(textureToCheck, gl_TexCoord[0].st).a > 0.0) discard;
                vec2 uv = vec2(gl_FragCoord.xy / iResolution);
            
                vec4 sum = texture2D(inTexture, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);
                sum.rgb *= sum.a;
                vec4 smpl1 =  texture2D(inTexture, uv + vec2(-halfpixel.x, halfpixel.y) * offset);
                smpl1.rgb *= smpl1.a;
                sum += smpl1 * 2.0;
                vec4 smp2 = texture2D(inTexture, uv + vec2(0.0, halfpixel.y * 2.0) * offset);
                smp2.rgb *= smp2.a;
                sum += smp2;
                vec4 smp3 = texture2D(inTexture, uv + vec2(halfpixel.x, halfpixel.y) * offset);
                smp3.rgb *= smp3.a;
                sum += smp3 * 2.0;
                vec4 smp4 = texture2D(inTexture, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);
                smp4.rgb *= smp4.a;
                sum += smp4;
                vec4 smp5 = texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);
                smp5.rgb *= smp5.a;
                sum += smp5 * 2.0;
                vec4 smp6 = texture2D(inTexture, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);
                smp6.rgb *= smp6.a;
                sum += smp6;
                vec4 smp7 = texture2D(inTexture, uv + vec2(-halfpixel.x, -halfpixel.y) * offset);
                smp7.rgb *= smp7.a;
                sum += smp7 * 2.0;
                vec4 result = sum / 12.0;
                vec3 boosted = (result.rgb / result.a) * strength;
                gl_FragColor = vec4(boosted, mix(result.a, result.a * (1.0 - texture2D(textureToCheck, gl_TexCoord[0].st).a),check));
            }""";

    private final String kawaseDownBloom = """
            #version 120
            
            uniform sampler2D inTexture;
            uniform vec2 offset, halfpixel, iResolution;
            
            void main() {
                vec2 uv = vec2(gl_FragCoord.xy / iResolution);
                vec4 sum = texture2D(inTexture, gl_TexCoord[0].st);
                sum.rgb *= sum.a;
                sum *= 4.0;
                vec4 smp1 = texture2D(inTexture, uv - halfpixel.xy * offset);
                smp1.rgb *= smp1.a;
                sum += smp1;
                vec4 smp2 = texture2D(inTexture, uv + halfpixel.xy * offset);
                smp2.rgb *= smp2.a;
                sum += smp2;
                vec4 smp3 = texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);
                smp3.rgb *= smp3.a;
                sum += smp3;
                vec4 smp4 = texture2D(inTexture, uv - vec2(halfpixel.x, -halfpixel.y) * offset);
                smp4.rgb *= smp4.a;
                sum += smp4;
                vec4 result = sum / 8.0;
                gl_FragColor = vec4(result.rgb / result.a, result.a);
            }""";

    private final String kawaseUp = """
            #version 120
            
            uniform sampler2D inTexture, textureToCheck;
            uniform vec2 halfpixel, offset, iResolution;
            uniform int check;
            
            void main() {
                vec2 uv = vec2(gl_FragCoord.xy / iResolution);
                vec4 sum = texture2D(inTexture, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);
                sum += texture2D(inTexture, uv + vec2(-halfpixel.x, halfpixel.y) * offset) * 2.0;
                sum += texture2D(inTexture, uv + vec2(0.0, halfpixel.y * 2.0) * offset);
                sum += texture2D(inTexture, uv + vec2(halfpixel.x, halfpixel.y) * offset) * 2.0;
                sum += texture2D(inTexture, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);
                sum += texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset) * 2.0;
                sum += texture2D(inTexture, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);
                sum += texture2D(inTexture, uv + vec2(-halfpixel.x, -halfpixel.y) * offset) * 2.0;
            
                gl_FragColor = vec4(sum.rgb /12.0, mix(1.0, texture2D(textureToCheck, gl_TexCoord[0].st).a, check));
            }
            """;

    private final String kawaseDown = """
            #version 120
            
            uniform sampler2D inTexture;
            uniform vec2 offset, halfpixel, iResolution;
            
            void main() {
                vec2 uv = vec2(gl_FragCoord.xy / iResolution);
                vec4 sum = texture2D(inTexture, gl_TexCoord[0].st) * 4.0;
                sum += texture2D(inTexture, uv - halfpixel.xy * offset);
                sum += texture2D(inTexture, uv + halfpixel.xy * offset);
                sum += texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);
                sum += texture2D(inTexture, uv - vec2(halfpixel.x, -halfpixel.y) * offset);
                gl_FragColor = vec4(sum.rgb * .125, 1.0);
            }
            """;

    private final String gradient = """
            #version 120
            
            uniform vec2 location, rectSize;
            uniform sampler2D tex;
            uniform vec4 color1, color2, color3, color4;
            
            #define NOISE .5/255.0
            
            vec3 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4){
                vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);
                //Dithering the color from https://shader-tutorial.dev/advanced/color-banding-dithering/
                color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898,78.233))) * 43758.5453));
                return color;
            }
            void main() {
                vec2 coords = (gl_FragCoord.xy - location) / rectSize;
                float texColorAlpha = texture2D(tex, gl_TexCoord[0].st).a;
                gl_FragColor = vec4(createGradient(coords, color1, color2, color3, color4).rgb, texColorAlpha);
            }""";

    private final String mainmenu = """
            uniform float TIME;
            uniform vec2 RESOLUTION;
            
            #define NUM_OCTAVES 6
            
            mat3 rotX(float a) {
                float c = cos(a);
                float s = sin(a);
                return mat3(
                    1, 0, 0,
                    0, c, -s,
                    0, s, c
                );
            }
            
            mat3 rotY(float a) {
                float c = cos(a);
                float s = sin(a);
                return mat3(
                    c, 0, -s,
                    0, 1, 0,
                    s, 0, c
                );
            }
            
            float random(vec2 pos) {
                return fract(sin(dot(pos.xy, vec2(12.9898, 78.233))) * 43758.5453123);
            }
            
            float noise(vec2 pos) {
                vec2 i = floor(pos);
                vec2 f = fract(pos);
                float a = random(i + vec2(0.0, 0.0));
                float b = random(i + vec2(1.0, 0.0));
                float c = random(i + vec2(0.0, 1.0));
                float d = random(i + vec2(1.0, 1.0));
                vec2 u = f * f * (3.0 - 2.0 * f);
                return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
            }
            
            float fbm(vec2 pos) {
                float v = 0.0;
                float a = 0.5;
                vec2 shift = vec2(100.0);
                mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
                for (int i = 0; i < NUM_OCTAVES; i++) {
                    float dir = mod(float(i), 2.0) > 0.5 ? 1.0 : -1.0;
                    v += a * noise(pos - 0.05 * dir * TIME);
            
                    pos = rot * pos * 2.0 + shift;
                    a *= 0.5;
                }
                return v;
            }
            
            vec3 render(in vec2 fragCoord) {
                vec2 p = (fragCoord * 2.0 - RESOLUTION.xy) / min(RESOLUTION.x, RESOLUTION.y);
                p -= vec2(12.0, 0.0);
            
                float time2 = 1.0;
                vec2 q = vec2(0.0);
                q.x = fbm(p + 0.00 * time2);
                q.y = fbm(p + vec2(1.0));
                vec2 r = vec2(0.0);
                r.x = fbm(p + 1.0 * q + vec2(1.7, 9.2) + 0.15 * time2);
                r.y = fbm(p + 1.0 * q + vec2(8.3, 2.8) + 0.126 * time2);
                float f = fbm(p + r);
            
                vec3 color = mix(
                    vec3(0.3, 0.3, 0.6),
                    vec3(0.7, 0.7, 0.7),
                    clamp((f * f) * 4.0, 0.0, 1.0)
                );
            
                color = mix(
                    color,
                    vec3(0.7, 0.7, 0.7),
                    clamp(length(q), 0.0, 1.0)
                );
            
                color = mix(
                    color,
                    vec3(0.4, 0.4, 0.4),
                    clamp(length(r.x), 0.0, 1.0)
                );
            
                color = (f * f * f + 0.9 * f * f + 0.8 * f) * color;
            
                return color * 0.5;
            }
            
            void mainImage(out vec4 fragColor, in vec2 fragCoord) {
                vec3 color = render(fragCoord);
                fragColor = vec4(color, color.r);
            }
            
            void main(void) {
                mainImage(gl_FragColor, gl_FragCoord.xy);
            }
            """;

    private final String gaussianBlur = """
            #version 120
            
            uniform sampler2D textureIn;
            uniform vec2 texelSize;
            uniform vec2 direction;
            uniform float strength;
            uniform float radius;
            uniform float weights[128];
            
            #define offset (texelSize * direction)
            
            void main() {
                vec2 uv = gl_TexCoord[0].st;
                vec3 color = texture2D(textureIn, uv).rgb * weights[0];
            
                for (int i = 1; i < 128; ++i) {
                    if (i > int(radius)) break;
            
                    vec2 delta = float(i) * offset;
                    color += texture2D(textureIn, uv + delta).rgb * weights[i];
                    color += texture2D(textureIn, uv - delta).rgb * weights[i];
                }
            
                // apply strength to control blur intensity
                gl_FragColor = vec4(color * strength, 1.0);
            }
            """;

    private final String cape = """
            #extension GL_OES_standard_derivatives : enable
            
            #ifdef GL_ES
            precision highp float;
            #endif
            
            uniform float time;
            uniform vec2  resolution;
            uniform float zoom;
            
            #define PI 3.1415926535
            
            mat2 rotate3d(float angle)
            {
                return mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
            }
            
            void main()
            {
                vec2 p = (gl_FragCoord.xy * 2.0 - resolution) / min(resolution.x, resolution.y);
                p = rotate3d((time * 2.0) * PI) * p;
                float t;
                if (sin(time) == 10.0)
                    t = 0.075 / abs(1.0 - length(p));
                else
                    t = 0.075 / abs(0.4/*sin(time)*/ - length(p));
                gl_FragColor = vec4(     ( 1. -exp( -vec3(t)  * vec3(0.13*(sin(time)+12.0), p.y*0.7, 3.0) )) , 1.0);
            }""";

    private final String glow = """
            #version 120
            
            uniform sampler2D textureIn, textureToCheck;
            uniform vec2 texelSize, direction;
            uniform vec3 color;
            uniform bool avoidTexture;
            uniform float exposure, radius;
            uniform float weights[256];
            
            #define offset direction * texelSize
            
            void main() {
                if (direction.y == 1 && avoidTexture) {
                    if (texture2D(textureToCheck, gl_TexCoord[0].st).a != 0.0) discard;
                }
            
                float innerAlpha = texture2D(textureIn, gl_TexCoord[0].st).a * weights[0];
            
                for (float r = 1.0; r <= radius; r ++) {
                    innerAlpha += texture2D(textureIn, gl_TexCoord[0].st + offset * r).a * weights[int(r)];
                    innerAlpha += texture2D(textureIn, gl_TexCoord[0].st - offset * r).a * weights[int(r)];
                }
            
                gl_FragColor = vec4(color, mix(innerAlpha, 1.0 - exp(-innerAlpha * exposure), step(0.0, direction.y)));
            }
            """;

    private final String outline = """
            #version 120
            
            uniform vec2 texelSize, direction;
            uniform sampler2D texture;
            uniform float radius;
            uniform vec3 color;
            
            #define offset direction * texelSize
            
            void main() {
                float centerAlpha = texture2D(texture, gl_TexCoord[0].xy).a;
                float innerAlpha = centerAlpha;
                for (float r = 1.0; r <= radius; r++) {
                    float alphaCurrent1 = texture2D(texture, gl_TexCoord[0].xy + offset * r).a;
                    float alphaCurrent2 = texture2D(texture, gl_TexCoord[0].xy - offset * r).a;
            
                    innerAlpha += alphaCurrent1 + alphaCurrent2;
                }
            
                gl_FragColor = vec4(color, innerAlpha) * step(0.0, -centerAlpha);
            }
            
            
            """;
}