package com.hospital.allocator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void rosterEndpointSolvesAndReturnsAssignments() throws Exception {
        String body = """
                {"nurses":[{"id":"A","skill":"GENERAL","maxShifts":1,"availableShifts":["S1","S2"]},
                           {"id":"B","skill":"GENERAL","maxShifts":1,"availableShifts":["S1"]}],
                 "shifts":[{"id":"S1","requiredSkill":"ANY","headcount":1},
                           {"id":"S2","requiredSkill":"ANY","headcount":1}]}
                """;
        mvc.perform(post("/api/roster").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssigned").value(2))
                .andExpect(jsonPath("$.feasible").value(true));
    }

    @Test
    void emptyRosterReturns400WithMessage() throws Exception {
        mvc.perform(post("/api/roster").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nurses\":[],\"shifts\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/roster").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sampleEndpointReturnsData() throws Exception {
        mvc.perform(get("/api/sample")).andExpect(status().isOk())
                .andExpect(jsonPath("$.roster.nurses").isArray());
    }
}
