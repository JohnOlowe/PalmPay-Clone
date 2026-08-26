package damjay.palmpay.clone.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import damjay.palmpay.clone.R;

public class ServiceActionTest {

    @Test
    public void serviceActionKeepsItsPresentationContract() {
        ServiceAction service = new ServiceAction(
                R.string.service_airtime,
                R.drawable.ic_airtime,
                R.color.pastel_lilac,
                R.color.icon_purple);

        assertEquals(R.string.service_airtime, service.getTitleRes());
        assertEquals(R.drawable.ic_airtime, service.getIconRes());
        assertEquals(R.color.pastel_lilac, service.getBackgroundColorRes());
        assertEquals(R.color.icon_purple, service.getIconColorRes());
    }
}
