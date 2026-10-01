package com.nethcare.controller;

import com.nethcare.model.StockCategory;
import com.nethcare.model.StockItem;
import com.nethcare.service.StockService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicPagesTest {
    @Autowired MockMvc mvc;
    @MockBean StockService stock;

    @Test
    void publicPagesRenderWithoutAuthentication() throws Exception {
        for (String path : List.of("/", "/about", "/contact")) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("class=\"public-page")))
                    .andExpect(content().string(containsString("aria-current=\"page\"")));
        }
        mvc.perform(get("/contact")).andExpect(content().string(containsString("Phone number coming soon")))
                .andExpect(content().string(not(containsString("tel:null"))));
    }

    @Test
    void cataloguePreservesCategoryAndQueryAndRendersRealProducts() throws Exception {
        StockItem frame = new StockItem();
        frame.setItemCode("TEST-OPTICAL");
        frame.setName("Test optical frame");
        frame.setCategory(StockCategory.FRAME);
        frame.setUnitPrice(new BigDecimal("12500.00"));
        frame.setQuantity(7);
        frame.setImageName(" ");
        when(stock.gallery(StockCategory.FRAME, "optical")).thenReturn(List.of(frame));
        mvc.perform(get("/frames").param("category", "FRAME").param("q", "optical"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Test optical frame")))
                .andExpect(content().string(containsString("12,500.00")))
                .andExpect(content().string(containsString("Photo coming soon")))
                .andExpect(content().string(containsString("name=\"category\" value=\"FRAME\"")))
                .andExpect(content().string(containsString("category=CONTACT_LENS&amp;q=optical")))
                .andExpect(content().string(not(containsString("7 left"))));
        verify(stock).gallery(StockCategory.FRAME, "optical");
    }

    @Test
    void homeFeaturesOnlyThreeRealFramesAndHandlesAnEmptyCatalogue() throws Exception {
        java.util.ArrayList<StockItem> frames = new java.util.ArrayList<>();
        for (int n = 1; n <= 4; n++) {
            StockItem frame = new StockItem();
            frame.setName("Featured frame " + n);
            frame.setItemCode("FEATURE-" + n);
            frame.setCategory(StockCategory.FRAME);
            frame.setUnitPrice(new BigDecimal("8500.00"));
            frames.add(frame);
        }
        when(stock.gallery(StockCategory.FRAME, null)).thenReturn(frames);
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Featured frame 1")))
                .andExpect(content().string(containsString("Featured frame 3")))
                .andExpect(content().string(not(containsString("Featured frame 4"))))
                .andExpect(content().string(containsString("8,500.00")));
        when(stock.gallery(StockCategory.FRAME, null)).thenReturn(List.of());
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Find your fit in store")))
                .andExpect(content().string(not(containsString("Featured frame"))));
    }

    @Test
    void emptyCatalogueAndEmptySearchHaveDifferentHelpfulMessages() throws Exception {
        when(stock.gallery(null, null)).thenReturn(List.of());
        when(stock.gallery(null, "missing")).thenReturn(List.of());
        mvc.perform(get("/frames")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Your next pair is worth a visit.")));
        mvc.perform(get("/frames").param("q", "missing")).andExpect(status().isOk())
                .andExpect(content().string(containsString("No items match these filters.")))
                .andExpect(content().string(containsString("View all items")));
    }
}
