package com.gjiazhe.multichoicescirclebutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by gjz on 08/12/2016.
 */

public class MultiChoicesCircleButton extends View {
    private boolean isDragged = false;
    private float mCollapseRadius;
    private float mExpandRadius;
    private float mCircleCentreX;
    private float mCircleCentreY;

    private float mCurrentExpandProgress = 0f;
    private float mFromExpandProgress;
    private Animation expandAnimation;
    private Animation collapseAnimation;
    private int mDuration;

    private String mText;
    private float mTextSize;
    private int mTextColor;
    private int mButtonColor;

    private Paint mPaint;
    private Camera mCamera = new Camera();
    private Matrix mMatrix = new Matrix();

    private DisplayMetrics mDisplayMetrics;

    public MultiChoicesCircleButton(Context context) {
        this(context, null);
    }

    public MultiChoicesCircleButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiChoicesCircleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDisplayMetrics = context.getResources().getDisplayMetrics();

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MultiChoicesCircleButton);
        mCollapseRadius = typedArray.getDimension(R.styleable.MultiChoicesCircleButton_mccb_collapseRadius, dp2px(40));
        mExpandRadius = typedArray.getDimension(R.styleable.MultiChoicesCircleButton_mccb_expandRadius, dp2px(120));
        mText = typedArray.getString(R.styleable.MultiChoicesCircleButton_mccb_text);
        mTextSize = typedArray.getDimension(R.styleable.MultiChoicesCircleButton_mccb_textSize, sp2px(30));
        mTextColor = typedArray.getColor(R.styleable.MultiChoicesCircleButton_mccb_textColor, Color.GRAY);
        mButtonColor = typedArray.getColor(R.styleable.MultiChoicesCircleButton_mccb_buttonColor, Color.RED);
        mDuration = typedArray.getInt(R.styleable.MultiChoicesCircleButton_mccb_duration, 200);
        typedArray.recycle();

        initPaint();
        initAnimation();
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void initAnimation() {
        expandAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                mCurrentExpandProgress = mFromExpandProgress + (1 - mFromExpandProgress) * interpolatedTime;
                if (mCurrentExpandProgress > 1f) {
                    mCurrentExpandProgress = 1f;
                }
                invalidate();
            }
        };

        collapseAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                mCurrentExpandProgress = mFromExpandProgress  * (1 - interpolatedTime);
                if (mCurrentExpandProgress < 0f) {
                    mCurrentExpandProgress = 0f;
                }
                invalidate();
            }
        };
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int viewWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int viewHeight = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        mCircleCentreX = viewWidth / 2;
        mCircleCentreY = viewHeight;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float eventY = event.getY();
        float eventX = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (actionDownInCircle(eventX, eventY)) {
                    clearAnimation();
                    mFromExpandProgress = mCurrentExpandProgress;
                    startExpandAnimation();
                    return true;
                } else {
                    return false;
                }

            case MotionEvent.ACTION_MOVE:
                isDragged = true;
                rotate(eventX, eventY);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragged = false;
                clearAnimation();
                mFromExpandProgress = mCurrentExpandProgress;
                startCollapseAnimation();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean actionDownInCircle(float x, float y) {
        final float currentRadius = (mExpandRadius - mCollapseRadius) * mCurrentExpandProgress + mCollapseRadius;
        double distance = Math.pow(x - mCircleCentreX, 2) + Math.pow(y - mCircleCentreY, 2);
        distance = Math.sqrt(distance);
        return distance <= currentRadius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isDragged) {
            canvas.concat(mMatrix);
        }

        // Draw circle
        mPaint.setColor(mButtonColor);
        final float radius = (mExpandRadius - mCollapseRadius) * mCurrentExpandProgress + mCollapseRadius;
        canvas.drawCircle(mCircleCentreX, mCircleCentreY, radius, mPaint);

        // Draw text
        if (mText != null && mText.length() != 0) {
            mPaint.setTextSize(mTextSize * mCurrentExpandProgress);
            mPaint.setColor(mTextColor);
            Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
            final float textHeight = fontMetrics.bottom - fontMetrics.top;
            final float baseLineY = mCircleCentreY - radius - textHeight / 2
                    - (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.ascent;
            canvas.drawText(mText, mCircleCentreX, baseLineY, mPaint);
        }
    }

    private void rotate(float eventX, float eventY) {
        final int width = getWidth() - getPaddingLeft() - getPaddingRight();
        final int height = getHeight() - getPaddingTop() - getPaddingBottom();
        final int size = Math.max(width, height);

        final float offsetY = mCircleCentreY - eventY;
        final float offsetX = mCircleCentreX - eventX;
        final float rotateX = offsetY / size * 45;
        final float rotateY = -offsetX / size * 45;
        mCamera.save();
        mCamera.rotateX(rotateX);
        mCamera.rotateY(rotateY);
        mCamera.getMatrix(mMatrix);
        mCamera.restore();
        mMatrix.preTranslate(-mCircleCentreX, -mCircleCentreY);
        mMatrix.postTranslate(mCircleCentreX, mCircleCentreY);
        invalidate();
    }

    private void startExpandAnimation() {
        expandAnimation.setDuration(mDuration);
        startAnimation(expandAnimation);
    }

    private void startCollapseAnimation() {
        collapseAnimation.setDuration(mDuration);
        startAnimation(collapseAnimation);
    }

    private float dp2px(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, this.mDisplayMetrics);
    }

    private float sp2px(int sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, this.mDisplayMetrics);
    }
}
