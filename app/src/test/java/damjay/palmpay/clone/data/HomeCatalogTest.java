package damjay.palmpay.clone.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.model.PromotionCard;
import damjay.palmpay.clone.model.QuickAction;
import damjay.palmpay.clone.model.ServiceAction;

/** Contract tests for the data that populates the home screen. */
public class HomeCatalogTest {

    @Test
    public void quickActionsContainTheExpectedFourEntries() {
        List<QuickAction> actions = HomeCatalog.quickActions();

        assertEquals(4, actions.size());
        assertEquals(R.string.quick_action_to_bank, actions.get(0).getTitleRes());
        assertEquals(R.string.quick_action_palmpay, actions.get(1).getTitleRes());
        assertEquals(R.string.quick_action_savings, actions.get(2).getTitleRes());
        assertEquals(R.string.quick_action_cards, actions.get(3).getTitleRes());

        Set<Integer> iconIds = new HashSet<>();
        for (QuickAction action : actions) {
            assertNotEquals(0, action.getIconRes());
            assertNotEquals(0, action.getBackgroundColorRes());
            assertNotEquals(0, action.getIconColorRes());
            iconIds.add(action.getIconRes());
        }
        assertEquals("Quick actions should use distinct icons", actions.size(), iconIds.size());
    }

    @Test
    public void serviceGridHasEightUsableEntries() {
        List<ServiceAction> services = HomeCatalog.services();

        assertEquals(8, services.size());
        assertEquals(R.string.service_airtime, services.get(0).getTitleRes());
        assertEquals(R.string.service_more, services.get(services.size() - 1).getTitleRes());
        for (ServiceAction service : services) {
            assertTrue(service.getTitleRes() > 0);
            assertTrue(service.getIconRes() > 0);
            assertTrue(service.getBackgroundColorRes() > 0);
            assertTrue(service.getIconColorRes() > 0);
        }
    }

    @Test
    public void promotionsHaveCopyIllustrationsAndDifferentSurfaces() {
        List<PromotionCard> promotions = HomeCatalog.promotions();

        assertEquals(2, promotions.size());
        assertEquals(R.string.cashbox_title, promotions.get(0).getTitleRes());
        assertEquals(R.string.fixed_savings_title, promotions.get(1).getTitleRes());
        assertNotNull(promotions.get(0));
        assertNotNull(promotions.get(1));
        assertNotEquals(
                promotions.get(0).getBackgroundColorRes(),
                promotions.get(1).getBackgroundColorRes());
        for (PromotionCard promotion : promotions) {
            assertTrue(promotion.getEyebrowRes() > 0);
            assertTrue(promotion.getSubtitleRes() > 0);
            assertTrue(promotion.getActionRes() > 0);
            assertTrue(promotion.getIllustrationRes() > 0);
        }
    }
}
