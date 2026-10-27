package com.rtms.backend.identity.service;

import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.identity.dto.LoginResponse;
import com.rtms.backend.identity.dto.UpdateMyProfileRequest;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

@Service
public class MyProfileService {
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;
    private final UserRepository users;
    private final FileStorageService storage;
    private final AuthService auth;

    public MyProfileService(UserRepository users, FileStorageService storage, AuthService auth) {
        this.users = users;
        this.storage = storage;
        this.auth = auth;
    }

    @Transactional
    public LoginResponse update(Long userId, UpdateMyProfileRequest request, MultipartFile avatar) {
        User user = activeUser(userId, true);
        boolean uploading = avatar != null && !avatar.isEmpty();
        if (request.fullName() == null || request.fullName().isBlank()) throw bad("Full name is required");
        if (uploading && request.removeAvatar()) throw bad("Choose a new photo or remove the current photo");
        if (avatar != null && avatar.isEmpty()) throw bad("Choose a non-empty image");
        if (uploading) validateAvatar(avatar);

        String oldKey = user.getAvatarUrl();
        String newKey = uploading ? storage.store(avatar, "rtms/users/" + userId + "/avatar")
                : request.removeAvatar() ? null : oldKey;
        if (!Objects.equals(oldKey, newKey)) {
            // A concurrent upload is serialized by the User row lock. Never delete a still-current photo.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { if (oldKey != null) storage.delete(oldKey); }
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED && uploading) storage.delete(newKey);
                }
            });
        }
        user.setFullName(request.fullName().trim());
        user.setPhone(clean(request.phone()));
        user.setAddress(clean(request.address()));
        user.setAvatarUrl(newKey);
        users.saveAndFlush(user);
        return auth.getMe(userId);
    }

    @Transactional(readOnly = true)
    public Photo avatar(Long userId) {
        String key = activeUser(userId, false).getAvatarUrl();
        if (key == null || key.isBlank()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No profile photo");
        return new Photo(storage.load(key), storage.mediaType(key));
    }

    private User activeUser(Long id, boolean lock) {
        User user = (lock ? users.findByIdForUpdate(id) : users.findById(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.isActive()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive");
        return user;
    }

    private void validateAvatar(MultipartFile file) {
        if (file.getContentType() == null || !Set.of("image/jpeg", "image/png").contains(file.getContentType()) || file.getSize() > MAX_AVATAR_BYTES) {
            throw bad("Choose a JPG or PNG image up to 5 MB");
        }
        storage.validate(file);
        try (var stream = file.getInputStream(); var input = ImageIO.createImageInputStream(stream)) {
            if (input == null) throw bad("The selected file is not a readable image");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw bad("The selected file is not a readable image");
            var reader = readers.next();
            try {
                reader.setInput(input);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > 16_000_000) throw bad("Choose an image with at most 16 megapixels");
                if (reader.read(0) == null) throw bad("The selected file is not a readable image");
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException ex) {
            throw bad("The selected file is not a readable image");
        }
    }

    public record Photo(Resource resource, org.springframework.http.MediaType mediaType) {}
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
