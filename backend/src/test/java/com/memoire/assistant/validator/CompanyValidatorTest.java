package com.memoire.assistant.validator;

import com.memoire.assistant.dto.CompanyCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CompanyValidatorTest {

    private CompanyValidator validator;
    private CompanyCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        validator = new CompanyValidator();
        validRequest = new CompanyCreateRequest();
        validRequest.setName("Valid Company");
        validRequest.setSector("Technology");
        validRequest.setSize("PME");
        validRequest.setWebsite("https://validcompany.com");
        validRequest.setPlan("Pro");
    }

    @Test
    void testValidate_ValidRequest_ShouldNotThrowException() {
        assertDoesNotThrow(() -> {
            validator.validate(validRequest);
        });
    }

    @Test
    void testValidate_NullRequest_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(null);
        });
        assertEquals("La requête de création d'entreprise ne peut pas être nulle", exception.getMessage());
    }

    @Test
    void testValidate_EmptyName_ShouldThrowException() {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("");
        request.setSector("Technology");
        request.setSize("PME");
        request.setWebsite("https://valid.com");
        request.setPlan("Pro");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(request);
        });
        assertEquals("Le nom de l'entreprise est obligatoire", exception.getMessage());
    }

    @Test
    void testValidate_ShortName_ShouldThrowException() {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("A");
        request.setSector("Technology");
        request.setSize("PME");
        request.setWebsite("https://valid.com");
        request.setPlan("Pro");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(request);
        });
        assertEquals("Le nom de l'entreprise doit contenir entre 2 et 100 caractères", exception.getMessage());
    }

    @Test
    void testValidate_LongName_ShouldThrowException() {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("A".repeat(101));
        request.setSector("Technology");
        request.setSize("PME");
        request.setWebsite("https://valid.com");
        request.setPlan("Pro");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(request);
        });
        assertEquals("Le nom de l'entreprise doit contenir entre 2 et 100 caractères", exception.getMessage());
    }

    @Test
    void testValidate_EmptySector_ShouldThrowException() {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("Valid Company");
        request.setSector("");
        request.setSize("PME");
        request.setWebsite("https://valid.com");
        request.setPlan("Pro");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(request);
        });
        assertEquals("Le secteur d'activité est obligatoire", exception.getMessage());
    }

    @Test
    void testValidate_ShortSector_ShouldThrowException() {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("Valid Company");
        request.setSector("A");
        request.setSize("PME");
        request.setWebsite("https://valid.com");
        request.setPlan("Pro");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(request);
        });
        assertEquals("Le secteur doit contenir entre 2 et 50 caractères", exception.getMessage());
    }

    @Test
    void testValidate_LongSector_ShouldThrowException() {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("Valid Company");
        request.setSector("A".repeat(51));
        request.setSize("PME");
        request.setWebsite("https://valid.com");
        request.setPlan("Pro");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(request);
        });
        assertEquals("Le secteur doit contenir entre 2 et 50 caractères", exception.getMessage());
    }

    @Test
    void testValidate_EmptySize_ShouldThrowException() {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("Valid Company");
        request.setSector("Technology");
        request.setSize("");
        request.setWebsite("https://valid.com");
        request.setPlan("Pro");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(request);
        });
        assertEquals("La taille de l'entreprise est obligatoire", exception.getMessage());
    }

    @Test
    void testValidate_InvalidSize_ShouldThrowException() {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("Valid Company");
        request.setSector("Technology");
        request.setSize("Invalid");
        request.setWebsite("https://valid.com");
        request.setPlan("Pro");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(request);
        });
        assertEquals("La taille doit être: Startup, PME, ETI ou Grande Entreprise", exception.getMessage());
    }

    @Test
    void testValidate_InvalidWebsite_ShouldThrowException() {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("Valid Company");
        request.setSector("Technology");
        request.setSize("PME");
        request.setWebsite("invalid-url");
        request.setPlan("Pro");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(request);
        });
        assertEquals("L'URL du site web n'est pas valide", exception.getMessage());
    }

    @Test
    void testValidate_ValidWebsite_ShouldNotThrowException() {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("Valid Company");
        request.setSector("Technology");
        request.setSize("PME");
        request.setWebsite("https://valid.com");
        request.setPlan("Pro");

        assertDoesNotThrow(() -> {
            validator.validate(request);
        });
    }

    @Test
    void testValidate_EmptyPlan_ShouldThrowException() {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("Valid Company");
        request.setSector("Technology");
        request.setSize("PME");
        request.setWebsite("https://valid.com");
        request.setPlan("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(request);
        });
        assertEquals("Le plan d'abonnement est obligatoire", exception.getMessage());
    }

    @Test
    void testValidate_InvalidPlan_ShouldThrowException() {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("Valid Company");
        request.setSector("Technology");
        request.setSize("PME");
        request.setWebsite("https://valid.com");
        request.setPlan("Invalid");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(request);
        });
        assertEquals("Le plan doit être: Basic, Pro ou Enterprise", exception.getMessage());
    }
}
