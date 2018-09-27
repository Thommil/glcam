package com.thommil.animalsgo.gl.plugins;

import android.opengl.GLES20;

import com.thommil.animalsgo.R;
import com.thommil.animalsgo.gl.UIPlugin;
import com.thommil.animalsgo.gl.libgl.GlColoredSprite;
import com.thommil.animalsgo.gl.libgl.GlDrawableBufferBatch;
import com.thommil.animalsgo.gl.libgl.GlIntRect;
import com.thommil.animalsgo.gl.libgl.GlOperation;
import com.thommil.animalsgo.gl.libgl.GlTexture;
import com.thommil.animalsgo.gl.libgl.GlTextureAtlas;
import com.thommil.animalsgo.utils.ResourcesLoader;

import org.json.JSONException;

import java.io.IOException;

public class UIDefault extends UIPlugin {

    private static final String TAG = "A_GO/Plugin/UIDefault";

    private static final String ID = "ui/default";
    private static final String PROGRAM_ID = "ui_default";

    private static final String ATLAS_FILE = "textures/ui_default.json";

    private int mTextureUniforHandle;
    private int mScreenRatioUniformHandle;

    private GlTextureAtlas mTextureAtlas;

    private GlColoredSprite mSmall;
    private GlColoredSprite mBig;
    private GlColoredSprite mLogo;
    GlDrawableBufferBatch mBatch;

    private final float[] mScreenRatio = new float[]{1f,1f};

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getProgramId() {
        return PROGRAM_ID;
    }

    @Override
    public String getName() {
        return this.mContext.getString(R.string.plugins_ui_default_name);
    }

    @Override
    public String getSummary() {
        return mContext.getString(R.string.plugins_ui_default_summary);
    }

    @Override
    public void allocate(final float surfaceRatio) {
        super.allocate(surfaceRatio);

        //Screen
        if(surfaceRatio < 1){
            mScreenRatio[0] = 1f;
            mScreenRatio[1] = surfaceRatio;
        }
        else if(surfaceRatio > 1){
            mScreenRatio[0] = surfaceRatio;
            mScreenRatio[1] = 1f;
        }


        //Scene
        try {
            mTextureAtlas = new GlTextureAtlas(new GlTexture() {
                @Override
                public int getMagnificationFilter() {
                    return GlTexture.MAG_FILTER_HIGH;
                }

                @Override
                public int getWrapMode(int axeId) {
                    return GlTexture.WRAP_CLAMP_TO_EDGE;
                }
            });

            mTextureAtlas.parseJON(mContext, ResourcesLoader.jsonFromAsset(mContext, ATLAS_FILE));
            mTextureAtlas.allocate();

            GlTextureAtlas.SubTexture subTexture = mTextureAtlas.getSubTexture("big");
            mLogo = new GlColoredSprite(mTextureAtlas.getTexture(), subTexture.x, subTexture.y, subTexture.width, subTexture.height);
            subTexture = mTextureAtlas.getSubTexture("small");
            mSmall = new GlColoredSprite(mTextureAtlas.getTexture(), subTexture.x, subTexture.y, subTexture.width, subTexture.height);
            mBig = new GlColoredSprite(mTextureAtlas.getTexture(), subTexture.x, subTexture.y, subTexture.width, subTexture.height);

        }catch(IOException ioe){
            throw new RuntimeException("Failed to load texture atlas : " + ioe);
        }catch(JSONException je){
            throw new RuntimeException("Failed to load texture atlas : " + je);
        }

        //Program
        mProgram.use();
        mTextureUniforHandle = mProgram.getUniformHandle(UNIFORM_TEXTURE);
        mScreenRatioUniformHandle = mProgram.getUniformHandle(UNIFORM_SCREEN_RATIO);

        //Buffer & Batch
        mBatch = new GlDrawableBufferBatch(mLogo);
        mBatch.setVertexAttribHandles(mProgram.getAttributeHandle(ATTRIBUTE_POSITION), mProgram.getAttributeHandle(ATTRIBUTE_TEXTCOORD),mProgram.getAttributeHandle(ATTRIBUTE_COLOR));
        //mBatch.allocate(GlBuffer.USAGE_DYNAMIC_DRAW, GlBuffer.TARGET_ARRAY_BUFFER, false);
        mLogo.size(0.5f,0.5f).position(0.0f, 0.0f);
        mBatch.commit();
        //Blend test (should be called each draw if another one is used)
        GlOperation.configureBlendTest(GlOperation.BLEND_FACTOR_SRC_ALPA, GlOperation.BLEND_FACTOR_ONE_MINUS_SRC_ALPA, GlOperation.BLEND_OPERATION_ADD, null);


    }

    float alpha = 1f;
    long time = 0;
    @Override
    public void draw(final GlIntRect viewport, final int orientation) {
        //Blend test
        GlOperation.setTestState(GlOperation.TEST_BLEND, true);
        //Log.d(TAG, CaptureBuilder.getInstance().getCapture().toString());
        //mLogo.setAlpha(Math.min(0f, 1f - CaptureBuilder.getInstance().getCapture().movement/3f));
        //mBatch.commit();

        //Program
        mProgram.use();
        GLES20.glUniform1i(mTextureUniforHandle, mTextureAtlas.getTexture().index);
        GLES20.glUniform2f(mScreenRatioUniformHandle, mScreenRatio[0], mScreenRatio[1]);

        //Texture
        mTextureAtlas.getTexture().bind();

        //Draw
        mBatch.draw(mProgram);
    }

    @Override
    public void free() {
        super.free();
        mBatch.free();
        mLogo.free();

        if(mTextureAtlas != null) {
            mTextureAtlas.free();
            mTextureAtlas = null;
        }
    }
}
