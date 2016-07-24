package org.bewellapp.wallpaper;

import static android.opengl.GLES10.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.bewellapp.ScoreComputation.ScoreComputationService;

import org.bewellapp.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.util.Log;

/**
 * OpenGL renderer that displays the user's wellness as an aquarium.
 * 
 * @author gcardone
 * 
 */
public class BeWellFishRenderer implements GLSurfaceView.Renderer, GLWallpaperService.Renderer {

	static final int FISHFRAMES = 18;
	static final int BGFRAMES = 20;
	static final int SCHLFRAMES = 18;
	/**
	 * Maximum number of frames per second to display.
	 */
	static final int FPS_LIMIT = 24;
	static final Random random = new Random(System.currentTimeMillis());
	private static final String TAG = "BeWellFishRenderer";
	/*
	 * change FPS_LIMIT to change the maximum FPS
	 */
	private static final long FRAME_MIN_INTERVAL = 1000L / FPS_LIMIT;

	private Context mContext;
	private boolean mIsBlueFishTextureInited;
	int[] mTexGoodBlueFishRL;
	int[] mTexPoorBlueFishRL;
	int[] mTexGoodBlueFishLR;
	int[] mTexPoorBlueFishLR;
	private Background mBackground;
	private Fish mFish;
	private List<BlueFish> mSchoolOfFish;
	private List<Sprite> mZOrdererObjects;
	private long mLastTick;
	private int mWidth, mHeight;
	int mPhysAct, mSleep, mSocialAct; // from 0 - 100
	int mPhysActOld, mSleepOld, mSocialActOld; // from 0 - 100
	private float mXOffset;
	private boolean mScrolling;
	boolean mHasToReorderSprites;
	boolean mPendingSchoolFishResize;
	int mNewSchoolOfFishSize;

	/*
	 * Fish oscillate up and down following a sin curve. Calculating sin(x) is a
	 * relatively expensive function (especially on phones that do not have a
	 * FPU, thus we store in an array pre-calculated values. table of sin(x) x
	 * in [0, 2pi] 0.05 step to avoid calculating it often
	 */
	private static final float[] SIN_TABLE = new float[] { 0.000f, 0.050f, 0.100f, 0.149f, 0.199f, 0.247f, 0.296f,
			0.343f, 0.389f, 0.435f, 0.479f, 0.523f, 0.565f, 0.605f, 0.644f, 0.682f, 0.717f, 0.751f, 0.783f, 0.813f,
			0.841f, 0.867f, 0.891f, 0.913f, 0.932f, 0.949f, 0.964f, 0.976f, 0.985f, 0.993f, 0.997f, 1.000f, 1.000f,
			0.997f, 0.992f, 0.984f, 0.974f, 0.961f, 0.946f, 0.929f, 0.909f, 0.887f, 0.863f, 0.837f, 0.808f, 0.778f,
			0.746f, 0.711f, 0.675f, 0.638f, 0.598f, 0.558f, 0.516f, 0.472f, 0.427f, 0.382f, 0.335f, 0.287f, 0.239f,
			0.190f, 0.141f, 0.091f, 0.042f, -0.008f, -0.058f, -0.108f, -0.158f, -0.207f, -0.256f, -0.304f, -0.351f,
			-0.397f, -0.443f, -0.487f, -0.530f, -0.572f, -0.612f, -0.651f, -0.688f, -0.723f, -0.757f, -0.789f, -0.818f,
			-0.846f, -0.872f, -0.895f, -0.916f, -0.935f, -0.952f, -0.966f, -0.978f, -0.987f, -0.994f, -0.998f, -1.000f,
			-0.999f, -0.996f, -0.991f, -0.982f, -0.972f, -0.959f, -0.944f, -0.926f, -0.906f, -0.883f, -0.859f, -0.832f,
			-0.804f, -0.773f, -0.740f, -0.706f, -0.669f, -0.631f, -0.592f, -0.551f, -0.508f, -0.465f, -0.420f, -0.374f,
			-0.327f, -0.279f, -0.231f, -0.182f, -0.133f, -0.083f, -0.033f };
	private static final int SIN_TABLE_LEN = SIN_TABLE.length;

	class Background extends Sprite {
		/*
		 * Background, bluefish and (main) fish all work in the same way. Look
		 * at the comments in the Fish class for details.
		 */
		private FloatBuffer mFVertexBuffer;
		private FloatBuffer mFTexBuffer;
		private ShortBuffer mIndexBuffer;
		private final int VERTS = 4;
		private float mZDepth;
		private int mTexFrameIdx;
		private int[] mTexture;
		private boolean mIsTextureInited;
		private boolean mPendingTextureUpdate;
		private BackgroundMood mNewMood;
		private BackgroundMood mCurrentMood;

		public Background() {
			float bottom = -2f;
			float top = -bottom;
			float left = -1f;
			float right = -left;
			float[][] coords = { { left, bottom, 0 }, { right, bottom, 0 }, { left, top, 0 }, { right, top, 0 } };
			ByteBuffer cbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
			cbb.order(ByteOrder.nativeOrder());
			mFVertexBuffer = cbb.asFloatBuffer();
			for (int i = 0; i < VERTS; i++) {
				mFVertexBuffer.put(coords[i]);
			}
			short[] myIndecesArray = { 0, 1, 2, 3 };
			ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
			ibb.order(ByteOrder.nativeOrder());
			mIndexBuffer = ibb.asShortBuffer();
			// stuff that into the buffer
			for (int i = 0; i < coords.length; i++) {
				mIndexBuffer.put(myIndecesArray[i]);
			}

			ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
			tbb.order(ByteOrder.nativeOrder());
			mFTexBuffer = tbb.asFloatBuffer();
			mFTexBuffer.put(-0.25f * 2f + 0.5f); // 0
			mFTexBuffer.put(0.25f * 2f + 0.5f); // 1
			mFTexBuffer.put(0.25f * 2f + 0.5f); // 1
			mFTexBuffer.put(0.25f * 2f + 0.5f); // 1
			mFTexBuffer.put(-0.25f * 2f + 0.5f); // 0
			mFTexBuffer.put(-0.25f * 2f + 0.5f); // 0
			mFTexBuffer.put(0.25f * 2f + 0.5f); // 1
			mFTexBuffer.put(-0.25f * 2f + 0.5f); // 0

			mFVertexBuffer.position(0);
			mIndexBuffer.position(0);
			mFTexBuffer.position(0);
			mZDepth = -1.9f;
			mTexFrameIdx = 0;
			mIsTextureInited = false;
			mCurrentMood = BackgroundMood.UNDEFINED;
		}

		void appendTextureUpdate(BackgroundMood mood) {
			if (mood != mCurrentMood) {
				mNewMood = mood;
				mPendingTextureUpdate = true;
			}
		}

		public void draw(GL10 gl) {
			if (mPendingTextureUpdate) {
				mPendingTextureUpdate = false;
				loadBackgroundTexture(mNewMood);
			}
			if (!mIsTextureInited) {
				return;
			}
			glPushMatrix();
			glScalef(2.0f, 1.6f, 1);
			glTranslatef(0, 0, mZDepth);
			glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
			glBindTexture(GL_TEXTURE_2D, mTexture[mTexFrameIdx]);
			glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexCoordPointer(2, GL_FLOAT, 0, mFTexBuffer);
			// type of shape
			// Number of indices
			// How big each index is
			// buffer containing the 3 indices
			glDrawElements(GL10.GL_TRIANGLE_STRIP, VERTS, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
			glPopMatrix();
		}

		void loadBackgroundTexture(BackgroundMood mood) {
			mCurrentMood = mood;
			int file;
			if (mIsTextureInited) {
				glDeleteTextures(BGFRAMES, mTexture, 0);
			}
			mIsTextureInited = true;
			mTexture = new int[BGFRAMES];
			glGenTextures(BGFRAMES, mTexture, 0);
			switch (mood) {
			case POOR:
				file = R.drawable.bg_poor000;
				break;
			case NORMAL:
				file = R.drawable.bg_norm000;
				break;
			case GOOD:
				file = R.drawable.bg_good000;
				break;
			default:
				file = R.drawable.bg_norm000;
				break;
			}
			loadTexture(BGFRAMES, file, mTexture);
		}

		public void tick() {
			/*
			 * The only background animation is to change texture.
			 */
			mTexFrameIdx++;
			if (mTexFrameIdx > BGFRAMES - 1 || mTexFrameIdx < 0) {
				mTexFrameIdx = 0; // start at beginning of loop
			}
		}
	}

	class BlueFish extends Sprite {
		private FloatBuffer mFVertexBuffer;
		private FloatBuffer mFTexBuffer;
		private ShortBuffer mIndexBuffer;
		private final int VERTS = 4;
		private int mTexFrameIdx;
		private boolean mIsPoor;
		private float mSpeed;
		private float mXCoord, mYCoord;
		private float mLeftBoundary, mRightBoundary;
		private int mUpdownidx;
		private int mUpdownStep;
		private int[] mCurrentTexture;
		private static final float SCENE_LEFT_BOUNDARY = -2.0f;

		public BlueFish() {
			float bottom = -0.25f;
			float top = -bottom;
			float left = -0.25f;
			float right = -left;
			float[][] coords = { { left, bottom, 0 }, { right, bottom, 0 }, { left, top, 0 }, { right, top, 0 } };
			ByteBuffer cbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
			cbb.order(ByteOrder.nativeOrder());
			mFVertexBuffer = cbb.asFloatBuffer();
			for (int i = 0; i < VERTS; i++) {
				mFVertexBuffer.put(coords[i]);
			}
			short[] myIndecesArray = { 0, 1, 2, 3 };
			ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
			ibb.order(ByteOrder.nativeOrder());
			mIndexBuffer = ibb.asShortBuffer();
			// stuff that into the buffer
			for (int i = 0; i < coords.length; i++) {
				mIndexBuffer.put(myIndecesArray[i]);
			}

			ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
			tbb.order(ByteOrder.nativeOrder());
			mFTexBuffer = tbb.asFloatBuffer();
			mFTexBuffer.put(-0.25f * 2f + 0.5f); // 0
			mFTexBuffer.put(0.25f * 2f + 0.5f); // 1
			mFTexBuffer.put(0.25f * 2f + 0.5f); // 1
			mFTexBuffer.put(0.25f * 2f + 0.5f); // 1
			mFTexBuffer.put(-0.25f * 2f + 0.5f); // 0
			mFTexBuffer.put(-0.25f * 2f + 0.5f); // 0
			mFTexBuffer.put(0.25f * 2f + 0.5f); // 1
			mFTexBuffer.put(-0.25f * 2f + 0.5f); // 0

			mFVertexBuffer.position(0);
			mIndexBuffer.position(0);
			mFTexBuffer.position(0);
			zDepth = 0.9f;
			mTexFrameIdx = 0;
			randomizeFish();
		}

		public void draw(GL10 gl) {
			glPushMatrix();
			glTranslatef(mXCoord, mYCoord + SIN_TABLE[mUpdownidx] * 0.05f, zDepth);
			glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glBindTexture(GL_TEXTURE_2D, mCurrentTexture[mTexFrameIdx]);
			glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexCoordPointer(2, GL_FLOAT, 0, mFTexBuffer);
			// type of shape
			// Number of indices
			// How big each index is
			// buffer containing the 3 indices
			glDrawElements(GL10.GL_TRIANGLE_STRIP, VERTS, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
			glPopMatrix();
		}

		/**
		 * Randomize fish position and speed.
		 */
		private void randomizeFish() {
			/*
			 * Randomize fish direction.
			 */
			boolean leftToRight = random.nextBoolean();
			/*
			 * Randomize fish z-depth.
			 */
			zDepth = random.nextFloat() * 3.5f - 1.85f;
			/*
			 * Use blurry texture if z < 0.5 (i.e. the fish is "far"
			 */
			mIsPoor = zDepth < 0.5f;
			/*
			 * Randomize speed.
			 */
			mSpeed = 0.01f + random.nextFloat() * 0.01f;
			if (!leftToRight) {
				mSpeed = -mSpeed;
			}
			/*
			 * Randomize fish X position
			 */
			if (leftToRight) {
				mXCoord = SCENE_LEFT_BOUNDARY - random.nextFloat() * 0.1f;
				mLeftBoundary = mXCoord - random.nextFloat() * 0.2f;
				mRightBoundary = -mLeftBoundary;
			} else {
				mXCoord = -SCENE_LEFT_BOUNDARY + random.nextFloat() * 0.1f;
				mRightBoundary = mXCoord + random.nextFloat() * 0.2f;
				mLeftBoundary = -mRightBoundary;
			}
			/*
			 * Randomize Y position
			 */
			mYCoord = random.nextFloat() * 2.2f - 1.1f;
			/*
			 * Select a random index in the sin table to avoid fish being
			 * "coordinated"
			 */
			mUpdownidx = random.nextInt(SIN_TABLE_LEN - 2);
			/*
			 * Change speed of "up-down" motion.
			 */
			mUpdownStep = 1 + random.nextInt(2);
			/*
			 * Select a random texture frame.
			 */
			mTexFrameIdx = random.nextInt(SCHLFRAMES);

			/*
			 * The fish may have changed it's z-order compared to other sprites,
			 * thus we signal to the main loop that it should reorder sprites.
			 */
			mHasToReorderSprites = true;
			/*
			 * Load the texture.
			 */
			updateTexture();
		}

		public void tick() {
			/*
			 * Increase texture frame index.
			 */
			mTexFrameIdx++;
			if (mTexFrameIdx > SCHLFRAMES - 1 || mTexFrameIdx < 0) {
				mTexFrameIdx = 0; // start at beginning of loop
			}
			/*
			 * Increase index in the sin table for the "up-down" effect.
			 */
			mUpdownidx = mUpdownidx > (SIN_TABLE_LEN - 1 - mUpdownStep) ? 0 : (mUpdownidx + 1);

			/*
			 * Change X coordinate by mSpeed
			 */
			mXCoord += mSpeed;

			/*
			 * If we are out of sight, randomize fish.
			 */
			if (mSpeed > 0) {
				if (mXCoord > mRightBoundary) {
					randomizeFish();
				}
			} else {
				if (mXCoord < mLeftBoundary) {
					randomizeFish();
				}
			}
		}

		/**
		 * Load correct texture: blurry/non-blurry, right-to-left/left-to-right
		 */
		private void updateTexture() {
			if (mSpeed > 0) {
				if (mIsPoor) {
					mCurrentTexture = mTexPoorBlueFishLR;
				} else {
					mCurrentTexture = mTexGoodBlueFishLR;
				}
			} else {
				if (mIsPoor) {
					mCurrentTexture = mTexPoorBlueFishRL;
				} else {
					mCurrentTexture = mTexGoodBlueFishRL;
				}
			}
		}

	}

	class Fish extends Sprite {
		/**
		 * mFVertexBuffer is a native buffer that contains the coordinates of
		 * the vertices that define the fish's polygon.
		 */
		private FloatBuffer mFVertexBuffer;
		/**
		 * mFTexBuffer is a native buffer that contains the coordinates that
		 * match texture's point to polygon's points.
		 */
		private FloatBuffer mFTexBuffer;
		/**
		 * mIndexBuffer is a native buffer that contains ordered list of vertex
		 * that define the polygon (the same set of vertices can define
		 * different polygons based on how we order them.
		 */
		private ShortBuffer mIndexBuffer;
		/**
		 * Constant: how many vertices define the polygon;
		 */
		private final int VERTS = 4;
		/**
		 * Index of the texture frame to display
		 */
		private int mTexFrameIdx;
		/**
		 * Current x and y coordinates of the fish. The z coordinate is
		 * inherited from the Sprite class.
		 */
		float xCoord, yCoord;
		/**
		 * Stores the Y coordinate the fish was created at (horizontal line of
		 * swimming)
		 */
		private float mYStartCoord;
		/**
		 * Index in the SIN_TABLE
		 */
		private int mUpdownidx;
		/**
		 * OpenGL texture array
		 */
		private int[] mTexture;
		/**
		 * Flag to check that textures are loaded
		 */
		private boolean mIsTextureInited;
		/**
		 * Flag to signal that textures should be updated
		 */
		private boolean mPendingTextureUpdate;
		/**
		 * Field that stores the fish's new speed (makes sense only if the
		 * texture should be updated.
		 */
		private FishSpeed mNewSpeed;
		/**
		 * Current speed of the fish.
		 */
		private FishSpeed mCurrentSpeed;

		public Fish() {
			/*
			 * Scale up or down these values to make the fish bigger (or
			 * smaller). They define the top, bottom, left and right boundaries
			 * of the fish's polygon.
			 */
			float bottom = -0.35f;
			float top = -bottom;
			float left = -0.35f;
			float right = -left;
			/*
			 * Store in coords the (x,y,z) coordinates of each vertex of the
			 * polygon.
			 */
			float[][] coords = { { left, bottom, 0 }, { right, bottom, 0 }, { left, top, 0 }, { right, top, 0 } };

			/*
			 * OpenGL needs to work on native byte arrays, thus we allocate
			 * them. mFVertexBuffer contains the vertices' coordinates, thus we
			 * need to allocate (# of vertices) x (# of coordinates per
			 * vertices) x (size of coordinate type). We have 4 vertices, each
			 * one has 3 coordinates (i.e.: x, y, z), each coordinate is a float
			 * (4 bytes).
			 */
			ByteBuffer cbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
			cbb.order(ByteOrder.nativeOrder());
			mFVertexBuffer = cbb.asFloatBuffer();
			for (int i = 0; i < VERTS; i++) {
				/*
				 * Copy coordinates from coords to the native buffer.
				 */
				mFVertexBuffer.put(coords[i]);
			}

			/*
			 * Store in mIndexBuffer the order of vertices as a native buffer.
			 * Shorts are 2 bytes long, thus we allocate VERTS * 2 bytes.
			 */
			short[] myIndecesArray = { 0, 1, 2, 3 };
			ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
			ibb.order(ByteOrder.nativeOrder());
			mIndexBuffer = ibb.asShortBuffer();
			// stuff that into the buffer
			for (int i = 0; i < coords.length; i++) {
				mIndexBuffer.put(myIndecesArray[i]);
			}

			/*
			 * Map texture to polygon. Texture coordinates are mapped in a [0.0
			 * .. 1.0] space .
			 */
			ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
			tbb.order(ByteOrder.nativeOrder());
			mFTexBuffer = tbb.asFloatBuffer();
			mFTexBuffer.put(-0.25f * 2f + 0.5f); // 0
			mFTexBuffer.put(0.25f * 2f + 0.5f); // 1
			mFTexBuffer.put(0.25f * 2f + 0.5f); // 1
			mFTexBuffer.put(0.25f * 2f + 0.5f); // 1
			mFTexBuffer.put(-0.25f * 2f + 0.5f); // 0
			mFTexBuffer.put(-0.25f * 2f + 0.5f); // 0
			mFTexBuffer.put(0.25f * 2f + 0.5f); // 1
			mFTexBuffer.put(-0.25f * 2f + 0.5f); // 0

			/*
			 * Native buffer need to be "rewound" before being used.
			 */
			mFVertexBuffer.position(0);
			mIndexBuffer.position(0);
			mFTexBuffer.position(0);
			/*
			 * We randomize the z-depth of the fish
			 */
			zDepth = 1.0f + random.nextFloat() * 0.85f;
			/*
			 * Set the current index of the texture to display as 0
			 */
			mTexFrameIdx = 0;
			xCoord = 1.5f;
			/*
			 * Randomize the y coordinate.
			 */
			yCoord = random.nextFloat() * 2.0f - 1.0f;
			/*
			 * Flag to signal that there are no textures ready.
			 */
			mIsTextureInited = false;
			/*
			 * Unknown current speed.
			 */
			mCurrentSpeed = FishSpeed.UNDEFINED;
		}

		/**
		 * Set flag to signal that new textures must be loaded.
		 * 
		 * @param speed
		 *            Speed of the fish, loads _slow, _normal or _fast textures.
		 */
		void appendTextureUpdate(FishSpeed speed) {
			if (mCurrentSpeed != speed) {
				mNewSpeed = speed;
				mPendingTextureUpdate = true;
			}
		}

		public void draw(GL10 gl) {
			/*
			 * Textures can be loaded only in a OpenGL thread. We load them now
			 * if we need.
			 */
			if (mPendingTextureUpdate) {
				mPendingTextureUpdate = false;
				loadFishTexture(mNewSpeed);
			}
			if (!mIsTextureInited) {
				return;
			}
			/*
			 * Save the current matrix stack.
			 */
			glPushMatrix();
			/*
			 * Move to the x, y, z coordinate where to put the fish.
			 */
			glTranslatef(xCoord, yCoord, zDepth);
			/*
			 * Load fish polygon vertices from mFVertexBuffer
			 */
			glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
			/*
			 * Activate blending to correctly render transparencies
			 */
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			/*
			 * Use the texture selected by mTexFrameIdx
			 */
			glBindTexture(GL_TEXTURE_2D, mTexture[mTexFrameIdx]);
			/*
			 * Clamp texture to polygon's edge
			 */
			glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexCoordPointer(2, GL_FLOAT, 0, mFTexBuffer);
			/*
			 * Draw the polygon
			 */
			// type of shape
			// Number of indices
			// How big each index is
			// buffer containing the 3 indices
			glDrawElements(GL10.GL_TRIANGLE_STRIP, VERTS, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
			/*
			 * Resotre saved matrix.
			 */
			glPopMatrix();
		}

		/**
		 * Load the correct texture for the fish based on its speed.
		 * 
		 * @param speed
		 */
		void loadFishTexture(FishSpeed speed) {
			mCurrentSpeed = speed;
			int file;
			if (mIsTextureInited) {
				/*
				 * Avoid memory leak: free existing textures.
				 */
				glDeleteTextures(FISHFRAMES, mTexture, 0);
			}
			mIsTextureInited = true;
			mTexture = new int[FISHFRAMES];
			/*
			 * Allocate textures.
			 */
			glGenTextures(FISHFRAMES, mTexture, 0);
			switch (speed) {
			case SLOW:
				file = R.drawable.fish_slow000;
				break;
			case NORMAL:
				file = R.drawable.fish_normal000;
				break;
			case FAST:
				file = R.drawable.fish_fast000;
				break;
			default:
				file = R.drawable.fish_normal000;
				break;
			}
			/*
			 * Load textures into mTexture.
			 */
			loadTexture(FISHFRAMES, file, mTexture);
		}

		public void tick() {
			/*
			 * Increment X coordinates proportionally to the physical activity
			 * score.
			 */
			xCoord = (xCoord - 0.0025f - (0.015f * mPhysAct / 34));
			if (xCoord < -1.5f) {
				/*
				 * Reset X coordinates (far right, hidden)
				 */
				xCoord = 1.5f;
				/*
				 * Randomize Y coordinates.
				 */
				mYStartCoord = random.nextFloat() * 2.0f - 1.0f;
				/*
				 * Randomize texture index.
				 */
				mTexFrameIdx = random.nextInt(FISHFRAMES);
				/*
				 * Randomize z depth
				 */
				zDepth = 1.0f + random.nextFloat() * 0.85f;
				mUpdownidx = random.nextInt(SIN_TABLE_LEN);
				/*
				 * Signal to main loop that z-order may have been changed.
				 */
				mHasToReorderSprites = true;
			}
			/*
			 * Increment Y coordinate based on start Y coordinates, and
			 * proportionally to physical activity score
			 */
			yCoord = mYStartCoord + SIN_TABLE[mUpdownidx] * (0.02f + 0.2f * mPhysAct / 100f);

			mTexFrameIdx++;
			if (mTexFrameIdx > FISHFRAMES - 1 || mTexFrameIdx < 0) {
				mTexFrameIdx = 0; // start at beginning of loop
			} else {
			}
			mUpdownidx = mUpdownidx > (SIN_TABLE_LEN - 2) ? 0 : mUpdownidx + 1;
		}
	}

	/**
	 * Sprites are 2D polygons that can be ordered in z-order.
	 * 
	 * @author gcardone
	 * 
	 */
	abstract class Sprite implements Comparable<Sprite> {
		public float zDepth;

		@Override
		public int compareTo(Sprite o2) {
			float diff = zDepth - o2.zDepth;
			if (diff < 0) {
				return -1;
			} else if (diff > 0) {
				return 1;
			}
			return 0;
		}

		/**
		 * Draw the current sprite using OpenGL functions.
		 * 
		 * @param gl
		 */
		public abstract void draw(GL10 gl);

		/**
		 * Animate the sprite by changing its appearance/position.
		 */
		public void tick() {

		}
	}

	public BeWellFishRenderer(Context context) {
		// Uncomment the line below to enable debugging of the Live Wallpaper
		// android.os.Debug.waitForDebugger();
		mContext = context;
		mTexPoorBlueFishRL = new int[SCHLFRAMES];
		mTexGoodBlueFishRL = new int[SCHLFRAMES];
		mTexPoorBlueFishLR = new int[SCHLFRAMES];
		mTexGoodBlueFishLR = new int[SCHLFRAMES];
		mLastTick = System.currentTimeMillis();
		mBackground = new Background();
		mFish = new Fish();
		mSchoolOfFish = new ArrayList<BeWellFishRenderer.BlueFish>();
		/*
		 * Objects in the OpenGL scene must be rendered back to front.
		 * mZOrderedObject stores all objects in the correct order.
		 */
		mZOrdererObjects = new ArrayList<BeWellFishRenderer.Sprite>();
		mZOrdererObjects.add(mFish);
		mHasToReorderSprites = false;
		mIsBlueFishTextureInited = false;

		mScrolling = false;
		mXOffset = 0.5f;
		initScoreValues();
	}

	/**
	 * Adds fish to the school of fish.
	 * 
	 * @param n
	 *            Number of fish to add. Must be positive.
	 */
	private void addBlueFish(int n) {
		if (n < 0) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < n; i++) {
			BlueFish bf = new BlueFish();
			mSchoolOfFish.add(bf);
			mZOrdererObjects.add(bf);
		}
	}

	/**
	 * Initializes wellness scores.
	 */
	private void initScoreValues() {
		// TODO: read wellness scores from database/whatever.

		Bundle b = ScoreComputationService.getScores();
		if (b != null) {
			mPhysAct = b.getInt(ScoreComputationService.BEWELL_SCORE_PHYSICAL, 0);
			mPhysActOld = b.getInt(ScoreComputationService.BEWELL_SCORE_PHYSICAL_OLD, 0);
			mSocialAct = b.getInt(ScoreComputationService.BEWELL_SCORE_SOCIAL, 0);
			mSocialActOld = b.getInt(ScoreComputationService.BEWELL_SCORE_SOCIAL_OLD, 0);
			mSleep = b.getInt(ScoreComputationService.BEWELL_SCORE_SLEEP, 0);
			mSleepOld = b.getInt(ScoreComputationService.BEWELL_SCORE_SLEEP_OLD, 0);
		}

		// if no score files
		else {
			mPhysAct = 50;
			mSleep = 75;
			mSocialAct = 70;
			mPhysActOld = 60;
			mSleepOld = 70;
			mSocialActOld = 60;
		}

	}

	/**
	 * Loads school of fish textures. mIsBlueFishTextureInited is used to avoid
	 * memory leaks.
	 */
	void loadBlueFish() {
		if (mIsBlueFishTextureInited) {
			mIsBlueFishTextureInited = true;
			glDeleteTextures(SCHLFRAMES, mTexGoodBlueFishRL, 0);
			glDeleteTextures(SCHLFRAMES, mTexGoodBlueFishLR, 0);
			glDeleteTextures(SCHLFRAMES, mTexPoorBlueFishLR, 0);
			glDeleteTextures(SCHLFRAMES, mTexPoorBlueFishRL, 0);
		}
		loadTexture(SCHLFRAMES, R.drawable.blue_good_rl_000, mTexGoodBlueFishRL);
		loadTexture(SCHLFRAMES, R.drawable.blue_poor_rl_000, mTexPoorBlueFishRL);
		loadTexture(SCHLFRAMES, R.drawable.blue_good_lr_000, mTexGoodBlueFishLR);
		loadTexture(SCHLFRAMES, R.drawable.blue_poor_lr_000, mTexPoorBlueFishLR);
	}

	/**
	 * Utility method to load a set of textures. <strong>Note that this method
	 * can be executed only in a OpenGL thread</strong>.
	 * 
	 * @param numFrames
	 *            Number of frames to load
	 * @param baseFrame
	 *            Identifier of the first texture frame.
	 * @param textures
	 *            Array where to store textures. it must be at least numFrames
	 *            long.
	 */

	private void loadTexture(int numFrames, int baseFrame, int[] textures) {
		int file;
		glGenTextures(numFrames, textures, 0);
		file = baseFrame;
		for (int i = 0; i < numFrames; i++) {
			try {
				InputStream bgis = mContext.getResources().openRawResource(file);
				Bitmap bgbmap = BitmapFactory.decodeStream(bgis);
				bgis.close();
				glBindTexture(GL_TEXTURE_2D, textures[i]);
				GLUtils.texImage2D(GL_TEXTURE_2D, 0, bgbmap, 0);
				bgbmap.recycle();
				file++;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Main loop that draws frames.
	 * 
	 * @param gl
	 *            OpenGL context.
	 */
	@Override
	public void onDrawFrame(GL10 gl) {
		// Disable dithering to improve performances
		glDisable(GL10.GL_DITHER);
		// Clear background
		glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		glMatrixMode(GL10.GL_MODELVIEW);
		glLoadIdentity();
		// Code to implement parallax effect when scrolling.
		// The "eye" position is shifted to the left/right of lookOffset,
		// centered on the y axis and distand 5 on the z axis. It looks at the
		// point at coordinates (lookOffset, 0, 0).
		float lookOffset = mXOffset - 0.50f;
		GLU.gluLookAt(gl, lookOffset, 0, 5, lookOffset, 0f, 0f, 0f, 1.0f, 0.0f);
		// GLU.gluLookAt(gl, 0, 0, 5, 0, 0f, 0f, 0f, 1.0f, 0.0f);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

		if (mPendingSchoolFishResize) {
			/*
			 * Resize the school of fish in the onDrawFrame event to avoid
			 * having concurrency issues on mZOrderedObjects and on
			 * mSchoolOfFish.
			 */
			mPendingSchoolFishResize = false;
			resizeSchoolOfFish(mNewSchoolOfFishSize);
		}

		// Move sprites only if the screen is not scrollin and
		// if we are rendering at most FPS_LIMIT FPS.
		long currTime = System.currentTimeMillis();
		if ((currTime - mLastTick > FRAME_MIN_INTERVAL) && !mScrolling) {
			mBackground.tick();
			for (Sprite s : mZOrdererObjects) {
				s.tick();
			}
			// Optimization: reorder sprites only if a sprite changed its
			// zOrder.
			if (mHasToReorderSprites) {
				mHasToReorderSprites = false;
				Collections.sort(mZOrdererObjects);
			}
			mLastTick = currTime;
		}

		// Draw all sprites
		mBackground.draw(gl);
		for (Sprite s : mZOrdererObjects) {
			s.draw(gl);
		}
	}

	/**
	 * Code to manage screen scrolling. See {@link http://goo.gl/6PJcJ}.
	 * xScrolling is set to true if currently the wallpaper is not centered on a
	 * screen.
	 */
	@Override
	public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset,
			int yPixelOffset) {
		this.mXOffset = xOffset;
		mScrolling = false;
		/* Scrolling effect to simulate parallax disabled: it causes problems
		 * on some Android devices that do not have different screen activity
		 * configurations compared to the official Android style.
		 */
		// if (xOffsetStep == 0f) {
		// mScrolling = false;
		// return;
		// }
		// if ((int) (xOffset * 1000) % (int) (xOffsetStep * 1000) == 0) {
		// mScrolling = false;
		// } else {
		// mScrolling = true;
		// }
	}

	/**
	 * Called when the surface changes size/orientation.
	 */
	@Override
	public void onSurfaceChanged(GL10 gl, int w, int h) {
		glViewport(0, 0, w, h);
		float ratio = (float) w / h;
		glMatrixMode(GL10.GL_PROJECTION);
		glLoadIdentity();
		/*
		 * The rendering volume is a 2x2x2 frustum centered on 0;0;0.
		 */
		glFrustumf(-ratio, ratio, -1, 1, 3, 7);
		mWidth = w;
		mHeight = h;
	}

	/**
	 * Called when the surface is created or recreated.
	 */
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		glDisable(GL10.GL_DITHER);
		glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
		glClearColor(0f, 0f, 0f, 1);
		glShadeModel(GL_SMOOTH);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);
		glEnableClientState(GL_TEXTURE_COORD_ARRAY);

		// mBackground.loadBackgroundTexture(BackgroundMood.NORMAL);
		// mFish.loadFishTexture(FishSpeed.NORMAL);
		loadBlueFish();
		setPhysicalActivityScore(mPhysAct);
		setSleepScore(mSleep);
		setSocialActivityScore(mSocialAct);
	}

	/**
	 * Removes blue fish from the school of fish.
	 * 
	 * @param n
	 *            Number of fish to remove from the school of fish
	 */
	private void removeBlueFish(int n) {
		int m = Math.min(n, mSchoolOfFish.size());
		for (int i = 0; i < m; i++) {
			BlueFish bf = mSchoolOfFish.remove(0);
			mZOrdererObjects.remove(bf);
		}
	}

	private void resizeSchoolOfFish(int n) {
		if (n > mSchoolOfFish.size()) {
			addBlueFish(n - mSchoolOfFish.size());
		} else {
			removeBlueFish(mSchoolOfFish.size() - n);
		}
	}

	/**
	 * Sets the new phyisical activity score.
	 * 
	 * @param value
	 *            The new score. It has to be in the [0..100] range (included).
	 *            Out of range scores are ignored.
	 */
	public void setPhysicalActivityScore(int value) {
		if (value < 0 || value > 100) {
			Log.e(TAG, "New physical activity value out of range: " + value);
		}
		mPhysAct = value;
		FishSpeed newSpeed;
		if (value < 33) {
			newSpeed = FishSpeed.SLOW;
		} else if (value < 66) {
			newSpeed = FishSpeed.NORMAL;
		} else {
			newSpeed = FishSpeed.FAST;
		}
		/*
		 * We can change the fish texture only in the OpenGL thread, thus we set
		 * a flag and wait for the next redraw to update it.
		 */
		mFish.appendTextureUpdate(newSpeed);
	}

	/**
	 * Sets the new sleep score.
	 * 
	 * @param value
	 *            The new score. It has to be in the [0..100] range (included).
	 *            Out of range scores are ignored.
	 */
	public void setSleepScore(int value) {
		if (value < 0 || value > 100) {
			Log.e(TAG, "New mood out of range: " + value);
			return;
		}

		BackgroundMood newMood;
		mSleep = value;
		if (value <= 45) {
			newMood = BackgroundMood.POOR;
		} else if (value <= 80) {
			newMood = BackgroundMood.NORMAL;
		} else {
			newMood = BackgroundMood.GOOD;
		}
		/*
		 * We can change the background texture only in the OpenGL thread, thus
		 * we set a flag and wait for the next redraw to update it.
		 */
		mBackground.appendTextureUpdate(newMood);
	}

	/**
	 * Sets the new social activity score.
	 * 
	 * @param value
	 *            The new score. It has to be in the [0..100] range (included).
	 *            Out of range scores are ignored.
	 */
	public void setSocialActivityScore(int value) {
		if (value < 0) {
			Log.e(TAG, "New social activity out of range: " + value);
			return;
		}
		/*
		 * Fish in the school of fish are accessed only by the main redrawing
		 * method (that runs in the OpenGL thread). We could update it here, but
		 * we would need a synchronizing lock to avoid concurrency issues. Thus
		 * we just set a flag and wait for the onDrawFrame to change the school
		 * of fish size.
		 */
		mSocialAct = value;
		mPendingSchoolFishResize = true;
		mNewSchoolOfFishSize = value / 20 * 3 + (value / 10) % 2;
	}

	/**
	 * / returns true if x, y is around the area of the main fish.
	 */
	public boolean touchTrigger(int x, int y) {
		// convert Android coordinate system to OpenGL matrices
		float xf, yf;
		xf = ((x * 2) / (float) mWidth) - 1.0f;
		yf = (((mHeight - y) * 2) / (float) mHeight) - 1.0f;
		return Math.abs(mFish.xCoord - xf) < 0.6f && Math.abs(mFish.yCoord - yf) < 0.2f;
	}
}
