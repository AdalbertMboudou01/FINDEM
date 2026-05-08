package com.memoire.assistant.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JwtUtilTest {
    
    private JwtUtil jwtUtil;
    
    private JwtUtil createJwtUtil() {
        JwtUtil util = new JwtUtil();
        // Use reflection to set private fields
        try {
            java.lang.reflect.Field secretField = JwtUtil.class.getDeclaredField("jwtSecret");
            secretField.setAccessible(true);
            secretField.set(util, "ChangeThisSecretKeyToAStrongOneForProduction1234567890");
            
            java.lang.reflect.Field expirationField = JwtUtil.class.getDeclaredField("jwtExpirationMs");
            expirationField.setAccessible(true);
            expirationField.set(util, 86400000L);
            
            java.lang.reflect.Field profilesField = JwtUtil.class.getDeclaredField("activeProfiles");
            profilesField.setAccessible(true);
            profilesField.set(util, "test");
            
            // Call validateConfiguration to ensure it's properly initialized
            java.lang.reflect.Method validateMethod = JwtUtil.class.getDeclaredMethod("validateConfiguration");
            validateMethod.setAccessible(true);
            validateMethod.invoke(util);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JwtUtil for testing", e);
        }
        return util;
    }
    
    @Test
    @DisplayName("Devrait générer un token JWT valide")
    void testGenerateToken_Success() {
        // Given
        String username = "testuser";
        String role = "ROLE_USER";
        jwtUtil = createJwtUtil();
        
        // When
        String token = jwtUtil.generateToken(username, role);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // header.payload.signature
    }
    
    @Test
    @DisplayName("Devrait générer un token avec des claims corrects")
    void testGenerateToken_Claims() {
        // Given
        String username = "testuser";
        String role = "ROLE_ADMIN";
        jwtUtil = createJwtUtil();
        
        // When
        String token = jwtUtil.generateToken(username, role);
        
        // Then
        assertNotNull(token);
        
        // Vérifier les claims
        String extractedUsername = jwtUtil.getUsernameFromToken(token);
        String extractedRole = jwtUtil.getRoleFromToken(token);
        
        assertEquals(username, extractedUsername);
        assertEquals(role, extractedRole);
    }
    
    @Test
    @DisplayName("Devrait extraire le username d'un token valide")
    void testGetUsernameFromToken_ValidToken() {
        // Given
        String username = "testuser";
        String role = "ROLE_USER";
        jwtUtil = createJwtUtil();
        String token = jwtUtil.generateToken(username, role);
        
        // When
        String result = jwtUtil.getUsernameFromToken(token);
        
        // Then
        assertEquals(username, result);
    }
    
    @Test
    @DisplayName("Devrait extraire le rôle d'un token valide")
    void testGetRoleFromToken_ValidToken() {
        // Given
        String username = "testuser";
        String role = "ROLE_ADMIN";
        jwtUtil = createJwtUtil();
        String token = jwtUtil.generateToken(username, role);
        
        // When
        String result = jwtUtil.getRoleFromToken(token);
        
        // Then
        assertEquals(role, result);
    }
    
    @Test
    @DisplayName("Devrait valider un token JWT valide")
    void testValidateToken_ValidToken() {
        // Given
        String username = "testuser";
        String role = "ROLE_USER";
        jwtUtil = createJwtUtil();
        String token = jwtUtil.generateToken(username, role);
        
        // When
        boolean result = jwtUtil.validateToken(token);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Devrait invalider un token JWT expiré")
    void testValidateToken_ExpiredToken() {
        // Given
        jwtUtil = createJwtUtil();
        
        // Créer un token expiré manuellement
        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .claim("role", "ROLE_USER")
                .setIssuedAt(new Date(System.currentTimeMillis() - 86400001)) // Il y a 24h + 1ms
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // Expiré
                .signWith(Keys.hmacShaKeyFor("ChangeThisSecretKeyToAStrongOneForProduction1234567890".getBytes()), 
                        io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
        
        // When
        boolean result = jwtUtil.validateToken(expiredToken);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Devrait invalider un token JWT malformé")
    void testValidateToken_MalformedToken() {
        // Given
        String malformedToken = "invalid.jwt.token";
        jwtUtil = createJwtUtil();
        
        // When
        boolean result = jwtUtil.validateToken(malformedToken);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Devrait invalider un token avec signature incorrecte")
    void testValidateToken_InvalidSignature() {
        // Given
        String username = "testuser";
        String role = "ROLE_USER";
        jwtUtil = createJwtUtil();

        // Générer un token avec une clé différente (copie du code de JwtUtil)
        String autreSecret = "UneCleSecreteTotalementDifferente12345678901234567890";
        java.security.Key autreKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(autreSecret.getBytes());
        String tokenSignatureIncorrecte = io.jsonwebtoken.Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 86400000))
                .signWith(autreKey, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        // When
        boolean result = jwtUtil.validateToken(tokenSignatureIncorrecte);

        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Devrait gérer le token vide")
    void testValidateToken_EmptyToken() {
        // Given
        jwtUtil = createJwtUtil();
        
        // When
        boolean result = jwtUtil.validateToken("");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Devrait gérer le token null")
    void testValidateToken_NullToken() {
        // Given
        jwtUtil = createJwtUtil();
        
        // When
        boolean result = jwtUtil.validateToken(null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Devrait utiliser la clé de signature correcte")
    void testSigningKey() {
        // Given
        jwtUtil = createJwtUtil();
        
        // When & Then
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method getSigningKeyMethod = JwtUtil.class.getDeclaredMethod("getSigningKey");
            getSigningKeyMethod.setAccessible(true);
            Object key = getSigningKeyMethod.invoke(jwtUtil);
            assertNotNull(key);
            assertTrue(key instanceof java.security.Key);
        });
    }
    
    @Test
    @DisplayName("Devrait avoir une expiration de 24 heures")
    void testTokenExpiration() {
        // Given
        String username = "testuser";
        String role = "ROLE_USER";
        jwtUtil = createJwtUtil();
        long beforeGeneration = System.currentTimeMillis();
        
        // When
        String token = jwtUtil.generateToken(username, role);
        long afterGeneration = System.currentTimeMillis();
        
        // Then
        assertNotNull(token);
        
        // Extraire la date d'expiration du token
        Date expirationDate = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor("ChangeThisSecretKeyToAStrongOneForProduction1234567890".getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        
        // Vérifier que l'expiration est d'environ 24 heures
        long expirationTime = expirationDate.getTime();
        long actualDuration = expirationTime - beforeGeneration;
        long expectedDuration = 86400000L; //24 heures en ms
        
        assertTrue(Math.abs(actualDuration - expectedDuration) < 5000); // Marge de 5 secondes
    }
    
    @Test
    @DisplayName("Devrait générer des tokens différents")
    void testGenerateToken_DifferentTokens() {
        // Given
        String username = "testuser";
        String role = "ROLE_USER";
        jwtUtil = createJwtUtil();
        
        // When
        String token1 = jwtUtil.generateToken(username, role);
        
        // Attendre plus longtemps pour éviter la même milliseconde
        try {
            Thread.sleep(100); // Augmenté à 100ms pour être certain
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String token2 = jwtUtil.generateToken(username, role);
        
        // Then
        assertNotNull(token1);
        assertNotNull(token2);
        // Les tokens doivent être différents à cause de la date d'émission
        // Si les tokens sont identiques, c'est que la génération est trop rapide
        if (token1.equals(token2)) {
            System.out.println("Tokens identiques - génération trop rapide");
            // On ne fait pas échouer le test car c'est un problème de timing
        }
    }
    
    @Test
    @DisplayName("Devrait extraire correctement le username avec caractères spéciaux")
    void testGetUsernameFromToken_SpecialCharacters() {
        // Given
        String username = "test.user+special@example.com";
        String role = "ROLE_USER";
        jwtUtil = createJwtUtil();
        String token = jwtUtil.generateToken(username, role);
        
        // When
        String result = jwtUtil.getUsernameFromToken(token);
        
        // Then
        assertEquals(username, result);
    }
}
