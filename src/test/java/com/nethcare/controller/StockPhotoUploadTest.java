package com.nethcare.controller;

import com.nethcare.exception.BusinessException;
import com.nethcare.model.StockCategory;
import com.nethcare.model.StockItem;
import com.nethcare.repository.StockImageRepository;
import com.nethcare.repository.StockItemRepository;
import com.nethcare.service.StockImageService;
import com.nethcare.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockPhotoUploadTest {
    @Autowired MockMvc mvc;
    @Autowired StockService stock;
    @Autowired StockItemRepository items;
    @Autowired StockImageRepository images;
    private final List<Long> createdIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : createdIds) {
            items.findById(id).ifPresent(item -> {
                if (item.getImageName() != null && item.getImageName().startsWith("upload-")) {
                    images.deleteById(item.getImageName());
                }
                items.deleteById(id);
            });
        }
    }

    private byte[] photoBytes(String format, int width, int height) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, out));
        return out.toByteArray();
    }

    private MockMultipartFile photo() throws Exception {
        return new MockMultipartFile("photo", "spectacle.png", "image/png", photoBytes("png", 20, 10));
    }

    private StockItem item(String code) {
        StockItem item = new StockItem();
        item.setItemCode(code);
        item.setName("Photo test frame");
        item.setCategory(StockCategory.FRAME);
        item.setUnitPrice(new BigDecimal("6500"));
        item.setQuantity(4);
        return item;
    }

    private StockItem add(String code) throws Exception {
        StockItem saved = stock.add(item(code), photo());
        createdIds.add(saved.getId());
        return saved;
    }

    @Test
    void staffCanUploadAndPublicGalleryServesPersistedPhoto() throws Exception {
        MockMultipartFile photo = photo();
        mvc.perform(multipart("/stock").file(photo).param("itemCode", "PHOTO-HTTP")
                        .param("name", "Uploaded frame").param("category", "FRAME")
                        .param("unitPrice", "6500").param("quantity", "2")
                        .with(user("staff").roles("STAFF_NURSE")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/stock"));
        StockItem saved = items.findByItemCode("PHOTO-HTTP").orElseThrow();
        createdIds.add(saved.getId());
        assertTrue(images.existsById(saved.getImageName()));
        mvc.perform(get(saved.getImageUrl()))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(photo.getBytes()));
        mvc.perform(get("/frames"))
                .andExpect(status().isOk()).andExpect(content().string(containsString(saved.getImageUrl())));
        mvc.perform(get("/stock/" + saved.getId() + "/edit").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
                .andExpect(content().string(containsString(saved.getImageUrl())));
        mvc.perform(get("/stock").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString(saved.getImageUrl())));
    }

    @Test
    void editingRetainsReplacesAndRemovesPhotosWithoutChangingCounts() throws Exception {
        StockItem saved = add("PHOTO-EDIT");
        String first = saved.getImageName();
        stock.reserve(saved.getId(), 2);
        StockItem incoming = item("PHOTO-EDIT");
        incoming.setQuantity(99);
        stock.update(saved.getId(), incoming, null, false);
        assertEquals(first, stock.get(saved.getId()).getImageName());
        stock.update(saved.getId(), incoming, photo(), false);
        StockItem replaced = stock.get(saved.getId());
        String second = replaced.getImageName();
        assertNotEquals(first, second);
        assertFalse(images.existsById(first));
        assertEquals(4, replaced.getQuantity());
        assertEquals(2, replaced.getReserved());
        stock.update(saved.getId(), incoming, null, true);
        assertNull(stock.get(saved.getId()).getImageName());
        assertFalse(images.existsById(second));
        mvc.perform(get("/images/stock/" + second)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateStockSaveRollsBackNewImage() throws Exception {
        StockItem saved = add("PHOTO-DUPLICATE");
        long before = images.count();
        assertThrows(BusinessException.class, () -> stock.add(item(saved.getItemCode()), photo()));
        assertEquals(before, images.count());
        assertTrue(images.existsById(saved.getImageName()));
    }

    @Test
    void rejectsFakeOversizedAndOverDimensionPhotosAndPreservesExistingPhoto() throws Exception {
        StockItem saved = add("PHOTO-INVALID");
        String original = saved.getImageName();
        List<MockMultipartFile> invalid = List.of(
                new MockMultipartFile("photo", "fake.png", "image/png", "not an image".getBytes()),
                new MockMultipartFile("photo", "large.png", "image/png", new byte[(int) StockImageService.MAX_BYTES + 1]),
                new MockMultipartFile("photo", "wide.png", "image/png", photoBytes("png", 4097, 1)),
                new MockMultipartFile("photo", "frame.gif", "image/gif", photoBytes("gif", 20, 10)));
        for (MockMultipartFile bad : invalid) {
            assertThrows(BusinessException.class, () -> stock.update(saved.getId(), item(saved.getItemCode()), bad, false));
            assertEquals(original, stock.get(saved.getId()).getImageName());
        }
        mvc.perform(multipart("/stock/" + saved.getId()).file(invalid.get(0))
                        .param("name", "Retained form data").param("category", "FRAME")
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk()).andExpect(view().name("stock/form"))
                .andExpect(content().string(containsString("Choose a valid JPEG or PNG image.")))
                .andExpect(content().string(containsString("Retained form data")))
                .andExpect(content().string(containsString(saved.getImageUrl())));
    }

    @Test
    void detectsJpegContentAndKeepsLegacyPhotosWhenEditing() throws Exception {
        StockItem legacy = item("PHOTO-LEGACY");
        legacy.setImageName("classic-frame.jpg");
        StockItem saved = stock.add(legacy);
        createdIds.add(saved.getId());
        assertEquals("/images/frames/classic-frame.jpg", saved.getImageUrl());
        stock.update(saved.getId(), item("PHOTO-LEGACY"), null, false);
        assertEquals(legacy.getImageName(), stock.get(saved.getId()).getImageName());
        MockMultipartFile jpeg = new MockMultipartFile("photo", "wrong-extension.png", "image/png", photoBytes("jpeg", 20, 10));
        StockItem replaced = stock.update(saved.getId(), item("PHOTO-LEGACY"), jpeg, false);
        mvc.perform(get(replaced.getImageUrl()))
                .andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"));
    }

    @Test
    void unauthorizedRoleAndMissingCsrfCannotUpload() throws Exception {
        mvc.perform(multipart("/stock").file(photo()).with(user("patient").roles("PATIENT")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/stock").file(photo()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
