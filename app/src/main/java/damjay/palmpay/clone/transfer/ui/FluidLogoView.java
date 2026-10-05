package damjay.palmpay.clone.transfer.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.animation.Keyframe;
import android.animation.PropertyValuesHolder;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import damjay.palmpay.clone.R;

/**
 * The PalmPay mark that "fills with liquid": the hollow parts of the logo
 * are covered by the solid hexagon silhouette rising from the bottom (1 s),
 * holding solid for ~300 ms, then draining again (1 s), looping.
 */
public final class FluidLogoView extends AppCompatImageView {
    private static final long FILL_MS = 1000;
    private static final long HOLD_MS = 300;
    private static final long DRAIN_MS = 1000;
    private static final long CYCLE_MS = FILL_MS + HOLD_MS + DRAIN_MS;

    private final android.graphics.drawable.Drawable solid =
            ContextCompat.getDrawable(getContext(), R.drawable.ic_palmpay_hex_solid);
    private final android.graphics.Rect clip = new android.graphics.Rect();
    private float fill;
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (fill <= 0f || solid == null) {
            return;
        }
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        // The square drawable is centred; clip the same square so the fill
        // line tracks the hexagon, not the padded bounds.
        int side = Math.min(width, height);
        int top = (height - side) / 2;
        int left = (width - side) / 2;
        solid.setBounds(left, top, left + side, top + side);
        canvas.save();
        clip.set(left, top + Math.round(side * (1f - fill)), left + side, top + side);
        canvas.clipRect(clip);
        solid.draw(canvas);
        canvas.restore();
    }
}
