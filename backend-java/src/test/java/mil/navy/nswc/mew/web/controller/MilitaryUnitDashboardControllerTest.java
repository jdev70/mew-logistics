package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.service.DashboardService;
import mil.navy.nswc.mew.web.dto.UnitDashboardDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MilitaryUnitDashboardController.class)
class MilitaryUnitDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    void listUnitMetricsReturnsSnakeCaseJson() throws Exception {
        UnitDashboardDto dto = new UnitDashboardDto();
        dto.setId(UUID.randomUUID());
        dto.setName("USS Arleigh Burke (DDG-51)");
        dto.setUic("N21944");
        dto.setPersonnelReadinessPct(new BigDecimal("96.50"));
        dto.setFleetAvgAo(98.2);

        when(dashboardService.getUnitDashboardMetrics(null)).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/v1/dashboard/units/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uic").value("N21944"))
                .andExpect(jsonPath("$[0].fleet_avg_ao").value(98.2));
    }

    @Test
    void exportCsvReturnsDownloadableCsv() throws Exception {
        when(dashboardService.exportUnitMetricsCsv(null))
                .thenReturn("Unit Name,UIC\nUSS Arleigh Burke,N21944\n");

        mockMvc.perform(get("/api/v1/dashboard/units/export_csv/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"unit_reliability_All_Assets.csv\""))
                .andExpect(content().contentType("text/csv"));
    }
}
