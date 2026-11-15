package com.rtms.backend.admission.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.AdmissionGroomAssignment;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AssignmentEndReason;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.notification.service.EmailService;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GroomAssignmentNotificationServiceTest {
    final UserRepository users = mock(UserRepository.class);
    final CandidateHorseProfileRepository candidates = mock(CandidateHorseProfileRepository.class);
    final NotificationService notifications = mock(NotificationService.class);
    final EmailService emails = mock(EmailService.class);
    final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    final GroomAssignmentNotificationService service = new GroomAssignmentNotificationService(
            users, candidates, notifications, emails, transactions);
    AdmissionApplication admission;
    AdmissionGroomAssignment row;

    @BeforeEach
    void setup() {
        admission = new AdmissionApplication(); admission.setId(10L);
        admission.setGroomReviewDueAt(LocalDateTime.now().plusHours(72));
        row = new AdmissionGroomAssignment(); row.setId(100L); row.setGroomId(2L);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        CandidateHorseProfile horse = new CandidateHorseProfile(); horse.setName("Mercury");
        when(candidates.findByAdmissionId(10L)).thenReturn(Optional.of(horse));
        when(users.findById(2L)).thenReturn(Optional.of(user(2L, "New Groom")));
        when(users.findById(1L)).thenReturn(Optional.of(user(1L, "Old Groom")));
        when(users.findActiveClubManagers()).thenReturn(List.of(user(9L, "Manager"), user(8L, "Manager Two")));
    }

    private User user(Long id, String name) {
        User user = new User(); user.setId(id); user.setFullName(name); user.setEmail(id + "@test.invalid");
        return user;
    }

    @Test
    void initialAssignmentNotifiesAndEmailsOnlyNewGroomAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.afterAssignment(admission, row, null, null);
            verifyNoInteractions(notifications, emails);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(notifications).createNotification(eq(2L), eq("New admission to review"),
                    contains("Mercury"), eq(NotificationTypes.ADMISSION_GROOM_REVIEW_ASSIGNED),
                    eq("ADMISSION"), eq(10L), eq("2:ADMISSION_10:ADMISSION_GROOM_REVIEW_ASSIGNED:assignment:100"));
            verify(emails).sendAdmissionGroomReviewAssignedEmail(eq("2@test.invalid"), eq("New Groom"),
                    eq("Mercury"), eq(admission.getGroomReviewDueAt()), contains("/groom/admissions/10"));
            verifyNoMoreInteractions(notifications);
            verify(transactions).commit(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rollbackDoesNotSendNotificationsOrEmail() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.afterAssignment(admission, row, null, null);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(1));
            verifyNoInteractions(notifications, emails, transactions);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void reassignmentNotifiesPreviousGroomAndEveryActiveManagerWithReason() {
        service.afterAssignment(admission, row, 1L, AssignmentEndReason.MANUAL);
        verify(notifications).createNotification(eq(1L), eq("Admission moved to another groom"),
                contains("Reassigned by manager"), eq(NotificationTypes.ADMISSION_GROOM_REVIEW_REASSIGNED),
                eq("ADMISSION"), eq(10L), contains(":assignment:100"));
        for (Long manager : List.of(9L, 8L)) {
            verify(notifications).createNotification(eq(manager), eq("Admission reassigned"),
                    eq("Mercury moved from Old Groom to New Groom. Reason: Reassigned by manager"),
                    eq(NotificationTypes.ADMISSION_GROOM_REVIEW_REASSIGNED), eq("ADMISSION"), eq(10L),
                    contains(":assignment:100"));
        }
        verify(emails).sendAdmissionGroomReviewAssignedEmail(anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void eachAssignmentHasItsOwnDeduplicationKey() {
        service.afterAssignment(admission, row, 1L, AssignmentEndReason.OVERDUE);
        row.setId(101L);
        service.afterAssignment(admission, row, 1L, AssignmentEndReason.FULL);
        verify(notifications).createNotification(eq(2L), anyString(), contains("Review due"), anyString(),
                eq("ADMISSION"), eq(10L), eq("2:ADMISSION_10:ADMISSION_GROOM_REVIEW_ASSIGNED:assignment:100"));
        verify(notifications).createNotification(eq(2L), anyString(), contains("Review due"), anyString(),
                eq("ADMISSION"), eq(10L), eq("2:ADMISSION_10:ADMISSION_GROOM_REVIEW_ASSIGNED:assignment:101"));
        assertEquals("No longer a quarantine groom", GroomAssignmentNotificationService.reasonLabel(AssignmentEndReason.LOST_QUARANTINE));
    }
}
