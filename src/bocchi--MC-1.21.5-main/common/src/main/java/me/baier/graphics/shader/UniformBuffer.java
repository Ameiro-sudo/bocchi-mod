package me.baier.graphics.shader;

import me.baier.graphics.MinecraftRenderInstance;

import static org.lwjgl.opengl.GL45C.*;

public class UniformBuffer implements AutoCloseable, MinecraftRenderInstance {
    private int buffer;

    /**
     * Returns the OpenGL buffer object name currently associated with this
     * object, or create and initialize it if not available. It may change in
     * the future if it is explicitly deleted.
     *
     * @return OpenGL buffer object
     */
    public final int getBufferID() {
        if (buffer == 0) {
            buffer = dsa() ? glCreateBuffers() : glGenBuffers();
        }
        return buffer;
    }

    /**
     * Binds this buffer to the indexed buffer target, as well as entirely to the binding
     * point in the array given by index. Each target has its own indexed array of buffer object
     * binding points.
     *
     * @param index  the index of the binding point within the array specified by {@code target}
     */
    public void bindBase(int index) {
        glBindBufferBase(GL_UNIFORM_BUFFER, index, getBufferID());
    }

    /**
     * Binds this buffer to the indexed buffer target, as well as a range within it to the
     * binding point in the array given by index. Each target has its own indexed array of buffer
     * object binding points.
     *
     * @param index  the index of the binding point within the array specified by {@code target}
     * @param offset the start offset in bytes into the buffer
     * @param size   the amount of data in bytes that can be read from the buffer object while used as an indexed
     *               target
     */
    public void bindRange(int index, long offset, long size) {
        glBindBufferRange(GL_UNIFORM_BUFFER, index, getBufferID(), offset, size);
    }

    /**
     * Creates the immutable data store of this buffer object.
     *
     * @param size the size of the data store in bytes
     */
    public void allocate(long size) {
        if (dsa()) {
            glNamedBufferData(getBufferID(), size, GL_DYNAMIC_DRAW);
        } else {
            glBindBuffer(GL_UNIFORM_BUFFER, getBufferID());
            nglBufferData(GL_UNIFORM_BUFFER, size, 0, GL_DYNAMIC_DRAW);
        }
    }

    /**
     * Modifies a subset of this buffer object's data store.
     *
     * @param offset the offset into the buffer object's data store where data replacement will begin, measured in
     *               bytes
     * @param size   the size in bytes of the data store region being replaced
     * @param data   a pointer to the new data that will be copied into the data store, can't be {@code NULL}
     */
    public void upload(long offset, long size, long data) {
        if (dsa()) {
            nglNamedBufferSubData(getBufferID(), offset, size, data);
        } else {
            glBindBuffer(GL_UNIFORM_BUFFER, getBufferID());
            nglBufferSubData(GL_UNIFORM_BUFFER, offset, size, data);
        }
    }

    @Override
    public void close() {
        if (buffer != 0) {
            glDeleteBuffers(buffer);
        }
        buffer = 0;
    }
}
