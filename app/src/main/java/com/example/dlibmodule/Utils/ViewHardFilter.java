package com.example.dlibmodule.Utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlendMode;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.example.dlibmodule.R;

import java.nio.FloatBuffer;

import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.tools.GLESTools;

/**
 * Created by lake on 02/12/16.
 * librestreaming project.
 */
public class ViewHardFilter extends BaseHardVideoFilter {
    //private FakeView fakeViewGroup;
    private static final int ROOT_TEX_ID = 11;
    private SurfaceTexture rootViewSufaceTexture;
    private Surface rootViewSuface;

    private int face_size;
    private float boundingBox_left[];
    private float boundingBox_top[];
    private float boundingBox_right[];
    private float boundingBox_bottom[];
    private Bitmap mosaic_bitmap;
    private Bitmap face_bitmap;


    public ViewHardFilter(int size, float left[], float top[], float right[], float bottom[], Bitmap bitmap) {
        face_size = size;
        boundingBox_left = left;
        boundingBox_top = top;
        boundingBox_right = right;
        boundingBox_bottom = bottom;
        mosaic_bitmap = bitmap;
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        rootViewSufaceTexture = new SurfaceTexture(ROOT_TEX_ID);
        rootViewSufaceTexture.setDefaultBufferSize(VWidth, VHeight);
        rootViewSuface = new Surface(rootViewSufaceTexture);

        glViewProgram = GLESTools.createProgram(viewVertexShader_filter, viewFragmentshader_filter);
        GLES20.glUseProgram(glViewProgram);
        glViewTextureLoc = GLES20.glGetUniformLocation(glViewProgram, "uViewTexture");
        glViewCamTextureLoc = GLES20.glGetUniformLocation(glViewProgram, "uViewCamTexture");
        glViewPostionLoc = GLES20.glGetAttribLocation(glViewProgram, "aViewPosition");
        glViewTextureCoordLoc = GLES20.glGetAttribLocation(glViewProgram, "aViewTextureCoord");
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        //draw view
        Canvas canvas;

//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
//            canvas = rootViewSuface.lockHardwareCanvas();
//        else
            canvas = rootViewSuface.lockCanvas(null);

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        //fakeViewGroup.draw(canvas);
        Paint paint = new Paint();
        paint.setColor(Color.rgb(255, 217, 173));
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(2.0f);
        //paint.setMaskFilter(new BlurMaskFilter(12, BlurMaskFilter.Blur.NORMAL));

        // 인식한 얼굴의 위치에 맞춰서 모자이크 처리
        for(int i=0; i<face_size; i++) {
            canvas.drawBitmap(mosaic_bitmap, null, new RectF(boundingBox_left[i] / 7, boundingBox_top[i] / 7,
                    boundingBox_right[i] / 7, boundingBox_bottom[i] / 7), paint);
        }

        rootViewSuface.unlockCanvasAndPost(canvas);
        rootViewSufaceTexture.updateTexImage();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);

        //drawviewoncamera
        GLES20.glUseProgram(glViewProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ROOT_TEX_ID);
        GLES20.glUniform1i(glViewTextureLoc, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cameraTexture);
        GLES20.glUniform1i(glViewCamTextureLoc, 1);
        GLES20.glEnableVertexAttribArray(glViewPostionLoc);
        GLES20.glEnableVertexAttribArray(glViewTextureCoordLoc);
        shapeBuffer.position(0);
        GLES20.glVertexAttribPointer(glViewPostionLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, shapeBuffer);
        textrueBuffer.position(0);
        GLES20.glVertexAttribPointer(glViewTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, textrueBuffer);
        GLES20.glViewport(0, 0, SIZE_WIDTH, SIZE_HEIGHT);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawIndecesBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, drawIndecesBuffer);
        GLES20.glFinish();
        GLES20.glDisableVertexAttribArray(glViewPostionLoc);
        GLES20.glDisableVertexAttribArray(glViewTextureCoordLoc);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        rootViewSuface.release();
        rootViewSufaceTexture.release();
    }


    protected int glViewProgram;
    protected int glViewTextureLoc;
    protected int glViewCamTextureLoc;
    protected int glViewPostionLoc;
    protected int glViewTextureCoordLoc;
    protected String viewVertexShader_filter = "" +
            "attribute vec4 aViewPosition;\n" +
            "attribute vec2 aViewTextureCoord;\n" +
            "varying vec2 vViewTextureCoord;\n" +
            "void main(){\n" +
            "    gl_Position= aViewPosition;\n" +
            "    vViewTextureCoord = aViewTextureCoord;\n" +
            "}";
    protected String viewFragmentshader_filter = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying highp vec2 vViewTextureCoord;\n" +
            "uniform sampler2D uViewCamTexture;\n" +
            "uniform samplerExternalOES uViewTexture;\n" +
            "void main(){\n" +
            "    vec4  c2 = texture2D(uViewTexture, vec2(vViewTextureCoord.x,1.0-vViewTextureCoord.y));\n" +
            "    vec4  c1 = texture2D(uViewCamTexture, vViewTextureCoord);\n" +
            "    lowp vec4 outputColor = c2+c1*c1.a*(1.0-c2.a);\n" +
            "    gl_FragColor = outputColor;\n" +
            "}";
}
