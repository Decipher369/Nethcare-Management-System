package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.model.StockImage;
import com.nethcare.repository.StockImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class StockImageService {
    public static final long MAX_BYTES = 5 * 1024 * 1024;
    private final StockImageRepository images;

    public StockImageService(StockImageRepository images) {
        this.images = images;
    }

    /** Called inside the stock transaction so failed saves cannot leave orphan photos. */
    @Transactional
    public String replace(String currentName, MultipartFile photo, boolean remove) {
        if (photo == null || photo.isEmpty()) {
            if (remove) deleteUpload(currentName);
            return remove ? null : currentName;
        }
        if (photo.getSize() > MAX_BYTES) {
            throw new BusinessException("Choose a photo no larger than 5 MB.");
        }
        try {
            byte[] data = photo.getBytes();
            if (data.length > MAX_BYTES) throw new BusinessException("Choose a photo no larger than 5 MB.");
            String format = validatedFormat(data);
            String name = "upload-" + UUID.randomUUID() + (format.equals("png") ? ".png" : ".jpg");
            images.save(new StockImage(name, format.equals("png") ? "image/png" : "image/jpeg", data));
            deleteUpload(currentName);
            return name;
        } catch (IOException ex) {
            throw new BusinessException("The photo could not be read. Choose a valid JPEG or PNG image.");
        }
    }

    private String validatedFormat(byte[] data) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (input == null) throw new BusinessException("Choose a valid JPEG or PNG image.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BusinessException("Choose a valid JPEG or PNG image.");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg")) {
                    throw new BusinessException("Only JPEG and PNG photos are supported.");
                }
                reader.setInput(input);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 4096 || height > 4096) {
                    throw new BusinessException("Choose a photo at most 4096 pixels wide and tall.");
                }
                // Decode once to reject corrupt files, rather than trusting the extension or MIME header.
                if (reader.read(0) == null) throw new BusinessException("Choose a valid JPEG or PNG image.");
                return format;
            } finally {
                reader.dispose();
            }
        }
    }

    private void deleteUpload(String name) {
        if (name != null && name.startsWith("upload-")) images.deleteById(name);
    }

    @Transactional(readOnly = true)
    public Optional<StockImage> find(String name) {
        return images.findById(name);
    }
}
