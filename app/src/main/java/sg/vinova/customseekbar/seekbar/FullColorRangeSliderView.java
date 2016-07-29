package sg.vinova.customseekbar.seekbar;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;

import java.util.concurrent.TimeUnit;

import sg.vinova.customseekbar.R;

public class FullColorRangeSliderView extends View {

    private static final String TAG = FullColorRangeSliderView.class.getSimpleName();

    private static final long RIPPLE_ANIMATION_DURATION_MS = TimeUnit.MILLISECONDS.toMillis(700);

    private static final int DEFAULT_PAINT_STROKE_WIDTH = 5;

    private static final int DEFAULT_FILLED_COLOR = Color.parseColor("#FFA500");

    private static final int DEFAULT_EMPTY_COLOR = Color.parseColor("#C3C3C3");

    private static final float DEFAULT_BAR_HEIGHT_PERCENT = 0.10f;

    private static final float DEFAULT_SLOT_RADIUS_PERCENT = 0.125f;

    private static final float DEFAULT_SLIDER_RADIUS_PERCENT = 0.25f;

    private static final int DEFAULT_RANGE_COUNT = 5;

    private static final int DEFAULT_HEIGHT_IN_DP = 50;
    private static final boolean DEFAULT_ENABLE_SLIDE = true;

    protected Paint paint;

    protected Paint ripplePaint;

    protected float radius;

    protected float slotRadius;

    private int currentIndex;

    private float currentSlidingX;

    private float currentSlidingY;

    private float selectedSlotX;

    private float selectedSlotY;

    private boolean gotSlot = false;

    private float[] slotPositions;

    private int filledColor = DEFAULT_FILLED_COLOR;

    private int emptyColor = DEFAULT_EMPTY_COLOR;

    private float barHeightPercent = DEFAULT_BAR_HEIGHT_PERCENT;

    private int rangeCount = DEFAULT_RANGE_COUNT;

    private int barHeight;

    private OnSlideListener listener;

    private float rippleRadius = 0.0f;

    private float downX;

    private float downY;

    private Path innerPath = new Path();

    private Path outerPath = new Path();

    private float slotRadiusPercent = DEFAULT_SLOT_RADIUS_PERCENT;

    private float sliderRadiusPercent = DEFAULT_SLIDER_RADIUS_PERCENT;

    private int layoutHeight;

    int[] colors = {Color.YELLOW, Color.GREEN, Color.RED, Color.BLUE, Color.CYAN, Color.DKGRAY, Color.GREEN, Color.RED, Color.BLUE};
    private boolean enableSlide;

    public FullColorRangeSliderView(Context context) {
        this(context, null);
    }

    public FullColorRangeSliderView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public FullColorRangeSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangeSliderView);
            TypedArray sa = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.layout_height});
            try {
                layoutHeight = sa.getLayoutDimension(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT);
                rangeCount = a.getInt(
                        R.styleable.RangeSliderView_rangeCount, DEFAULT_RANGE_COUNT);
                filledColor = a.getColor(
                        R.styleable.RangeSliderView_filledColor, DEFAULT_FILLED_COLOR);
                emptyColor = a.getColor(
                        R.styleable.RangeSliderView_emptyColor, DEFAULT_EMPTY_COLOR);
                barHeightPercent = a.getFloat(
                        R.styleable.RangeSliderView_barHeightPercent, DEFAULT_BAR_HEIGHT_PERCENT);
                barHeightPercent = a.getFloat(
                        R.styleable.RangeSliderView_barHeightPercent, DEFAULT_BAR_HEIGHT_PERCENT);
                slotRadiusPercent = a.getFloat(
                        R.styleable.RangeSliderView_slotRadiusPercent, DEFAULT_SLOT_RADIUS_PERCENT);
                sliderRadiusPercent = a.getFloat(
                        R.styleable.RangeSliderView_sliderRadiusPercent, DEFAULT_SLIDER_RADIUS_PERCENT);
                enableSlide = a.getBoolean(R.styleable.RangeSliderView_setEnableSlide, DEFAULT_ENABLE_SLIDE);
            } finally {
                a.recycle();
                sa.recycle();
            }
        }

        setBarHeightPercent(barHeightPercent);
        setRangeCount(rangeCount);
        setSlotRadiusPercent(slotRadiusPercent);
        setSliderRadiusPercent(sliderRadiusPercent);

        slotPositions = new float[rangeCount];
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(DEFAULT_PAINT_STROKE_WIDTH);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ripplePaint.setStrokeWidth(2.0f);
        ripplePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);

                // Update radius after we got new height
                updateRadius(getHeight());

                // Compute drawing position again
                preComputeDrawingPosition();

                // Ready to draw now
                return true;
            }
        });
        currentIndex = 0;
    }

    private void updateRadius(int height) {
        barHeight = (int) (height * barHeightPercent);
        radius = height * sliderRadiusPercent;
        slotRadius = height * slotRadiusPercent;
    }

    public int getRangeCount() {
        return rangeCount;
    }

    public void setRangeCount(int rangeCount) {
        if (rangeCount < 2) {
            throw new IllegalArgumentException("rangeCount must be >= 2");
        }
        this.rangeCount = rangeCount;
    }

    public float getBarHeightPercent() {
        return barHeightPercent;
    }

    public void setBarHeightPercent(float percent) {
        if (percent <= 0.0 || percent > 1.0) {
            throw new IllegalArgumentException("Bar height percent must be in (0, 1]");
        }
        this.barHeightPercent = percent;
    }

    public float getSlotRadiusPercent() {
        return slotRadiusPercent;
    }

    public void setSlotRadiusPercent(float percent) {
        if (percent <= 0.0 || percent > 1.0) {
            throw new IllegalArgumentException("Slot radius percent must be in (0, 1]");
        }
        this.slotRadiusPercent = percent;
    }

    public float getSliderRadiusPercent() {
        return sliderRadiusPercent;
    }

    public void setSliderRadiusPercent(float percent) {
        if (percent <= 0.0 || percent > 1.0) {
            throw new IllegalArgumentException("Slider radius percent must be in (0, 1]");
        }
        this.sliderRadiusPercent = percent;
    }

    @AnimateMethod
    public void setRadius(final float radius) {
        rippleRadius = radius;
        if (rippleRadius > 0) {
            RadialGradient radialGradient = new RadialGradient(
                    downX,
                    downY,
                    rippleRadius * 3,
                    Color.TRANSPARENT,
                    Color.BLACK,
                    Shader.TileMode.MIRROR
            );
            ripplePaint.setShader(radialGradient);
        }
        invalidate();
    }

    public void setOnSlideListener(OnSlideListener listener) {
        this.listener = listener;
    }

    /**
     * Perform all the calculation before drawing, should only run once
     */
    private void preComputeDrawingPosition() {
        int w = getWidthWithPadding();
        int h = getHeightWithPadding();

        /** Space between each slot */
        int spacing = w / rangeCount;

        /** Center vertical */
        int y = getPaddingTop() + h / 2;
        currentSlidingY = y;
        selectedSlotY = y;
        /**
         * Try to center it, so start by half
         * <pre>
         *
         *  Example for 4 slots
         *
         *  ____o____|____o____|____o____|____o____
         *  --space--
         *
         * </pre>
         */
        int x = getPaddingLeft() + (spacing / 2);

        /** Store the position of each slot index */
        for (int i = 0; i < rangeCount; ++i) {
            slotPositions[i] = x;
            if (i == currentIndex) {
                currentSlidingX = x;
                selectedSlotX = x;
            }
            x += spacing;
        }
    }

    public void setInitialIndex(int index) {
        if (index < 0 || index >= rangeCount) {
            throw new IllegalArgumentException("Attempted to set index=" + index + " out of range [0," + rangeCount + "]");
        }
        currentIndex = index;
        currentSlidingX = selectedSlotX = slotPositions[currentIndex];
        invalidate();
    }

    public int getFilledColor() {
        return filledColor;
    }

    public void setFilledColor(int filledColor) {
        this.filledColor = filledColor;
        invalidate();
    }

    public int getEmptyColor() {
        return emptyColor;
    }

    public void setEmptyColor(int emptyColor) {
        this.emptyColor = emptyColor;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    /**
     * Measures height according to the passed measure spec
     *
     * @param measureSpec int measure spec to use
     * @return int pixel size
     */
    private int measureHeight(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int result;
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            final int height;
            if (layoutHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
                height = dpToPx(getContext(), DEFAULT_HEIGHT_IN_DP);
            } else if (layoutHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                height = getMeasuredHeight();
            } else {
                height = layoutHeight;
            }
            result = height + getPaddingTop() + getPaddingBottom() + (2 * DEFAULT_PAINT_STROKE_WIDTH);
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    /**
     * Measures width according to the passed measure spec
     *
     * @param measureSpec int measure spec to use
     * @return int pixel size
     */
    private int measureWidth(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int result;
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = specSize + getPaddingLeft() + getPaddingRight() + (2 * DEFAULT_PAINT_STROKE_WIDTH) + (int) (2 * radius);
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private void updateCurrentIndex() {
        float min = Float.MAX_VALUE;
        int j = 0;
        /** Find the closest to x */
        for (int i = 0; i < rangeCount; ++i) {
            float dx = Math.abs(currentSlidingX - slotPositions[i]);
            if (dx < min) {
                min = dx;
                j = i;
            }
        }
        /** This is current index of slider */
        if (j != currentIndex) {
            if (listener != null) {
                listener.onSlide(j);
            }
        }
        currentIndex = j;
        /** Correct position */
        currentSlidingX = slotPositions[j];
        selectedSlotX = currentSlidingX;
        downX = currentSlidingX;
        downY = currentSlidingY;
        animateRipple();
        invalidate();
    }

    private void animateRipple() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "radius", 0, radius);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.setDuration(RIPPLE_ANIMATION_DURATION_MS);
        animator.start();
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                rippleRadius = 0;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!enableSlide)
            return false;
        float y = event.getY();
        float x = event.getX();
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                gotSlot = isInSelectedSlot(x, y);
                downX = x;
                downY = y;
                if (x >= slotPositions[0] && x <= slotPositions[rangeCount - 1]) {
                    currentSlidingX = x;
                    currentSlidingY = y;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (gotSlot) {
                    if (x >= slotPositions[0] && x <= slotPositions[rangeCount - 1]) {
                        currentSlidingX = x;
                        currentSlidingY = y;
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (gotSlot) ;
            {
                gotSlot = false;
                currentSlidingX = x;
                currentSlidingY = y;
                updateCurrentIndex();
            }
            break;
        }
        return true;
    }

    private boolean isInSelectedSlot(float x, float y) {
        return
                selectedSlotX - radius <= x && x <= selectedSlotX + radius &&
                        selectedSlotY - radius <= y && y <= selectedSlotY + radius;
    }

    RectF fillRect = new RectF();

    private void drawEmptySlots(Canvas canvas) {

        int h = getHeightWithPadding();
        int y = getPaddingTop() + (h >> 1);
        for (int i = 0; i < rangeCount; ++i) {
            float left = slotPositions[i] - (radius);
            float right = slotPositions[i] + (radius);
            fillRect.left = left;
            fillRect.top = y - (y / 2);
            fillRect.right = right;
            fillRect.bottom = y + (y / 2);
            paint.setColor(emptyColor);
            canvas.drawRoundRect(fillRect, (float) (radius / 1.5), (float) (radius / 1.5), paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(slotPositions[i], y, radius / 2, paint);
        }
    }

    public int getHeightWithPadding() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    public int getWidthWithPadding() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    RectF emptyRect = new RectF();

    private void drawFilledSlots(Canvas canvas) {
        int h = getHeightWithPadding();
        int y = getPaddingTop() + (h >> 1);
        for (int i = 0; i < rangeCount; ++i) {
            if (slotPositions[i] <= currentSlidingX) {
                paint.setColor(colors[i]);
                float left = slotPositions[i] - (radius);
                float right = slotPositions[i] + (radius);
                emptyRect.left = left;
                emptyRect.top = y - (y / 2);
                emptyRect.right = right;
                emptyRect.bottom = y + (y / 2);

                canvas.drawRoundRect(emptyRect, (float) (radius / 1.5), (float) (radius / 1.5), paint);
            }
        }
    }

    private void drawBar(Canvas canvas, int from, int to, int color) {
        paint.setColor(color);
        int h = getHeightWithPadding();
        int half = (barHeight >> 1);
        int half1 = half / 5;
        int y = getPaddingTop() + (h >> 1);
        canvas.drawRect(from, y - half, to, y + half, paint);
        for (int i = 1; i < slotPositions.length; i++) {
            paint.setColor(colors[i - 1]);
            canvas.drawRect(slotPositions[i - 1], y - half1, slotPositions[i], y + half1, paint);
        }
    }


    private void drawFillBar(Canvas canvas, int from, int to, int color) {
        paint.setColor(filledColor);
        int h = getHeightWithPadding();
        int half = (barHeight >> 1) / 4;
        int y = getPaddingTop() + (h >> 1);
        canvas.drawRect(from, y - half, to, y + half, paint);
    }

    private void drawRippleEffect(Canvas canvas) {
        if (rippleRadius != 0) {
            canvas.save();
            ripplePaint.setColor(Color.GRAY);
            outerPath.reset();
            outerPath.addCircle(downX, downY, rippleRadius, Path.Direction.CW);
            canvas.clipPath(outerPath);
            innerPath.reset();
            innerPath.addCircle(downX, downY, rippleRadius / 3, Path.Direction.CW);
            canvas.clipPath(innerPath, Region.Op.DIFFERENCE);
            canvas.drawCircle(downX, downY, rippleRadius, ripplePaint);
            canvas.restore();
        }
    }

    RectF rectF = new RectF();

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidthWithPadding();
        int h = getHeightWithPadding();
        int spacing = w / rangeCount;
        int border = (spacing >> 1);
        int x0 = getPaddingLeft() + border;
        int y0 = getPaddingTop() + (h >> 1);


        /** Draw empty bar */
        drawBar(canvas, (int) slotPositions[0], (int) slotPositions[rangeCount - 1], emptyColor);
        drawEmptySlots(canvas);

        /** Draw filled bar */
        //drawFillBar(canvas, x0, (int) currentSlidingX, filledColor);

        drawFilledSlots(canvas);

        /** Draw the selected range circle */
        if (getNearliestPosition(currentSlidingX) == 0)
            paint.setColor(emptyColor);
        else
            paint.setColor(colors[getNearliestPosition(currentSlidingX)]);
        float left = currentSlidingX - (radius);
        float right = currentSlidingX + (radius);
        rectF.left = left;
        rectF.top = y0 - (y0 / 2);
        rectF.right = right;
        rectF.bottom = y0 + (y0 / 2);
        canvas.drawRoundRect(rectF, (float) (radius / 1.5), (float) (radius / 1.5), paint);

        paint.setColor(Color.WHITE);
        canvas.drawCircle(currentSlidingX, y0, radius / 2, paint);
        drawRippleEffect(canvas);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.saveIndex = this.currentIndex;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.currentIndex = ss.saveIndex;
    }

    static class SavedState extends BaseSavedState {
        int saveIndex;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.saveIndex = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.saveIndex);
        }

        //required field that makes Parcelables from a Parcel
        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    /**
     * Helper method to convert pixel to dp
     *
     * @param context
     * @param px
     * @return
     */
    static int pxToDp(final Context context, final float px) {
        return (int) (px / context.getResources().getDisplayMetrics().density);
    }

    /**
     * Helper method to convert dp to pixel
     *
     * @param context
     * @param dp
     * @return
     */
    static int dpToPx(final Context context, final float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    /**
     * Interface to keep track sliding position
     */
    public interface OnSlideListener {

        /**
         * Notify when slider change to new index position
         *
         * @param index The index value of range count [0, rangeCount - 1]
         */
        void onSlide(int index);
    }

    /**
     * get nearliest slot position by coordinate x
     *
     * @param x coordinate x
     * @return
     */
    private int getNearliestPosition(float x) {
        float[] values = new float[slotPositions.length];
        for (int i = 0; i < slotPositions.length; i++) {
            values[i] = Math.abs(x - slotPositions[i]);
        }
        int positionMin = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] < values[positionMin])
                positionMin = i;
        }
        return positionMin;
    }

    public boolean isEnableSlide() {
        return enableSlide;
    }

    public void setEnableSlide(boolean enableSlide) {
        this.enableSlide = enableSlide;
    }
}