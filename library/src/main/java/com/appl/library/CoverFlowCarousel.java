package com.appl.library;

import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;

/**
 * @author Martin Appl
 */
public class CoverFlowCarousel extends Carousel {

    /**
     * Widget size on which was tuning of parameters done. This value is used to scale parameters on when widgets has different size
     */
    private int mTuningWidgetSize = 1280;

    /**
     * Distance from center as fraction of half of widget size where covers start to rotate into center
     * 1 means rotation starts on edge of widget, 0 means only center rotated
     */
    private float mRotationThreshold = 0.3f;

    /**
     * Distance from center as fraction of half of widget size where covers start to zoom in
     * 1 means scaling starts on edge of widget, 0 means only center scaled
     */
    private float mScalingThreshold = 0.3f;

    /**
     * Distance from center as fraction of half of widget size,
     * where covers start enlarge their spacing to allow for smooth passing each other without jumping over each other
     * 1 means edge of widget, 0 means only center
     */
    private float mAdjustPositionThreshold = 0.1f;

    /**
     * By enlarging this value, you can enlarge spacing in center of widget done by position adjustment
     */
    private float mAdjustPositionMultiplier = 0.8f;

    /**
     * Absolute value of rotation angle of cover at edge of widget in degrees
     */
    private float mMaxRotationAngle = 70.0f;

    /**
     * Scale factor of item in center
     */
    private float mMaxScaleFactor = 1.2f;

    /**
     * Radius of circle path which covers follow. Range of screen is -1 to 1, minimal radius is therefore 1
     */
    private float mRadius = 2f;

    /**
     * Size multiplier used to simulate perspective
     */
    private float mPerspectiveMultiplier = 1f;

    /**
     * Size of reflection as a fraction of original image (0-1)
     */
    private float mReflectionHeight = 0.5f;

    /**
     * Starting opacity of reflection. Reflection fades from this value to transparency;
     */
    private int mReflectionOpacity = 0x70;

    /**
     * How long will alignment animation take
     */
    private int mAlignTime = 350;

    private int mCenterItemOffset;

    private int mReverseOrderIndex = -1;

    private final Scroller mAlignScroller = new Scroller(getContext(), new DecelerateInterpolator());

    //reflection
    private final Matrix mReflectionMatrix = new Matrix();
    private final Paint mPaint = new Paint();
    //private final Paint mReflectionPaint = new Paint();
    private final PorterDuffXfermode mXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
    private final Canvas mReflectionCanvas = new Canvas();

    public CoverFlowCarousel(Context context) {
        super(context);
    }

    public CoverFlowCarousel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CoverFlowCarousel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void setTransformation(View v){
        int c = getChildCenter(v);
        v.setRotationY(getRotationAngle(c) - getAngleOnCircle(c));
        v.setTranslationX(getChildAdjustPosition(v));
        float scale = getScaleFactor(c) - getChildCircularPathZOffset(c);
        v.setScaleX(scale);
        v.setScaleY(scale);
    }


    @Override
    public void computeScroll() {
        if (mTouchState == TOUCH_STATE_ALIGN) {
            if (mAlignScroller.computeScrollOffset()) {
                if(mAlignScroller.getFinalX() == mAlignScroller.getCurrX()){
                    mAlignScroller.abortAnimation();
                    mTouchState = TOUCH_STATE_RESTING;
                    return;
                }

                int x = mAlignScroller.getCurrX();
                scrollTo(x, 0);

                postInvalidate();
                return;
            }
            else{
                mTouchState = TOUCH_STATE_RESTING;
                return;
            }
        }

        super.computeScroll();
    }

    @Override
    protected int getPartOfViewCoveredBySibling() {
        return 0;
    }

    @Override
    protected View getViewFromAdapter(int position){
        CoverFrame finalFrame;
        View currView = mCache.getCachedView();
        if (currView instanceof CoverFrame) {
            finalFrame = (CoverFrame)currView;
            View recycled = finalFrame.getChildAt(0);

            View v = mAdapter.getView(position, recycled, this);

            finalFrame.setCover(v);
        }
        else {
            View v = mAdapter.getView(position, null, this);

            finalFrame = new CoverFrame(getContext(), v);
        }

        //to enable drawing cache
        if(android.os.Build.VERSION.SDK_INT >= 11) finalFrame.setLayerType(LAYER_TYPE_HARDWARE, null);
        finalFrame.setDrawingCacheEnabled(true);

        return finalFrame;
    }

    private float getRotationAngle(int childCenter){
        return -mMaxRotationAngle * getClampedRelativePosition(getRelativePosition(childCenter), mRotationThreshold * getWidgetSizeMultiplier());
    }

    private float getAngleOnCircle(int childCenter){
        float x = getRelativePosition(childCenter)/mRadius;
        if(x < -1.0f) x = -1.0f;
        if(x > 1.0f) x = 1.0f;

        return (float) (Math.acos(x)/Math.PI*180.0f - 90.0f);
    }

    private float getScaleFactor(int childCenter){
        return 1 + (mMaxScaleFactor-1) * (1 - Math.abs(getClampedRelativePosition(getRelativePosition(childCenter), mScalingThreshold * getWidgetSizeMultiplier())));
    }

    /**
     * Clamps relative position by threshold, and produces values in range -1 to 1 directly usable for transformation computation
     * @param position value int range -1 to 1
     * @param threshold always positive value of threshold distance from center in range 0-1
     * @return
     */
    private float getClampedRelativePosition(float position, float threshold){
        if(position < 0){
            if(position < -threshold) return -1f;
            else return position/threshold;
        }
        else{
            if(position > threshold) return 1;
            else return position/threshold;
        }
    }

    /**
     * Calculates relative position on screen in range -1 to 1, widgets out of screen can have values ove 1 or -1
     * @param pixexPos Absolute position in pixels including scroll offset
     * @return relative position
     */
    private float getRelativePosition(int pixexPos){
        final int half = getWidth()/2;
        final int centerPos = getScrollX() + half;

        return (pixexPos - centerPos)/((float) half);
    }

    private float getWidgetSizeMultiplier(){
        return ((float)mTuningWidgetSize)/((float)getWidth());
    }

    private float getChildAdjustPosition(View child) {
        final int c = getChildCenter(child);
        final float crp = getClampedRelativePosition(getRelativePosition(c), mAdjustPositionThreshold * getWidgetSizeMultiplier());

        return mChildWidth * mAdjustPositionMultiplier * mSpacing * crp * getSpacingMultiplierOnCircle(c);
    }

    private float getSpacingMultiplierOnCircle(int childCenter){
        float x = getRelativePosition(childCenter)/mRadius;
        return (float) Math.sin(Math.acos(x));
    }

    /**
     * Compute offset following path on circle
     * @param childCenter
     * @return offset from position on unitary circle
     */
    private float getOffsetOnCircle(int childCenter){
        float x = getRelativePosition(childCenter)/mRadius;
        if(x < -1.0f) x = -1.0f;
        if(x > 1.0f) x = 1.0f;

        return (float) (1 - Math.sin(Math.acos(x)));
    }

    private float getChildCircularPathZOffset(int center){
        final float v = getOffsetOnCircle(center);
        return mPerspectiveMultiplier * v;
    }

    /**
     * Adds a view as a child view and takes care of measuring it.
     * Wraps cover in its frame.
     *
     * @param child      The view to add
     * @param layoutMode Either LAYOUT_MODE_LEFT or LAYOUT_MODE_RIGHT
     * @return child which was actually added to container, subclasses can override to introduce frame views
     */
    protected View addAndMeasureChild(final View child, final int layoutMode) {
        if (child.getLayoutParams() == null) child.setLayoutParams(new LayoutParams(mChildWidth,
            mChildHeight));

        final int index = layoutMode == LAYOUT_MODE_TO_BEFORE ? 0 : -1;
        addViewInLayout(child, index, child.getLayoutParams(), true);

        final int pwms = MeasureSpec.makeMeasureSpec(mChildWidth, MeasureSpec.EXACTLY);
        final int phms = MeasureSpec.makeMeasureSpec(mChildHeight, MeasureSpec.EXACTLY);
        measureChild(child, pwms, phms);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            child.setDrawingCacheEnabled(isChildrenCached());
        }
        else {
            child.setDrawingCacheEnabled(isChildrenDrawnWithCacheEnabled());
        }

        return child;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mReverseOrderIndex = -1;

        super.dispatchDraw(canvas);

        //make sure we never stay unaligned after last draw in resting state
        if(mTouchState == TOUCH_STATE_RESTING && mCenterItemOffset != 0) {
            scrollBy(mCenterItemOffset, 0);
            postInvalidate();
        }
    }

    @Override
    protected boolean checkScrollPosition() {
        //Log.d("Carousel", "CheckScrollPosition");
        if(mCenterItemOffset != 0){
            mAlignScroller.startScroll(getScrollX(), 0, mCenterItemOffset, 0, mAlignTime);
            mTouchState = TOUCH_STATE_ALIGN;
            invalidate();
            return true;
        }
        return false;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        setTransformation(child);

        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        final int screenCenter = getWidth() / 2 + getScrollX();
        final int myCenter = getChildCenter(i);
        final int d = myCenter - screenCenter;

        final View v = getChildAt(i);
        final int sz = (int) (mSpacing * v.getWidth()/2f);

        if(mReverseOrderIndex == -1 && (Math.abs(d) < sz || d >= 0)){
            mReverseOrderIndex = i;
            mCenterItemOffset = d;
            return childCount - 1;
        }

        return super.getChildDrawingOrder(childCount, i);
    }

    private Bitmap createReflectionBitmap(Bitmap original){
        final int w = original.getWidth();
        final int h = original.getHeight();
        final int rh = (int) (h * mReflectionHeight);
        final int gradientColor = Color.argb(mReflectionOpacity, 0xff, 0xff, 0xff);

        final Bitmap reflection = Bitmap.createBitmap(original, 0, rh, w, rh, mReflectionMatrix, false);

        final LinearGradient shader = new LinearGradient(0, 0, 0, reflection.getHeight(), gradientColor, 0x00ffffff, Shader.TileMode.CLAMP);
        mPaint.reset();
        mPaint.setShader(shader);
        mPaint.setXfermode(mXfermode);

        mReflectionCanvas.setBitmap(reflection);
        mReflectionCanvas.drawRect(0, 0, reflection.getWidth(), reflection.getHeight(), mPaint);

        return reflection;
    }

    private class CoverFrame extends FrameLayout {
        private Bitmap mReflectionCache;
        private boolean mReflectionCacheInvalid = false;


        public CoverFrame(Context context, View cover) {
            super(context);
            setCover(cover);
        }

        public void setCover(View cover){
            removeAllViews();
            //mReflectionCacheInvalid = true; //todo uncomment after adding support for reflection
            if(cover.getLayoutParams() != null) setLayoutParams(cover.getLayoutParams());

            final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            lp.leftMargin = 1;
            lp.topMargin = 1;
            lp.rightMargin = 1;
            lp.bottomMargin = 1;

            if (cover.getParent()!=null && cover.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) cover.getParent();
                parent.removeView(cover);
            }

            addView(cover, lp);
        }

        /*
        @Override
        protected void dispatchDraw(Canvas canvas) {
            canvas.setDrawFilter(new PaintFlagsDrawFilter(1, Paint.ANTI_ALIAS_FLAG));
            super.dispatchDraw(canvas);
        }
        */

        @Override
        public Bitmap getDrawingCache(boolean autoScale) {
            final Bitmap b = super.getDrawingCache(autoScale);

            if(mReflectionCacheInvalid){
                if (/*(mTouchState != TOUCH_STATE_FLING && mTouchState != TOUCH_STATE_ALIGN) ||*/ mReflectionCache == null){
                    try{
                        mReflectionCache = createReflectionBitmap(b);
                        mReflectionCacheInvalid = false;
                    }
                    catch (NullPointerException e){
                        Log.e(VIEW_LOG_TAG, "Null pointer in createReflectionBitmap. Bitmap b=" + b, e);
                    }
                }
            }
            return b;
        }

        public void recycle(){ //todo add puttocache method and call recycle
            if(mReflectionCache != null){
                mReflectionCache.recycle();
                mReflectionCache = null;
            }
            mReflectionCacheInvalid = true;

            //removeAllViewsInLayout();
        }

    }

    public void scrollToItemPosition(int position) {
        int newItemOffset;

        if (position > getSelection()) {
            newItemOffset =  (mChildWidth / 2) * (position - getSelection());
        }
        else if (position < getSelection()) {
            newItemOffset =  -(mChildWidth / 2) * (getSelection() - position);
        }
        else {
            return;
        }

        mCenterItemOffset = newItemOffset;

        checkScrollPosition();
    }

    public void setMaxScaleFactor(float maxScaleFactor) {
        mMaxScaleFactor = maxScaleFactor;
    }

    public void setRotationThreshold(float rotationThreshold) {
        mRotationThreshold = rotationThreshold;
    }

    public void setMaxRotationAngle(int maxRotationAngle) {
        mMaxRotationAngle = maxRotationAngle;
    }
}
