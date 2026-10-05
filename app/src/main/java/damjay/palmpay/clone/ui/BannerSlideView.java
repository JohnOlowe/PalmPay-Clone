package damjay.palmpay.clone.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.BannerSlideItemBinding;

/** One page of the promo rail: logo disc, title, subtitle, action pill. */
public final class BannerSlideView extends LinearLayout {
    public static final class Slide {
        public final int logoRes;
        public final int discColorRes;
        public final int titleRes;
        public final int subtitleRes;
        public final int actionRes;

        public Slide(int logoRes, int discColorRes, int titleRes,
                     int subtitleRes, int actionRes) {
            this.logoRes = logoRes;
            this.discColorRes = discColorRes;
            this.titleRes = titleRes;
            this.subtitleRes = subtitleRes;
            this.actionRes = actionRes;
        }
    }

    public BannerSlideView(Context context) {
        super(context);
    }

    public void bind(final Slide slide) {
        removeAllViews();
        BannerSlideItemBinding item = BannerSlideItemBinding.inflate(
                LayoutInflater.from(getContext()), this, true);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(ContextCompat.getColor(getContext(), slide.discColorRes));
        item.slideLogoCircle.setBackground(disc);
        item.slideLogo.setImageResource(slide.logoRes);
        item.slideTitle.setText(slide.titleRes);
        item.slideSubtitle.setText(slide.subtitleRes);
        item.slideAction.setText(slide.actionRes);
        item.slideAction.setOnClickListener(view ->
                Toast.makeText(getContext(),
                        getContext().getString(slide.actionRes) + " selected",
                        Toast.LENGTH_SHORT).show());
    }
}
