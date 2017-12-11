package io.github.sawameimei.playopengles20.common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by huangmeng on 2017/12/11.
 */

public interface GLVertex {

    ByteBuffer toByteBuffer();

    //only 1 2 3 4
    int getSize();

    int getStride();

    int getCount();

    abstract class FloatGLVertex implements GLVertex {

        public static final int BYTE_SIZE_PRE_FLOAT = 4;
        private final float[] mVertex;
        private final ByteBuffer mVertexBuffer;

        public FloatGLVertex(float[] vertex) {
            this.mVertex = vertex;
            mVertexBuffer = ByteBuffer.allocateDirect(mVertex.length * BYTE_SIZE_PRE_FLOAT);
            mVertexBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer().put(mVertex);
        }

        @Override
        public abstract int getSize();

        @Override
        public int getStride() {
            return getSize() * BYTE_SIZE_PRE_FLOAT;
        }

        @Override
        public int getCount() {
            return mVertex.length / getSize();
        }

        @Override
        public ByteBuffer toByteBuffer() {
            return mVertexBuffer;
        }
    }
}
