package damjay.palmpay.clone.transfer.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.animation.Keyframe;
import android.animation.PropertyValuesHolder;

import androidx.appcompat.widget.AppCompatImageView;

import damjay.palmpay.clone.R;

/**
 * The PalmPay mark that "fills with liquid": the white slashes and diamond
 * are hoses - purple water pours in from BOTH leaked outer ends at once,
 * the two fronts run along the slashes and meet at the centre diamond, and
 * at full fill the mark is solid; then the water drains back out the way it
 * came (1 s in, 300 ms hold, 1 s out).
 *
 * Implemented as two 45-degree wipes clipped to the white channels, so the
 * fill follows the hoses instead of blooming as a circle.
 */
public final class FluidLogoView extends AppCompatImageView {
    private static final long FILL_MS = 1000;
    private static final long HOLD_MS = 300;
    private static final long DRAIN_MS = 1000;
    private static final long CYCLE_MS = FILL_MS + HOLD_MS + DRAIN_MS;

    /** White channels in the 48-unit viewport, leaking past the hexagon. */
    private static final float[][] LEFT_SLASH = {
            {1f, 29.5f}, {4f, 32.5f}, {20f, 16.5f}, {17f, 13.5f}};
    private static final float[][] RIGHT_SLASH = {
            {47f, 18.5f}, {44f, 15.5f}, {28f, 31.5f}, {31f, 34.5f}};
    private static final float[][] DIAMOND = {
            {24f, 21.4f}, {26.6f, 24f}, {24f, 26.6f}, {21.4f, 24f}};

    private final Paint water = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path channels = new Path();
    private float fill;
    private int builtFor;
    private ValueAnimator animator;

    public FluidLogoView(Context context) {
        super(context);
        init();
    }

    public FluidLogoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setImageResource(R.drawable.ic_palmpay_mark);
        water.setColor(0xFF8800F8);
        water.setStyle(Paint.Style.FILL);
        float fillStart = FILL_MS / (float) CYCLE_MS;
        float holdEnd = (FILL_MS + HOLD_MS) / (float) CYCLE_MS;
        PropertyValuesHolder values = PropertyValuesHolder.ofKeyframe(
                "fill",
                Keyframe.ofFloat(0f, 0f),
                Keyframe.ofFloat(fillStart, 1f),
                Keyframe.ofFloat(holdEnd, 1f),
                Keyframe.ofFloat(1f, 0f));
        animator = ValueAnimator.ofPropertyValuesHolder(values);
        animator.setDuration(CYCLE_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            fill = (float) animation.getAnimatedValue("fill");
            invalidate();
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        animator.cancel();
        super.onDetachedFromWindow();
    }

    private void buildChannels(int side, int left, int top) {
        channels.reset();
        addQuad(side, left, top, LEFT_SLASH);
        addQuad(side, left, top, RIGHT_SLASH);
        addQuad(side, left, top, DIAMOND);
        channels.close();
    }

    private void addQuad(int side, int left, int top, float[][] pts) {
        float s = side / 48f;
        channels.moveTo(left + pts[0][0] * s, top + pts[0][1] * s);
        for (int i = 1; i < pts.length; i++) {
            channels.lineTo(left + pts[i][0] * s, top + pts[i][1] * s);
        }
        channels.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (fill <= 0f) {
            return;
        }
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        int side = Math.min(width, height);
        int top = (height - side) / 2;
        int left = (width - side) / 2;
        if (builtFor != side) {
            buildChannels(side, left, top);
            builtFor = side;
        }

        float cx = left + side / 2f;
        float cy = top + side / 2f;
        // The hoses run on the bottom-left/top-right diagonal; after a -45
        // degree turn that axis is horizontal, so a growing rect becomes a
        // front of water travelling up the left slash, through the diamond
        // and out of the right slash.
        float halfSpan = 21f * (side / 48f);
        canvas.save();
        canvas.clipPath(channels);
        canvas.rotate(-45f, cx, cy);
        // Water enters from both leaked outer ends at once: the left front
        // runs up the left slash, the right front down the right slash, and
        // they meet (and fill the diamond) at the centre.
        float reach = halfSpan * fill;
        canvas.drawRect(cx - halfSpan, cy - side, cx - halfSpan + reach,
                cy + side, water);
        canvas.drawRect(cx + halfSpan - reach, cy - side, cx + halfSpan,
                cy + side, water);
        canvas.restore();
    }
}
