package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.GroupCandidateResponse;
import com.rtms.backend.training.dto.GroupMemberResponse;
import com.rtms.backend.training.dto.HorseStallInfo;
import com.rtms.backend.training.dto.RenameTrainingGroupRequest;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.GroupPlan;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.entity.TrainingGroupMember;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.TrainingGroupMemberRepository;
import com.rtms.backend.training.repository.TrainingGroupRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cài đặt TrainingGroupService. Luật thành viên được kiểm một lần khi thêm ngựa (không kiểm lại mỗi ngày như mô hình session cũ).
 * (Implementation of TrainingGroupService. Membership rules are checked once, when a horse is added (not re-checked every day
 * as the old session model did).)
 */
@Service
public class TrainingGroupServiceImpl implements TrainingGroupService {

    private static final List<TrainingPlanStatus> UNFINISHED =
            List.of(TrainingPlanStatus.UPCOMING, TrainingPlanStatus.ACTIVE);

    private final TrainingGroupRepository groupRepository;
    private final TrainingGroupMemberRepository memberRepository;
    private final GroupPlanRepository planRepository;
    private final CourseRepository courseRepository;
    private final HorseRepository horseRepository;
    private final UserRepository userRepository;
    private final TrainingDecisionService trainingDecisionService;
    private final GroupWorkoutSyncService groupSync;
    private final GroupPlanService planService;

    public TrainingGroupServiceImpl(TrainingGroupRepository groupRepository,
                                    TrainingGroupMemberRepository memberRepository,
                                    GroupPlanRepository planRepository,
                                    CourseRepository courseRepository,
                                    HorseRepository horseRepository,
                                    UserRepository userRepository,
                                    TrainingDecisionService trainingDecisionService,
                                    GroupWorkoutSyncService groupSync,
                                    GroupPlanService planService) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.planRepository = planRepository;
        this.courseRepository = courseRepository;
        this.horseRepository = horseRepository;
        this.userRepository = userRepository;
        this.trainingDecisionService = trainingDecisionService;
        this.groupSync = groupSync;
        this.planService = planService;
    }

    // =================================================================
    // XEM (Reading)
    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public List<TrainingGroupResponse> getGroups(AuthenticatedUser currentUser) {
        List<TrainingGroup> found = "HEAD_TRAINER".equals(currentUser.getRole())
                ? groupRepository.findByTrainerIdAndDisbandedAtIsNullOrderByNameAsc(currentUser.getUserId())
                : sortedByName(groupRepository.findByDisbandedAtIsNull());
        return toResponses(found);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingGroupResponse> getDisbandedGroups(AuthenticatedUser currentUser) {
        List<TrainingGroup> found = "HEAD_TRAINER".equals(currentUser.getRole())
                ? groupRepository.findByTrainerIdAndDisbandedAtIsNotNullOrderByNameAsc(currentUser.getUserId())
                : sortedByName(groupRepository.findByDisbandedAtIsNotNull());
        return toResponses(found);
    }

    private List<TrainingGroup> sortedByName(List<TrainingGroup> groups) {
        return groups.stream().sorted(Comparator.comparing(TrainingGroup::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupCandidateResponse> getCandidates(Long groupId, AuthenticatedUser currentUser) {
        TrainingGroup group = findLiveOwnGroup(groupId, currentUser);
        List<TrainingGroupMember> groupMembers = memberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId());
        Set<Long> memberHorseIds = groupMembers.stream().map(TrainingGroupMember::getHorseId).collect(Collectors.toSet());

        List<Long> candidateIds = memberRepository.findHorseIdsInAreasOf(currentUser.getUserId()).stream()
                .filter(id -> !memberHorseIds.contains(id)).toList();
        Map<Long, Horse> horsesById = horseRepository.findAllById(candidateIds).stream()
                .collect(Collectors.toMap(Horse::getId, Function.identity()));
        Map<Long, HorseStallInfo> stallByHorse = stallInfo(candidateIds);
        Map<Long, TrainingGroupMember> membershipByHorse = memberRepository.findByHorseIdIn(candidateIds).stream()
                .collect(Collectors.toMap(TrainingGroupMember::getHorseId, Function.identity()));
        Map<Long, String> horseNamesInGroup = horseNames(memberHorseIds);
        Map<Long, String> groomNames = userNames(
                stallByHorse.values().stream().map(HorseStallInfo::stallGroomId).collect(Collectors.toSet()));
        Map<Long, Long> memberByGroom = groupMembers.stream().filter(m -> m.getGroomId() != null)
                .collect(Collectors.toMap(TrainingGroupMember::getGroomId, TrainingGroupMember::getHorseId, (a, b) -> a));

        List<GroupCandidateResponse> result = new ArrayList<>();
        for (Long horseId : candidateIds) {
            Horse horse = horsesById.get(horseId);
            HorseStallInfo stall = stallByHorse.get(horseId);
            String reason = candidateReason(horse, stall, membershipByHorse.get(horseId), groupMembers.size(), memberByGroom,
                    horseNamesInGroup);
            String groomName = stall == null ? null : groomNames.get(stall.stallGroomId());
            result.add(new GroupCandidateResponse(horseId, horse.getName(), groomName, reason == null, reason));
        }
        return result;
    }

    // =================================================================
    // GHI (Writing)
    // =================================================================

    @Override
    @Transactional
    public TrainingGroupResponse createGroup(CreateTrainingGroupRequest request, AuthenticatedUser currentUser) {
        String name = normalizeName(request.name());
        assertNameFree(currentUser.getUserId(), name);

        TrainingGroup group = new TrainingGroup();
        group.setTrainerId(currentUser.getUserId());
        group.setName(name);
        group = groupRepository.saveAndFlush(group);

        Set<Long> horseIds = request.horseIds() == null ? Set.of() : new LinkedHashSet<>(request.horseIds());
        for (Long horseId : horseIds) {
            join(group, horseId, currentUser);
        }
        return toResponse(group);
    }

    @Override
    @Transactional
    public TrainingGroupResponse renameGroup(Long groupId, RenameTrainingGroupRequest request, AuthenticatedUser currentUser) {
        TrainingGroup group = findLiveOwnGroup(groupId, currentUser);
        String name = normalizeName(request.name());
        if (!name.equalsIgnoreCase(group.getName())) {
            assertNameFree(currentUser.getUserId(), name);
        }
        group.setName(name);
        return toResponse(groupRepository.saveAndFlush(group));
    }

    @Override
    @Transactional
    public void disbandGroup(Long groupId, AuthenticatedUser currentUser) {
        findLiveOwnGroup(groupId, currentUser);
        for (GroupPlan plan : planRepository.findByGroupIdAndStatusIn(groupId, UNFINISHED)) {
            planService.cancelPlan(plan.getId(), currentUser);
        }
        // Cancelling plans runs bulk updates that clear the persistence context, so the rows are read again.
        for (TrainingGroupMember member : memberRepository.findByGroupIdOrderByJoinedAtAsc(groupId)) {
            groupSync.exitGroup(member);
        }
        memberRepository.flush();
        TrainingGroup group = groupRepository.findById(groupId).orElseThrow();
        if (planRepository.existsByGroupId(groupId)) {
            group.setDisbandedAt(LocalDateTime.now());
            groupRepository.saveAndFlush(group);
        } else {
            groupRepository.delete(group);
            groupRepository.flush();
        }
    }

    @Override
    @Transactional
    public TrainingGroupResponse addHorse(Long groupId, Long horseId, AuthenticatedUser currentUser) {
        TrainingGroup group = findLiveOwnGroup(groupId, currentUser);
        join(group, horseId, currentUser);
        return toResponse(group);
    }

    @Override
    @Transactional
    public TrainingGroupResponse removeHorse(Long groupId, Long horseId, AuthenticatedUser currentUser) {
        TrainingGroup group = findLiveOwnGroup(groupId, currentUser);
        TrainingGroupMember member = memberRepository.findByHorseId(horseId)
                .filter(m -> m.getGroupId().equals(group.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_IN_GROUP",
                        "That horse is not in group '" + group.getName() + "'!"));
        groupSync.exitGroup(member);
        return toResponse(group);
    }

    @Override
    @Transactional
    public TrainingGroupResponse moveHorse(Long targetGroupId, Long horseId, AuthenticatedUser currentUser) {
        TrainingGroup target = findLiveOwnGroup(targetGroupId, currentUser);
        Optional<TrainingGroupMember> current = memberRepository.findByHorseId(horseId);
        if (current.isPresent()) {
            TrainingGroup source = findOwnGroup(current.get().getGroupId(), currentUser);
            if (source.getId().equals(target.getId())) {
                throw new ApiException(HttpStatus.CONFLICT, "HORSE_ALREADY_IN_GROUP",
                        "That horse is already in group '" + target.getName() + "'!");
            }
            groupSync.exitGroup(current.get());
        }
        join(target, horseId, currentUser);
        return toResponse(target);
    }

    // =================================================================
    // LUẬT THÀNH VIÊN (Membership rules)
    // =================================================================

    /**
     * Thêm một chiến mã vào nhóm sau khi kiểm đủ luật: ngựa tồn tại, ở trong khu của Trainer, được phép tập, chuồng có Groom,
     * chưa ở nhóm khác, nhóm chưa đủ 6 con (BR-10) và chưa có ngựa cùng Groom (BR-09).
     * (Adds a horse to a group after checking every rule: the horse exists, is in the Trainer's area, may train, its stall has a
     * Groom, it is in no other group, the group has fewer than 6 horses (BR-10) and no horse of the same Groom (BR-09).)
     */
    private void join(TrainingGroup group, Long horseId, AuthenticatedUser currentUser) {
        Horse horse = horseRepository.findById(horseId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse #" + horseId + " not found!"));
        HorseStallInfo stall = stallInfo(List.of(horseId)).get(horseId);

        assertInTrainerArea(horse, stall, currentUser.getUserId());
        trainingDecisionService.assertCanTrain(horse);
        if (stall.stallGroomId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "STALL_NO_GROOM", String.format(
                    "Stall '%s' of horse '%s' has no groom assigned. Contact the club manager to assign one!",
                    stall.stallCode(), horse.getName()));
        }

        Optional<TrainingGroupMember> existing = memberRepository.findByHorseId(horseId);
        if (existing.isPresent()) {
            boolean sameGroup = existing.get().getGroupId().equals(group.getId());
            throw new ApiException(HttpStatus.CONFLICT, sameGroup ? "HORSE_ALREADY_IN_GROUP" : "HORSE_IN_OTHER_GROUP",
                    sameGroup ? "Horse '" + horse.getName() + "' is already in this group!"
                            : "Horse '" + horse.getName() + "' is already in another group. Move it instead!");
        }

        List<TrainingGroupMember> groupMembers = memberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId());
        if (groupMembers.size() >= TrainingLimits.MAX_HORSES_PER_GROUP) {
            throw new ApiException(HttpStatus.CONFLICT, "GROUP_FULL", "Group '" + group.getName() + "' is full ("
                    + TrainingLimits.MAX_HORSES_PER_GROUP + " horses)!");
        }
        Optional<TrainingGroupMember> sameGroom = groupMembers.stream()
                .filter(m -> stall.stallGroomId().equals(m.getGroomId())).findFirst();
        if (sameGroom.isPresent()) {
            String other = horseNames(List.of(sameGroom.get().getHorseId())).get(sameGroom.get().getHorseId());
            throw new ApiException(HttpStatus.CONFLICT, "GROOM_ALREADY_IN_GROUP", String.format(
                    "Horse '%s' has the same groom as '%s', who is already in this group. A groom can lead only one horse per group!",
                    horse.getName(), other));
        }

        groupSync.assertCanJoin(group.getId());

        TrainingGroupMember member = new TrainingGroupMember();
        member.setGroupId(group.getId());
        member.setHorseId(horseId);
        member.setGroomId(stall.stallGroomId());
        try {
            member = memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            // Hai request đồng thời cùng qua các bước kiểm trên; DB là lớp chặn cuối.
            // (Two simultaneous requests passed the checks above; the DB is the last guard.)
            throw new ApiException(HttpStatus.CONFLICT, "GROUP_CONFLICT",
                    "The group changed at the same time. Reload and try again!");
        }
        groupSync.createFutureWorkouts(member);
    }

    private void assertInTrainerArea(Horse horse, HorseStallInfo stall, Long trainerId) {
        if (stall == null) {
            throw new ApiException(HttpStatus.CONFLICT, "HORSE_NO_STALL", String.format(
                    "Horse '%s' has not been assigned to a stall, so it cannot join a group!", horse.getName()));
        }
        if (stall.areaTrainerId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "AREA_NO_TRAINER", String.format(
                    "Area '%s' is not assigned to any trainer. Contact the club manager to assign the area!", stall.areaCode()));
        }
        if (!stall.areaTrainerId().equals(trainerId)) {
            throw new AccessDeniedException(String.format(
                    "Horse '%s' is in area '%s', which you do not manage!", horse.getName(), stall.areaCode()));
        }
    }

    /** Lý do một chiến mã chưa vào được nhóm, hoặc null nếu vào được. (Why a horse cannot join the group yet, or null if it can.) */
    private String candidateReason(Horse horse, HorseStallInfo stall, TrainingGroupMember membership, int groupSize,
                                   Map<Long, Long> memberByGroom, Map<Long, String> horseNamesInGroup) {
        if (!horse.canTrain()) {
            return "Not allowed to train";
        }
        if (stall == null || stall.stallGroomId() == null) {
            return "Its stall has no groom";
        }
        if (membership != null) {
            return "Already in another group";
        }
        if (groupSize >= TrainingLimits.MAX_HORSES_PER_GROUP) {
            return "The group is full";
        }
        Long sameGroomHorse = memberByGroom.get(stall.stallGroomId());
        if (sameGroomHorse != null) {
            return "Same groom as " + horseNamesInGroup.get(sameGroomHorse);
        }
        return null;
    }

    // =================================================================
    // TIỆN ÍCH (Helpers)
    // =================================================================

    /** Nhóm của chính Trainer này; nhóm của người khác báo "không tìm thấy" để không lộ nó tồn tại. */
    private TrainingGroup findLiveOwnGroup(Long groupId, AuthenticatedUser currentUser) {
        TrainingGroup group = findOwnGroup(groupId, currentUser);
        if (group.isDisbanded()) {
            throw new ApiException(HttpStatus.CONFLICT, "GROUP_DISBANDED", "Group '" + group.getName() + "' is disbanded!");
        }
        return group;
    }

    private TrainingGroup findOwnGroup(Long groupId, AuthenticatedUser currentUser) {
        return groupRepository.findById(groupId)
                .filter(g -> g.getTrainerId().equals(currentUser.getUserId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group not found!"));
    }

    private String normalizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Group name must not be empty!");
        }
        if (trimmed.length() > TrainingLimits.GROUP_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Group name must be at most " + TrainingLimits.GROUP_NAME_MAX_LENGTH + " characters!");
        }
        return trimmed;
    }

    private void assertNameFree(Long trainerId, String name) {
        if (groupRepository.existsByTrainerIdAndNameIgnoreCaseAndDisbandedAtIsNull(trainerId, name)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_GROUP_NAME",
                    "You already have a group named '" + name + "'!");
        }
    }

    private Map<Long, HorseStallInfo> stallInfo(Collection<Long> horseIds) {
        if (horseIds.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findStallInfoByHorseIds(horseIds).stream()
                .collect(Collectors.toMap(HorseStallInfo::horseId, Function.identity()));
    }

    private Map<Long, String> horseNames(Collection<Long> horseIds) {
        return horseRepository.findAllById(horseIds).stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName));
    }

    private Map<Long, String> userNames(Collection<Long> userIds) {
        Set<Long> ids = userIds.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, User::getFullName));
    }

    private TrainingGroupResponse toResponse(TrainingGroup group) {
        return toResponses(List.of(group)).get(0);
    }

    /** Dựng phản hồi cho nhiều nhóm bằng một số truy vấn cố định, không phụ thuộc số nhóm. (Builds responses with a fixed number of queries.) */
    private List<TrainingGroupResponse> toResponses(List<TrainingGroup> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }
        List<Long> groupIds = groups.stream().map(TrainingGroup::getId).toList();
        List<TrainingGroupMember> allMembers = memberRepository.findByGroupIdIn(groupIds);
        Map<Long, String> horseNames = horseNames(allMembers.stream().map(TrainingGroupMember::getHorseId).toList());
        Map<Long, String> groomNames = userNames(allMembers.stream().map(TrainingGroupMember::getGroomId).toList());
        Map<Long, List<TrainingGroupMember>> membersByGroup = allMembers.stream()
                .sorted(Comparator.comparing(TrainingGroupMember::getJoinedAt).thenComparing(TrainingGroupMember::getId))
                .collect(Collectors.groupingBy(TrainingGroupMember::getGroupId));

        Map<Long, GroupPlan> currentPlanByGroup = currentPlans(groupIds);
        Map<Long, String> courseNames = courseRepository.findAllById(
                        currentPlanByGroup.values().stream().map(GroupPlan::getCourseId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Course::getId, Course::getName));

        List<TrainingGroupResponse> result = new ArrayList<>();
        for (TrainingGroup group : groups) {
            List<GroupMemberResponse> memberResponses = membersByGroup.getOrDefault(group.getId(), List.of()).stream()
                    .map(m -> new GroupMemberResponse(m.getHorseId(), horseNames.get(m.getHorseId()), m.getGroomId(),
                            groomNames.get(m.getGroomId()), m.getJoinedAt()))
                    .toList();
            GroupPlan plan = currentPlanByGroup.get(group.getId());
            result.add(new TrainingGroupResponse(group.getId(), group.getName(), group.getTrainerId(), memberResponses,
                    plan == null ? null : courseNames.get(plan.getCourseId()),
                    plan == null ? null : plan.getStatus().name(),
                    plan == null ? null : plan.getEndDate(), group.isDisbanded()));
        }
        return result;
    }

    /** Khóa đang chạy của mỗi nhóm, hoặc khóa sắp chạy sớm nhất nếu chưa có khóa nào chạy. (The running course of each group, else the earliest upcoming one.) */
    private Map<Long, GroupPlan> currentPlans(Collection<Long> groupIds) {
        return planRepository.findByGroupIdInAndStatusIn(groupIds, UNFINISHED).stream()
                .collect(Collectors.toMap(GroupPlan::getGroupId, Function.identity(), (a, b) -> {
                    if (a.getStatus() != b.getStatus()) {
                        return a.getStatus() == TrainingPlanStatus.ACTIVE ? a : b;
                    }
                    return a.getStartDate().isBefore(b.getStartDate()) ? a : b;
                }));
    }
}
