package damjay.palmpay.clone;

import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * Diagnostic: inflates the real MainActivity and reports what the bottom
 * navigation renders (labels, sizes, colors) so theme/density regressions
 * are caught by CI instead of by eye.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BottomNavigationLabelTest {

    @Test
    public void bottomNavigationShowsLabels() {
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView nav = activity.findViewById(R.id.bottom_navigation);
                StringBuilder dump = new StringBuilder("NAV DUMP height=")
                        .append(nav.getHeight());
                dumpViews(nav, dump, 0);
                System.out.println(dump);

                String[] labels = findLabelTexts(nav);
                System.out.println("LABELS=" + String.join(",", labels));
                assertTrue("Expected 5 visible labels but got: "
                        + String.join(",", labels) + " :: " + dump,
                        labels.length >= 5);
            });
        }
    }

    private static String[] findLabelTexts(BottomNavigationView nav) {
        java.util.List<String> texts = new java.util.ArrayList<>();
        collectLabels(nav, texts);
        return texts.toArray(new String[0]);
    }

    private static void collectLabels(View view, java.util.List<String> texts) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getVisibility() == View.VISIBLE
                    && text.getText() != null
                    && text.getText().length() > 0
                    && text.getLayout() != null) {
                texts.add(text.getText() + "@vis=" + text.getVisibility()
                        + " h=" + text.getHeight()
                        + " color=" + Integer.toHexString(text.getCurrentTextColor()));
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectLabels(group.getChildAt(i), texts);
            }
        }
    }

    private static void dumpViews(View view, StringBuilder sb, int depth) {
        sb.append('\n');
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        sb.append(view.getClass().getSimpleName())
                .append(" id=").append(view.getId())
                .append(" vis=").append(view.getVisibility())
                .append(" w=").append(view.getWidth())
                .append(" h=").append(view.getHeight());
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            sb.append(" text='").append(text.getText())
                    .append("' size=").append(text.getTextSize())
                    .append(" color=").append(Integer.toHexString(text.getCurrentTextColor()));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                dumpViews(group.getChildAt(i), sb, depth + 1);
            }
        }
    }
}
