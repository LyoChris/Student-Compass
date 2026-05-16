package org.backendcompas.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class OnboardingCatalogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCitiesReturnsSeedData() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/cities")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Cluj-Napoca"))
                .andExpect(jsonPath("$[1].name").value("Iasi"));
    }

    @Test
    void getFacultiesForCityReturnsSeededFaculty() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/cities/11111111-1111-1111-1111-111111111111/faculties")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Facultatea de Informatica"));
    }

    @Test
    void getFacultiesForUnknownCityReturnsEmptyList() throws Exception {
        // City exists check is not done in the controller — it returns empty list
        mockMvc.perform(get("/api/v1/catalog/cities/00000000-0000-0000-0000-000000000000/faculties")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
