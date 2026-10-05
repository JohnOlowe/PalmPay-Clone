package damjay.palmpay.clone.ui;

import java.util.Arrays;

import damjay.palmpay.clone.R;

/**
 * The five promos the official rail cycles through, in order. Shared by the
 * home page and the Transfer-to-PalmPay page so both rails stay identical.
 */
public final class BannerSlides {
    private BannerSlides() {
        // No instances.
    }

    public static void populate(BannerCarousel carousel) {
        carousel.setSlides(Arrays.asList(
                new BannerSlideView.Slide(R.drawable.ic_coins,
                        R.color.banner_disc_green,
                        R.string.banner_cashback_title,
                        R.string.banner_cashback_sub,
                        R.string.banner_go),
                new BannerSlideView.Slide(R.drawable.ic_data,
                        R.color.banner_disc_blue,
                        R.string.banner_data_title,
                        R.string.banner_data_sub,
                        R.string.banner_claim),
                new BannerSlideView.Slide(R.drawable.ic_check_circle,
                        R.color.banner_disc_purple,
                        R.string.banner_verify_title,
                        R.string.banner_verify_sub,
                        R.string.banner_claim),
                new BannerSlideView.Slide(R.drawable.ic_refer_earn,
                        R.color.banner_disc_orange,
                        R.string.banner_share_title,
                        R.string.banner_share_sub,
                        R.string.banner_claim),
                new BannerSlideView.Slide(R.drawable.ic_loan,
                        R.color.banner_disc_purple,
                        R.string.banner_loan_title,
                        R.string.banner_loan_sub,
                        R.string.banner_borrow)));
    }
}
