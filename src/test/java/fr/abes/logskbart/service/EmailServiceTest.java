package fr.abes.logskbart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceTest {

    private CapturingEmailService service;

    @BeforeEach
    void setUp() {
        service = new CapturingEmailService();
        ReflectionTestUtils.setField(service, "recipient", "fonctionnel@abes.fr");
        ReflectionTestUtils.setField(service, "env", "test");
        ReflectionTestUtils.setField(service, "serveurUrl", "https://bacon.abes.fr/reports/");
    }

    @Test
    void sendsAnImmediateLinkToTheOtherReport() {
        service.sendOtherErrorsEmail("TEST_PACKAGE.tsv");

        assertEquals(1, service.sendCount);
        assertTrue(service.requestJson.contains("bad/TEST_PACKAGE_other.bad"));
        assertTrue(service.requestJson.contains("erreurs hors 400"));
    }

    @Test
    void sendsOneDailyEmailWithEvery400ReportSortedByName() {
        boolean sent = service.sendDailyRecapEmail(List.of("B_400.bad", "A_400.bad"));

        assertTrue(sent);
        assertEquals(1, service.sendCount);
        assertTrue(service.requestJson.contains("bad/A_400.bad"));
        assertTrue(service.requestJson.contains("bad/B_400.bad"));
        assertTrue(service.requestJson.indexOf("A_400.bad") < service.requestJson.indexOf("B_400.bad"));
    }

    @Test
    void reportsDailyEmailFailureToTheScheduler() {
        service.sendResult = false;

        assertFalse(service.sendDailyRecapEmail(List.of("A_400.bad")));
        assertEquals(1, service.sendCount);
    }

    private static class CapturingEmailService extends EmailService {
        private String requestJson;
        private int sendCount;
        private boolean sendResult = true;

        @Override
        protected boolean sendMail(String requestJson) {
            this.requestJson = requestJson;
            this.sendCount++;
            return sendResult;
        }
    }
}
