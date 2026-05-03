package com.parc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.parc.service.UtilisateurService;

@SpringBootTest
class GestionParcApplicationTests {

    // Mock du service attendu par SecurityConfig
    @MockBean
    private UtilisateurService utilisateurService;

    @Test
    void contextLoads() {
        // Test minimal pour vérifier le démarrage du contexte
    }
}
