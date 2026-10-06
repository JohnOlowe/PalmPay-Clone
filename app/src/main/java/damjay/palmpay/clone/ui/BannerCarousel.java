package damjay.palmpay.clone.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import java.util.List;

import damjay.palmpay.clone.R;

/**
 * The home promo rail: pages dwell ~3 s, then a fluid ~2 s scroll to the
 * left pushes the current page out; a finger can drag at any time and the
 * rail snaps one page per gesture. Built on HorizontalScrollView so every
 * page is a real laid-out child - the earlier translation-on-a-LinearLayout
 * version rendered page one only on some devices.
 */
public final class BannerCarousel extends LinearLayout {
    public static final long DWELL_MS = 3000;
    public static final long SCROLL_MS = 2000;
    private static final long SNAP_MS = 260;

    private final PagingScroll scroller = new PagingScroll(getContext());
    private final LinearLayout track = new LinearLayout(getContext());
    private final LinearLayout dots = new LinearLayout(getContext());
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int pageCount;
    private int pageWidth;
    private int currentPage;
    private ValueAnimator animator;

    private final class PagingScroll extends HorizontalScrollView {
        PagingScroll(Context context) {
            super(context);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    stopAuto();
                    cancelAnimator();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    snap();
                    break;
                default:
                    break;
            }
            return super.onTouchEvent(ev);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                stopAuto();
                cancelAnimator();
            }
            return super.onInterceptTouchEvent(ev);
        }

        @Override
        public void fling(int velocityX) {
            // No coasting: release always hands over to snap(), one page max.
        }

        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            super.onScrollChanged(l, t, oldl, oldt);
            if (pageWidth > 0) {
                updateDots((l + pageWidth / 2) / pageWidth);
            }
        }
    }

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
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setOverScrollMode(OVER_SCROLL_NEVER);
        addView(scroller, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(78)));
        scroller.addView(track, new ViewGroup.LayoutParams(
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
        currentPage = 0;
        requestLayout();
        post(() -> {
            scroller.scrollTo(0, 0);
            updateDots(0);
            scheduleAuto();
        });
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

    private void updateDots(int page) {
        if (pageCount == 0) {
            return;
        }
        int active = Math.floorMod(page, pageCount);
        for (int i = 0; i < dots.getChildCount(); i++) {
            dots.getChildAt(i).setBackgroundResource(
                    i == active ? R.drawable.bg_dot_active
                            : R.drawable.bg_dot_inactive);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int usable = width - getPaddingLeft() - getPaddingRight();
        if (usable > 0 && usable != pageWidth) {
            pageWidth = usable;
            for (int i = 0; i < track.getChildCount(); i++) {
                ViewGroup.LayoutParams params =
                        track.getChildAt(i).getLayoutParams();
                params.width = pageWidth;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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
        handler.postDelayed(this::advance, DWELL_MS);
    }

    private void stopAuto() {
        handler.removeCallbacksAndMessages(null);
    }

    private void cancelAnimator() {
        if (animator != null) {
            animator.removeAllUpdateListeners();
            animator.cancel();
            animator = null;
        }
    }

    /** Fluid leftward scroll to the next page, wrapping through the clone. */
    private void advance() {
        if (pageCount == 0 || pageWidth == 0) {
            scheduleAuto();
            return;
        }
        animateScrollTo((currentPage + 1) * pageWidth, SCROLL_MS, () -> {
            if (currentPage + 1 >= pageCount) {
                // Landed on the clone: jump home invisibly.
                scroller.scrollTo(0, 0);
                currentPage = 0;
                updateDots(0);
            } else {
                currentPage += 1;
            }
            scheduleAuto();
        });
    }

    /** One page per gesture, never more. */
    private void snap() {
        if (pageWidth == 0) {
            scheduleAuto();
            return;
        }
        int nearest = Math.round(scroller.getScrollX() / (float) pageWidth);
        nearest = Math.max(currentPage - 1, Math.min(currentPage + 1, nearest));
        nearest = Math.max(0, Math.min(pageCount, nearest));
        final int target = nearest;
        animateScrollTo(target * pageWidth, SNAP_MS, () -> {
            if (target >= pageCount) {
                scroller.scrollTo(0, 0);
                currentPage = 0;
            } else {
                currentPage = target;
            }
            updateDots(currentPage);
            scheduleAuto();
        });
    }

    private void animateScrollTo(final int x, long duration, Runnable onEnd) {
        cancelAnimator();
        animator = ValueAnimator.ofInt(scroller.getScrollX(), x);
        animator.setDuration(duration);
        animator.setInterpolator(new DecelerateInterpolator(1.1f));
        animator.addUpdateListener(animation ->
                scroller.scrollTo((int) animation.getAnimatedValue(), 0));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator a) {
                onEnd.run();
            }
        });
        animator.start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
