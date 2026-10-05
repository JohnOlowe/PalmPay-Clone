package damjay.palmpay.clone.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.List;

import damjay.palmpay.clone.R;

/**
 * The home promo rail: pages sit still for ~3 s, then a fluid ~2 s scroll to
 * the left pushes the current page out and reveals the next one, exactly as
 * if a finger swiped right-to-left. The finger can also drag at any time and
 * the rail follows, snapping one page per gesture.
 *
 * Implemented without a ViewPager so the dwell/scroll rhythm is ours: a
 * horizontal track translated inside a clipping viewport, plus the dot row.
 * The first page is cloned at the tail of the track so the wrap from last to
 * first is still a leftward scroll, after which the track jumps home
 * invisibly.
 */
public final class BannerCarousel extends LinearLayout {
    public static final long DWELL_MS = 3000;
    public static final long SCROLL_MS = 2000;
    private static final long SNAP_MS = 260;

    private final FrameLayout viewport = new FrameLayout(getContext());
    private final LinearLayout track = new LinearLayout(getContext());
    private final LinearLayout dots = new LinearLayout(getContext());
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int touchSlop =
            ViewConfiguration.get(getContext()).getScaledTouchSlop();

    private int pageCount;
    private int position;
    private int pageWidth;
    private ValueAnimator animator;
    private VelocityTracker velocityTracker;
    private float downX;
    private float downY;
    private float startOffset;
    private boolean dragging;

    public BannerCarousel(Context context) {
        super(context);
        init();
    }

    public BannerCarousel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        track.setOrientation(LinearLayout.HORIZONTAL);
        viewport.setClipChildren(true);
        addView(viewport, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(78)));
        viewport.addView(track, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LayoutParams dotsParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(12));
        dotsParams.topMargin = dp(6);
        dotsParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        addView(dots, dotsParams);
    }

    /** Builds the pages (plus the wrap-around clone) and the dots. */
    public void setSlides(List<BannerSlideView.Slide> slides) {
        track.removeAllViews();
        dots.removeAllViews();
        pageCount = slides.size();
        for (int i = 0; i < slides.size(); i++) {
            track.addView(newBannerSlideView(slides.get(i)));
            addDot(i);
        }
        track.addView(newBannerSlideView(slides.get(0)));
        position = 0;
        requestLayout();
        applyOffset(offsetFor(position));
        updateDots();
    }

    private BannerSlideView newBannerSlideView(BannerSlideView.Slide slide) {
        BannerSlideView view = new BannerSlideView(getContext());
        view.bind(slide);
        return view;
    }

    private void addDot(int index) {
        View dot = new View(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dp(10), dp(5));
        if (index > 0) {
            params.setMarginStart(dp(4));
        }
        dot.setBackgroundResource(R.drawable.bg_dot_inactive);
        dots.addView(dot, params);
    }

    private void updateDots() {
        int active = position % pageCount;
        for (int i = 0; i < dots.getChildCount(); i++) {
            dots.getChildAt(i).setBackgroundResource(
                    i == active ? R.drawable.bg_dot_active
                            : R.drawable.bg_dot_inactive);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        pageWidth = w - getPaddingLeft() - getPaddingRight();
        if (pageWidth <= 0) {
            return;
        }
        for (int i = 0; i < track.getChildCount(); i++) {
            ViewGroup.LayoutParams params = track.getChildAt(i).getLayoutParams();
            params.width = pageWidth;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            track.getChildAt(i).setLayoutParams(params);
        }
        applyOffset(offsetFor(position));
    }

    private float offsetFor(int pos) {
        return -pos * (float) pageWidth;
    }

    private void applyOffset(float offset) {
        track.setTranslationX(offset);
    }

    // ------------------------------------------------------------ autoplay

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        scheduleAuto();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAuto();
        cancelAnimator();
        super.onDetachedFromWindow();
    }

    private void scheduleAuto() {
        stopAuto();
        handler.postDelayed(this::advanceAuto, DWELL_MS);
    }

    private void stopAuto() {
        handler.removeCallbacksAndMessages(null);
    }

    private void advanceAuto() {
        if (pageCount == 0 || pageWidth == 0) {
            scheduleAuto();
            return;
        }
        animateTo(position + 1, SCROLL_MS);
    }

    private void cancelAnimator() {
        if (animator != null) {
            // A cancelled scroll must not run the settle callback, or a
            // drag started mid-scroll would snap against the wrong page.
            animator.removeAllListeners();
            animator.cancel();
            animator = null;
        }
    }

    /** Fluid leftward scroll to the target page, then wrap or settle. */
    private void animateTo(final int target, long duration) {
        cancelAnimator();
        float from = track.getTranslationX();
        float to = offsetFor(target);
        animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(duration);
        animator.setInterpolator(new DecelerateInterpolator(1.1f));
        animator.addUpdateListener(animation ->
                applyOffset((float) animation.getAnimatedValue()));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator a) {
                settleAt(target);
                scheduleAuto();
            }
        });
        animator.start();
        position = target;
        updateDots();
    }

    /** After landing on the clone page, jump to the real first page. */
    private void settleAt(int target) {
        if (target >= pageCount) {
            position = 0;
            applyOffset(0);
            updateDots();
        } else {
            position = target;
        }
    }

    // -------------------------------------------------------- manual drag

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (pageCount == 0) {
            return false;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                stopAuto();
                cancelAnimator();
                downX = ev.getX();
                downY = ev.getY();
                startOffset = track.getTranslationX();
                dragging = false;
                return false;
            case MotionEvent.ACTION_MOVE: {
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    dragging = true;
                    return true;
                }
                return false;
            }
            default:
                scheduleAuto();
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (pageCount == 0) {
            return super.onTouchEvent(ev);
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                stopAuto();
                cancelAnimator();
                downX = ev.getX();
                downY = ev.getY();
                startOffset = track.getTranslationX();
                dragging = true;
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = ev.getX() - downX;
                float raw = startOffset + dx;
                applyOffset(rubber(raw));
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                velocityTracker.computeCurrentVelocity(1000);
                float vx = velocityTracker.getXVelocity();
                velocityTracker.recycle();
                velocityTracker = null;
                float current = track.getTranslationX();
                float home = offsetFor(position);
                float delta = current - home;
                int target = position;
                if (delta < -pageWidth * 0.25f || vx < -600f) {
                    target = Math.min(position + 1, pageCount);
                } else if (delta > pageWidth * 0.25f || vx > 600f) {
                    target = Math.max(position - 1, 0);
                }
                // One page per gesture, never more.
                animateTo(target, SNAP_MS);
                dragging = false;
                return true;
            }
            default:
                return super.onTouchEvent(ev);
        }
    }

    /** Gentle resistance at the two ends of the rail. */
    private float rubber(float raw) {
        float min = offsetFor(pageCount);
        if (raw > 0) {
            return raw / 2.5f;
        }
        if (raw < min) {
            return min + (raw - min) / 2.5f;
        }
        return raw;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
