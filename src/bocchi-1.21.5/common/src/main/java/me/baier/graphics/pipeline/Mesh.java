package me.baier.graphics.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import me.baier.graphics.shader.Shader;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL33C;

import java.awt.*;
import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class Mesh {
    private final RenderPass pass;

    public Mesh mat(Matrix4f mat) {
        pass.setModel_mat(mat);
        return this;
    }

    public Mesh mat(PoseStack stack) {
        return this.mat(stack.last().pose());
    }

    public Shader shader() {
        return pass.shader();
    }

    private final VertexFormat format;
    private final VertexFormat.Mode mode;

    private final int primitiveVerticesSize;
    private final int primitiveIndicesCount;

    private ByteBuffer vertices;
    private long verticesPointerStart, verticesPointer;

    private ByteBuffer indices;
    private long indicesPointer;

    @Getter
    private int vertexI, indicesCount;

    @Getter
    private boolean building;

    public Mesh(RenderPass pass) {
        this.pass = pass;
        this.format = pass.vertexFormat;
        this.mode = pass.mode;

        this.indices = memAlloc(512 * Integer.BYTES);
        this.indicesPointer = memAddress0(indices);

        this.vertices = memAlloc(256 * Float.BYTES);
        this.verticesPointer = this.verticesPointerStart = memAddress0(vertices);

        this.primitiveVerticesSize = format.getVertexSize();
        this.primitiveIndicesCount = mode.primitiveLength;
    }

    public Mesh(Shader shader, VertexFormat format, VertexFormat.Mode mode) {
        this.pass = new RenderPass(format, mode);
        this.pass.setShader(shader);

        this.format = format;
        this.mode = mode;

        this.indices = memAlloc(512 * Integer.BYTES);
        this.indicesPointer = memAddress0(indices);

        this.vertices = memAlloc(256 * Float.BYTES);
        this.verticesPointer = this.verticesPointerStart = memAddress0(vertices);

        this.primitiveVerticesSize = format.getVertexSize();
        this.primitiveIndicesCount = mode.primitiveLength;
    }

    public void begin() {
        if (building) throw new IllegalStateException("Mesh.begin() called while already building.");

        building = true;

        verticesPointer = verticesPointerStart;
        vertexI = 0;
        indicesCount = 0;
    }

    public Mesh vec3(float x, float y, float z) {
        long p = verticesPointer;

        memPutFloat(p, x);
        memPutFloat(p + 4, y);
        memPutFloat(p + 8, z);

        verticesPointer += 12;

        return this;
    }

    public Mesh vec2(float x, float y) {
        return this.vec3(x, y, 0);
    }

    public Mesh color(float r, float g, float b, float a) {
        return this.color((int) r * 255, (int) g * 255, (int) b * 255, (int) a * 255);
    }

    public Mesh color(int r, int g, int b, int a) {
        long p = verticesPointer;

        memPutByte(p, (byte) r);
        memPutByte(p + 1, (byte) g);
        memPutByte(p + 2, (byte) b);
        memPutByte(p + 3, (byte) a);

        verticesPointer += 4;
        return this;
    }

    public Mesh color(Color c) {
        return this.color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    public int next() {
        ensureCapacity(1, 0);

        return vertexI++;
    }

    public void line(int i1, int i2) {
        ensureLineCapacity();

        long p = indicesPointer + indicesCount * 4L;

        memPutInt(p, i1);
        memPutInt(p + 4, i2);

        indicesCount += 2;
    }

    public void quad(int i1, int i2, int i3, int i4) {
        ensureQuadCapacity();

        long p = indicesPointer + indicesCount * 4L;

        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        memPutInt(p + 8, i3);

        memPutInt(p + 12, i3);
        memPutInt(p + 16, i4);
        memPutInt(p + 20, i1);

        indicesCount += 6;
    }

    public void triangle(int i1, int i2, int i3) {
        ensureTriCapacity();

        long p = indicesPointer + indicesCount * 4L;

        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        memPutInt(p + 8, i3);

        indicesCount += 3;
    }

    public void ensureQuadCapacity() {
        ensureCapacity(4, 6);
    }

    public void ensureTriCapacity() {
        ensureCapacity(3, 3);
    }

    public void ensureLineCapacity() {
        ensureCapacity(2, 2);
    }

    public void ensureCapacity(int vertexCount, int indexCount) {
        if ((vertexI + vertexCount) * primitiveVerticesSize >= vertices.capacity()) {
            int offset = getVerticesOffset();
            int newSize = vertices.capacity() * 2;

            ByteBuffer newVertices = memAlloc(newSize);
            memCopy(memAddress0(vertices), memAddress0(newVertices), offset);

            vertices = newVertices;
            verticesPointerStart = memAddress0(vertices);
            verticesPointer = verticesPointerStart + offset;
        }

        if ((indicesCount + indexCount) * Integer.BYTES >= indices.capacity()) {
            int newSize = indices.capacity() * 2;

            ByteBuffer newIndices = memAlloc(newSize);
            memCopy(memAddress0(indices), memAddress0(newIndices), indicesCount * 4L);

            memFree(indices);

            indices = newIndices;
            indicesPointer = memAddress0(indices);
        }
    }

    public void end() {
        if (!building) throw new IllegalStateException("Mesh.end() called while not building.");

        building = false;

        // size: 4 x indexCount
        pass.setIndexBuffer(getIndexBuffer(), VertexFormat.IndexType.INT);
        pass.setVertexBuffer(getVertexBuffer());
    }

    public void draw() {
        end();

        // force changing Blaze3D state
        for (int i = 0; i <= 3; i++) {
            GL33C.glBindSampler(i, 0);
        }
        GL33C.glDisable(GL33C.GL_STENCIL_TEST);
        RenderSystem.disableScissor();
        GL33C.glDisable(GL33C.GL_SCISSOR_TEST);
        GL33C.glBlendFuncSeparate(GL33C.GL_SRC_ALPHA, GL33C.GL_ONE_MINUS_SRC_ALPHA, GL33C.GL_ONE, GL33C.GL_ZERO);
        GlStateManager._enableBlend();
        GL33C.glEnable(GL33C.GL_BLEND);
        GL33C.glBlendEquation(GL33C.GL_FUNC_ADD);
        GlStateManager._disableDepthTest();
        GL33C.glDisable(GL33C.GL_DEPTH_TEST);
        GlStateManager._depthFunc(GL33C.GL_LEQUAL);
        GL33C.glDepthFunc(GL33C.GL_LEQUAL);
        GlStateManager._depthMask(true);
        GL33C.glDepthMask(true);
        GlStateManager._disableCull();
        RenderSystem.lineWidth(1);

        _draw();
    }

    public void _draw() {
        pass.drawIndexed(0, this.indicesCount);
    }

    private int getVerticesOffset() {
        return (int) (verticesPointer - verticesPointerStart);
    }

    public GpuBuffer getVertexBuffer() {
        vertices.limit(getVerticesOffset());
        return format.uploadImmediateVertexBuffer(this.vertices);
    }

    public GpuBuffer getIndexBuffer() {
        indices.limit(indicesCount * Integer.BYTES);
        return format.uploadImmediateIndexBuffer(indices);
    }
}
