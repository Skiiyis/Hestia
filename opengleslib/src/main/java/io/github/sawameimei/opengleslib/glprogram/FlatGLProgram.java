package io.github.sawameimei.opengleslib.glprogram;

import android.opengl.GLES20;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;

import io.github.sawameimei.opengleslib.common.ShaderHelper;

/**
 * Created by huangmeng on 2018/1/15.
 */

public class FlatGLProgram {

    private ArrayList<ProgramPair> program = new ArrayList<>();

    public void amount(FragmentShader fragmentShader) {
        FlatVertexShader vertexShader = new FlatVertexShader();
        int vertexShaderHandle = vertexShader.compile();
        int fragmentShaderHandle = fragmentShader.compile();
        int programHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[]{"aPosition", "aTextureCoord"});
        program.add(new ProgramPair(programHandle, vertexShader, fragmentShader));
    }

    public void disAmount(FragmentShader fragmentShader) {
        for (ProgramPair programPair : program) {
            if (programPair.fragmentShader.equals(fragmentShader)) {
                GLES20.glDeleteProgram(programPair.programHandle);
                program.remove(programPair);
                break;
            }
        }
    }

    public void draw() {
        ProgramPair programPair = program.get(0);
    }

    public void release() {
        for (ProgramPair programPair : program) {
            GLES20.glDeleteProgram(programPair.programHandle);
        }
    }

    private static class ProgramPair {
        public int programHandle;
        public FragmentShader fragmentShader;
        public VertexShader vertexShader;


        public ProgramPair(int programHandle, VertexShader vertexShader, FragmentShader fragmentShader) {
            this.programHandle = programHandle;
            this.fragmentShader = fragmentShader;
            this.vertexShader = vertexShader;
        }
    }
}
