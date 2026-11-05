package com.rtms.backend.identity.service;

import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.identity.dto.LoginResponse;
import com.rtms.backend.identity.dto.UpdateMyProfileRequest;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyProfileServiceTest {
    @TempDir Path directory;
    UserRepository users;
    AuthService auth;
    FileStorageService storage;
    MyProfileService service;
    User user;

    @BeforeEach void setup() {
        users=mock(UserRepository.class);auth=mock(AuthService.class);
        storage=new FileStorageService(directory.toString(),"");
        service=new MyProfileService(users,storage,auth);
        user=new User();user.setId(7L);user.setFullName("Before");user.setEmail("owner@demo.test");user.setActive(true);
        Role role=new Role();role.setName("HORSE_OWNER");user.setRole(role);user.setPasswordHash("unchanged-hash");
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(auth.getMe(7L)).thenAnswer(i -> LoginResponse.fromUser(user,null));
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach void cleanup() { TransactionSynchronizationManager.clearSynchronization(); }

    @Test void basicEditKeepsAccountAuthorityAndExistingPhoto() throws Exception {
        String old=storage.store(png(),"avatar");user.setAvatarUrl(old);
        var result=service.update(7L,new UpdateMyProfileRequest("  New name  "," 0909000000 ","  ",false),null);
        assertEquals("New name",result.getFullName());assertEquals("0909000000",result.getPhone());assertNull(result.getAddress());
        assertEquals("owner@demo.test",user.getEmail());assertEquals("HORSE_OWNER",user.getRole().getName());
        assertEquals("unchanged-hash",user.getPasswordHash());assertEquals(old,user.getAvatarUrl());
        assertTrue(storage.load(old).exists());
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        verify(users).findByIdForUpdate(7L);
    }

    @Test void replacingPhotoDeletesOldOnlyAfterCommit() throws Exception {
        String old=storage.store(png(),"avatar");user.setAvatarUrl(old);
        String oldUrl=LoginResponse.fromUser(user,null).getAvatarUrl();
        var result=service.update(7L,request(false),png());String replacement=user.getAvatarUrl();
        assertNotEquals(old,replacement);assertTrue(storage.load(old).exists());assertTrue(storage.load(replacement).exists());
        assertNotEquals(oldUrl,result.getAvatarUrl());assertFalse(result.getAvatarUrl().contains("local:"));
        complete(TransactionSynchronization.STATUS_COMMITTED);
        assertThrows(ResponseStatusException.class,()->storage.load(old));assertTrue(storage.load(replacement).exists());
    }

    @Test void rollingBackReplacementKeepsOldAndRemovesNew() throws Exception {
        String old=storage.store(png(),"avatar");user.setAvatarUrl(old);
        service.update(7L,request(false),png());String replacement=user.getAvatarUrl();
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertTrue(storage.load(old).exists());assertThrows(ResponseStatusException.class,()->storage.load(replacement));
    }

    @Test void databaseFailureCleansUploadedPhotoOnRollback() throws Exception {
        String old=storage.store(png(),"avatar");user.setAvatarUrl(old);
        doThrow(new IllegalStateException("save failed")).when(users).saveAndFlush(user);
        assertThrows(IllegalStateException.class,()->service.update(7L,request(false),png()));
        String uploaded=user.getAvatarUrl();complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertTrue(storage.load(old).exists());assertThrows(ResponseStatusException.class,()->storage.load(uploaded));
    }

    @Test void deletingPhotoIsAlsoCommitSafe() throws Exception {
        String old=storage.store(png(),"avatar");user.setAvatarUrl(old);
        service.update(7L,request(true),null);assertNull(user.getAvatarUrl());assertTrue(storage.load(old).exists());
        complete(TransactionSynchronization.STATUS_COMMITTED);assertThrows(ResponseStatusException.class,()->storage.load(old));
    }

    @Test void rejectsCorruptImageEvenWithJpegMagicBytes() {
        reject(new MockMultipartFile("avatar","fake.jpg","image/jpeg",new byte[]{(byte)255,(byte)216,(byte)255,0,1,2}));
    }
    @Test void rejectsPdf() { reject(new MockMultipartFile("avatar","file.pdf","application/pdf","%PDF-test".getBytes())); }
    @Test void rejectsMissingMediaType() { reject(new MockMultipartFile("avatar","file.jpg",null,new byte[]{1,2,3})); }
    @Test void rejectsEmptyFile() { reject(new MockMultipartFile("avatar","file.png","image/png",new byte[0])); }
    @Test void rejectsOversizedFileBeforeStorage() { reject(new MockMultipartFile("avatar","file.png","image/png",new byte[5*1024*1024+1])); }
    @Test void rejectsUploadAndRemovalTogether() throws Exception {
        assertThrows(ResponseStatusException.class,()->service.update(7L,request(true),png()));verify(users,never()).saveAndFlush(any());
    }
    @Test void inactiveAccountCannotWrite() throws Exception {
        user.setActive(false);
        var ex=assertThrows(ResponseStatusException.class,()->service.update(7L,request(false),png()));
        assertEquals(403,ex.getStatusCode().value());verify(users,never()).saveAndFlush(any());
    }
    @Test void missingAvatarReturns404() {
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.avatar(7L)).getStatusCode().value());
    }
    @Test void avatarReadUsesTheAuthenticatedUserOnly() throws Exception {
        user.setAvatarUrl(storage.store(png(),"avatar"));
        assertEquals("image/png",service.avatar(7L).mediaType().toString());verify(users).findById(7L);
    }

    private void reject(MockMultipartFile file) {
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.update(7L,request(false),file)).getStatusCode().value());
        verify(users,never()).saveAndFlush(any());assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }
    private void complete(int status) {
        for(var synchronization:TransactionSynchronizationManager.getSynchronizations()) {
            if(status==TransactionSynchronization.STATUS_COMMITTED) synchronization.afterCommit();
            synchronization.afterCompletion(status);
        }
    }
    private static UpdateMyProfileRequest request(boolean remove) { return new UpdateMyProfileRequest("Name",null,null,remove); }
    static MockMultipartFile png() throws Exception {
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB),"png",out);
        return new MockMultipartFile("avatar","avatar.png","image/png",out.toByteArray());
    }
}
