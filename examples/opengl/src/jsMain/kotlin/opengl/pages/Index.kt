package opengl.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.foundation.layout.BoxScope
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.graphics.CanvasGl
import com.varabyte.kobweb.silk.components.graphics.ONE_FRAME_MS_60_FPS
import opengl.bindings.glMatrix.mat4
import opengl.bindings.toReadonlyVec3
import opengl.components.layouts.PageLayout
import org.khronos.webgl.*
import org.w3c.dom.Image
import kotlin.math.PI

typealias GL = WebGLRenderingContext // Just for readability

// There's... a lot going on here. Writing an OpenGL program from scratch is quite involved!
// The code is essentially a translation from
// https://developer.mozilla.org/en-US/docs/Web/API/WebGL_API/Tutorial/Getting_started_with_WebGL
// but Kotlin-y :)


private class ProgramInfo(
    val program: WebGLProgram,
    val attribLocations: AttribLocations,
    val uniformLocations: UniformLocations,
) {
    class AttribLocations(
        val vertexPosition: Int,
        val vertexNormal: Int,
        val textureCoord: Int,
    )

    class UniformLocations(
        val projectionMatrix: WebGLUniformLocation,
        val modelViewMatrix: WebGLUniformLocation,
        val normalMatrix: WebGLUniformLocation,
        val textureSampler: WebGLUniformLocation,
    )

    companion object {
        fun from(gl: GL): ProgramInfo {
            val program = initShaderProgram(gl)
            return ProgramInfo(
                program,
                AttribLocations(
                    vertexPosition = gl.getAttribLocation(program, "aVertexPosition"),
                    vertexNormal = gl.getAttribLocation(program, "aVertexNormal"),
                    textureCoord = gl.getAttribLocation(program, "aTextureCoord"),
                ),
                UniformLocations(
                    projectionMatrix = gl.getUniformLocation(program, "uProjectionMatrix")!!,
                    modelViewMatrix = gl.getUniformLocation(program, "uModelViewMatrix")!!,
                    normalMatrix = gl.getUniformLocation(program, "uNormalMatrix")!!,
                    textureSampler = gl.getUniformLocation(program, "uSampler")!!,
                )
            )
        }
    }
}

private class Buffers(
    val position: WebGLBuffer,
    val normal: WebGLBuffer,
    val textureCoord: WebGLBuffer,
    val indices: WebGLBuffer,
) {
    companion object {
        fun from(gl: GL): Buffers {
            return initBuffers(gl)
        }
    }
}

private class FrameData(
    val programInfo: ProgramInfo,
    val buffers: Buffers,
    val texture: WebGLTexture,
    var rotateRad: Double = 0.0,
) {
    companion object {
        fun from(gl: GL): FrameData {
            return FrameData(ProgramInfo.from(gl), Buffers.from(gl), loadTexture(gl, "/images/varabyte-face.png"))
        }
    }
}

private fun loadShader(gl: GL, type: Int, source: String): WebGLShader {
    val shader = gl.createShader(type)!!
    gl.shaderSource(shader, source)
    gl.compileShader(shader)

    if (!(gl.getShaderParameter(shader, GL.COMPILE_STATUS) as Boolean)) {
        error("An error occurred compiling a shader: " + gl.getShaderInfoLog(shader))
    }

    return shader
}

private fun initShaderProgram(gl: GL): WebGLProgram {
    val vertexShader = loadShader(
        gl, GL.VERTEX_SHADER, """
        attribute vec4 aVertexPosition;
        attribute vec3 aVertexNormal;
        attribute vec2 aTextureCoord;

        uniform mat4 uNormalMatrix;
        uniform mat4 uModelViewMatrix;
        uniform mat4 uProjectionMatrix;

        varying highp vec2 vTextureCoord;
        varying highp vec3 vLighting;

        void main() {
            gl_Position = uProjectionMatrix * uModelViewMatrix * aVertexPosition;
            vTextureCoord = aTextureCoord;

            // Apply lighting effect

            highp vec3 ambientLight = vec3(0.3, 0.3, 0.3);
            highp vec3 directionalLightColor = vec3(1, 1, 1);
            highp vec3 directionalVector = normalize(vec3(0.85, 0.8, 0.75));

            highp vec4 transformedNormal = uNormalMatrix * vec4(aVertexNormal, 1.0);

            highp float directional = max(dot(transformedNormal.xyz, directionalVector), 0.0);
            vLighting = ambientLight + (directionalLightColor * directional);
        }
        """.trimIndent()
    )
    val fragmentShader = loadShader(
        gl, GL.FRAGMENT_SHADER, """
        varying highp vec2 vTextureCoord;
        varying highp vec3 vLighting;

        uniform sampler2D uSampler;

        void main(void) {
            highp vec4 texelColor = texture2D(uSampler, vTextureCoord);

            gl_FragColor = vec4(texelColor.rgb * vLighting, texelColor.a);
        }
        """.trimIndent()
    )

    val shaderProgram = gl.createProgram()!!
    // Vertex shader
    gl.attachShader(shaderProgram, vertexShader)
    gl.attachShader(shaderProgram, fragmentShader)
    gl.linkProgram(shaderProgram)

    if (!(gl.getProgramParameter(shaderProgram, GL.LINK_STATUS) as Boolean)) {
        error("Unable to initialize the shader program: " + gl.getProgramInfoLog(shaderProgram))
    }

    return shaderProgram
}

private fun initBuffers(gl: GL): Buffers {
    val positionBuffer = gl.createBuffer()!!
    run {
        val corners = arrayOf(
            // Front face
            -1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,

            // Back face
            -1.0f, -1.0f, -1.0f,
            -1.0f, 1.0f, -1.0f,
            1.0f, 1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,

            // Top face
            -1.0f, 1.0f, -1.0f,
            -1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, -1.0f,

            // Bottom face
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f,

            // Right face
            1.0f, -1.0f, -1.0f,
            1.0f, 1.0f, -1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, -1.0f, 1.0f,

            // Left face
            -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, -1.0f,
        )

        gl.bindBuffer(GL.ARRAY_BUFFER, positionBuffer)
        gl.bufferData(GL.ARRAY_BUFFER, Float32Array(corners), GL.STATIC_DRAW)
    }

    val normalBuffer = gl.createBuffer()!!
    run {
        val vertexNormals = arrayOf(
            // Front
            0.0f,  0.0f,  1.0f,
            0.0f,  0.0f,  1.0f,
            0.0f,  0.0f,  1.0f,
            0.0f,  0.0f,  1.0f,

            // Back
            0.0f,  0.0f, -1.0f,
            0.0f,  0.0f, -1.0f,
            0.0f,  0.0f, -1.0f,
            0.0f,  0.0f, -1.0f,

            // Top
            0.0f,  1.0f,  0.0f,
            0.0f,  1.0f,  0.0f,
            0.0f,  1.0f,  0.0f,
            0.0f,  1.0f,  0.0f,

            // Bottom
            0.0f, -1.0f,  0.0f,
            0.0f, -1.0f,  0.0f,
            0.0f, -1.0f,  0.0f,
            0.0f, -1.0f,  0.0f,

            // Right
            1.0f,  0.0f,  0.0f,
            1.0f,  0.0f,  0.0f,
            1.0f,  0.0f,  0.0f,
            1.0f,  0.0f,  0.0f,

            // Left
            -1.0f,  0.0f,  0.0f,
            -1.0f,  0.0f,  0.0f,
            -1.0f,  0.0f,  0.0f,
            -1.0f,  0.0f,  0.0f,
        )

        gl.bindBuffer(GL.ARRAY_BUFFER, normalBuffer)
        gl.bufferData(GL.ARRAY_BUFFER, Float32Array(vertexNormals), GL.STATIC_DRAW)
    }

    val textureCoordBuffer = gl.createBuffer()!!
    run {
        val textureCoords = arrayOf(
            // Front
            0.0f,  0.0f,
            1.0f,  0.0f,
            1.0f,  1.0f,
            0.0f,  1.0f,
            // Back
            0.0f,  0.0f,
            1.0f,  0.0f,
            1.0f,  1.0f,
            0.0f,  1.0f,
            // Top
            0.0f,  0.0f,
            1.0f,  0.0f,
            1.0f,  1.0f,
            0.0f,  1.0f,
            // Bottom
            0.0f,  0.0f,
            1.0f,  0.0f,
            1.0f,  1.0f,
            0.0f,  1.0f,
            // Right
            0.0f,  0.0f,
            1.0f,  0.0f,
            1.0f,  1.0f,
            0.0f,  1.0f,
            // Left
            0.0f,  0.0f,
            1.0f,  0.0f,
            1.0f,  1.0f,
            0.0f,  1.0f,
        )

        gl.bindBuffer(GL.ARRAY_BUFFER, textureCoordBuffer)
        gl.bufferData(GL.ARRAY_BUFFER, Float32Array(textureCoords), GL.STATIC_DRAW)
    }

    val indexBuffer = gl.createBuffer()!!
    run {
        // This array defines each face as two triangles, using the
        // indices into the vertex array to specify each triangle's
        // position.

        val indices = arrayOf<Short>(
            0,  1,  2,      0,  2,  3,    // front
            4,  5,  6,      4,  6,  7,    // back
            8,  9,  10,     8,  10, 11,   // top
            12, 13, 14,     12, 14, 15,   // bottom
            16, 17, 18,     16, 18, 19,   // right
            20, 21, 22,     20, 22, 23,   // left
        )

        gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, indexBuffer)
        gl.bufferData(GL.ELEMENT_ARRAY_BUFFER, Uint16Array(indices), GL.STATIC_DRAW)
    }

    return Buffers(positionBuffer, normalBuffer, textureCoordBuffer, indexBuffer)
}

private fun loadTexture(gl: GL, url: String): WebGLTexture {
    fun isPowerOf2(value: Int): Boolean {
        return (value.and(value - 1) == 0)
    }

    val texture = gl.createTexture()!!
    gl.bindTexture(GL.TEXTURE_2D, texture)

    // Because images have to be downloaded over the internet
    // they might take a moment until they are ready.
    // Until then put a single pixel in the texture so we can
    // use it immediately. When the image has finished downloading
    // we'll update the texture with the contents of the image.
    gl.texImage2D(
        GL.TEXTURE_2D,
        level = 0,
        internalformat = GL.RGBA,
        width = 1,
        height = 1,
        border = 0,
        format = GL.RGBA,
        type = GL.UNSIGNED_BYTE,
        pixels = Uint8Array(arrayOf(0, 0, 255.toByte(), 255.toByte())) // Blue
    )

    val image = Image()
    image.onload = {
        gl.bindTexture(GL.TEXTURE_2D, texture)
        gl.texImage2D(
            GL.TEXTURE_2D,
            level = 0,
            internalformat = GL.RGBA,
            format = GL.RGBA,
            type = GL.UNSIGNED_BYTE,
            source = image
        )

        // WebGL1 has different requirements for power of 2 images
        // vs non power of 2 images so check if the image is a
        // power of 2 in both dimensions.
        if (isPowerOf2(image.width) && isPowerOf2(image.height)) {
            // Yes, it's a power of 2. Generate mips.
            gl.generateMipmap(GL.TEXTURE_2D)
        } else {
            // No, it's not a power of 2. Turn off mips and set
            // wrapping to clamp to edge
            gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
            gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
            gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR)
        }
    }
    image.src = url

    return texture
}


@Composable
private fun BoxScope.OpenGlScene() {
    var frameData: FrameData? = null

    CanvasGl(640, 480, Modifier.align(Alignment.Center), minDeltaMs = ONE_FRAME_MS_60_FPS) {
        val gl = ctx // Just for readability

        frameData = frameData ?: FrameData.from(gl)
        with(frameData!!) {
            rotateRad += (elapsedMs / 1000.0)

            gl.clearColor(0.0f, 0.0f, 0.0f, 1.0f)
            gl.clearDepth(1.0f)
            gl.enable(GL.DEPTH_TEST)
            gl.depthFunc(GL.LEQUAL)
            gl.clear(GL.COLOR_BUFFER_BIT.or(GL.DEPTH_BUFFER_BIT))

            val projectionMatrix = mat4.create()
            mat4.perspective(
                projectionMatrix,
                fov = 45.0 * PI / 180.0,
                aspect = width / height.toFloat(),
                zNear = 0.1f,
                zFar = 100f
            )

            val modelViewMatrix = mat4.create()
            val translateBy = arrayOf(0.0f, 0.0f, -6.0f).toReadonlyVec3()
            mat4.translate(modelViewMatrix, modelViewMatrix, translateBy)
            val rotateAxisZ = arrayOf(0.0f, 0.0f, 1.0f).toReadonlyVec3()
            mat4.rotate(modelViewMatrix, modelViewMatrix, rotateRad, rotateAxisZ)
            val rotateAxisX = arrayOf(0.0f, 1.0f, 0.0f).toReadonlyVec3()
            mat4.rotate(modelViewMatrix, modelViewMatrix, rotateRad * 0.7, rotateAxisX)

            val normalMatrix = mat4.create()
            mat4.invert(normalMatrix, modelViewMatrix)
            mat4.transpose(normalMatrix, normalMatrix)

            gl.bindBuffer(GL.ARRAY_BUFFER, buffers.position)
            gl.vertexAttribPointer(
                programInfo.attribLocations.vertexPosition,
                size = 3,
                type = GL.FLOAT,
                normalized = false,
                stride = 0,
                offset = 0,
            )
            gl.enableVertexAttribArray(programInfo.attribLocations.vertexPosition)

            gl.bindBuffer(GL.ARRAY_BUFFER, buffers.normal)
            gl.vertexAttribPointer(
                programInfo.attribLocations.vertexNormal,
                size = 3,
                type = GL.FLOAT,
                normalized = false,
                stride = 0,
                offset = 0,
            )
            gl.enableVertexAttribArray(programInfo.attribLocations.vertexNormal)

            gl.bindBuffer(GL.ARRAY_BUFFER, buffers.textureCoord)
            gl.vertexAttribPointer(
                programInfo.attribLocations.textureCoord,
                size = 2,
                type = GL.FLOAT,
                normalized = false,
                stride = 0,
                offset = 0,
            )
            gl.enableVertexAttribArray(programInfo.attribLocations.textureCoord)

            gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, buffers.indices)

            gl.useProgram(programInfo.program)

            gl.uniformMatrix4fv(programInfo.uniformLocations.projectionMatrix, transpose = false, projectionMatrix)
            gl.uniformMatrix4fv(programInfo.uniformLocations.modelViewMatrix, transpose = false, modelViewMatrix)
            gl.uniformMatrix4fv(programInfo.uniformLocations.normalMatrix, transpose = false, normalMatrix)

            gl.activeTexture(GL.TEXTURE0)
            gl.bindTexture(GL.TEXTURE_2D, texture)
            gl.uniform1i(programInfo.uniformLocations.textureSampler, 0)

            gl.drawElements(GL.TRIANGLES, count = 36, type = GL.UNSIGNED_SHORT, offset = 0)
        }
    }
}

@Page
@Composable
fun OpenGlPage() {
    PageLayout {
        OpenGlScene()
    }
}