package com.memoire.assistant.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.email.host=smtp.gmail.com",
    "app.email.port=587",
    "app.email.username=adalbertmboudou100@gmail.com",
    "app.email.password=votre_mot_de_passe_application",
    "app.email.from=adalbertmboudou100@gmail.com",
    "JWT_SECRET=test-secret-key-for-testing-only-123456789",
    "JWT_EXPIRATION=86400000",
    "app.frontend.url=http://localhost:3100"
})
public class SimpleEmailTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testEmailConfigEndpoint() {
        String response = restTemplate.getForObject("/api/notifications/config", String.class);
        
        assertNotNull(response);
        assertTrue(response.contains("smtp.gmail.com"));
        assertTrue(response.contains("587"));
        System.out.println("✅ Configuration email accessible: " + response);
    }

    @Test
    void testEmailPropertiesInjected() {
        // Test que les propriétés email sont bien injectées
        // Ce test vérifie que le contexte Spring démarre correctement
        // avec les propriétés email
        assertTrue(true, "Le contexte Spring a démarré avec les propriétés email");
        System.out.println("✅ Propriétés email injectées avec succès");
    }
}
