package com.rtms.backend.identity.service;

import com.rtms.backend.identity.dto.*;
import com.rtms.backend.identity.repository.*;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.repository.*;
import com.rtms.backend.stable.service.AreaTrainerAssignmentPolicy;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** Commits test fixtures only in an explicitly named disposable local database. */
@SpringBootTest(properties={"spring.datasource.url=${RTMS_STAFF_TEST_DB_URL}",
    "spring.datasource.username=rtms_user","spring.datasource.password=rtms_password",
    "spring.config.import=","rtms.storage.cloudinary-url=","spring.jpa.show-sql=false"})
@EnabledIfEnvironmentVariable(named="RTMS_STAFF_TEST_DB_URL",matches="jdbc:postgresql://localhost:5433/rtms_staff_test_[a-zA-Z0-9_]+")
class StaffCreationPostgresTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired StaffManagementService service;
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder encoder;
    @Autowired GroomProfileRepository grooms;
    @Autowired VeterinarianProfileRepository vets;
    @Autowired TrainerProfileRepository trainers;
    @Autowired AreaRepository areas;
    @Autowired StableStallRepository stalls;
    @Autowired AreaTrainerAssignmentPolicy policy;
    @Autowired EntityManager em;
    @Autowired Validator validator;
    @Autowired PlatformTransactionManager transactions;
    @Autowired WebApplicationContext web;
    String tag;

    @BeforeEach void isolate() {
        assertTrue(System.getenv("RTMS_STAFF_TEST_DB_URL").contains("/rtms_staff_test_"));
        jdbc.update("UPDATE stable_stalls SET deleted_at=now() WHERE deleted_at IS NULL");
        jdbc.update("UPDATE areas SET deleted_at=now() WHERE deleted_at IS NULL");
        jdbc.update("UPDATE users SET is_active=false WHERE role_id=(SELECT id FROM roles WHERE name='HEAD_TRAINER')");
        tag=UUID.randomUUID().toString().replace("-", "").substring(0,10);
    }
    long employee(String role,String suffix) {
        long id=jdbc.queryForObject("INSERT INTO users(full_name,email,password_hash,role_id,is_active) SELECT ?,?,'unused',id,true FROM roles WHERE name=? RETURNING id",
                Long.class,"Test "+suffix,tag+suffix+"@test.invalid",role);
        if (role.equals("HEAD_TRAINER")) jdbc.update("INSERT INTO trainer_profiles(user_id,certification_number) VALUES (?,'TEST')",id);
        if (role.equals("GROOM")) jdbc.update("INSERT INTO groom_profiles(user_id) VALUES (?)",id);
        return id;
    }
    long area(String code,String type,Long trainer) {
        // Old rows stay referenced by historical fixtures; release their codes only in this disposable DB.
        jdbc.update("UPDATE areas SET code='OLD'||id WHERE code=?",code);
        long id=jdbc.queryForObject("INSERT INTO areas(code,name,type,trainer_id) VALUES (?,?,?,?) RETURNING id",Long.class,code,code,type,trainer);
        jdbc.update("INSERT INTO stable_stalls(area_id,stall_number,stall_code,status) SELECT ?,n,?||n,'AVAILABLE' FROM generate_series(1,12) n",id,tag+code);
        return id;
    }
    StaffCreationRequest request(String role,String suffix) {
        var req=new StaffCreationRequest();req.setRole(role);req.setFullName("Staff "+suffix);
        req.setEmail(tag+suffix+"@test.invalid");req.setPassword("Test@2026");
        if(role.equals("HEAD_TRAINER"))req.setCertificationNumber("TR-"+tag);
        if(role.equals("VETERINARIAN"))req.setLicenseNumber("VT-"+tag);
        return req;
    }
    void fill(long area,int from,int to) {
        for(int block=from;block<=to;block++) {
            long id=employee("GROOM","existing"+block);
            jdbc.update("UPDATE groom_profiles SET trainer_id=(SELECT trainer_id FROM areas WHERE id=?) WHERE user_id=?",area,id);
            jdbc.update("UPDATE stable_stalls SET groom_id=? WHERE area_id=? AND stall_number BETWEEN ? AND ?",id,area,(block-1)*3+1,block*3);
        }
    }
    List<StaffCreationResponse> concurrent(String role) throws Exception {
        var barrier=new CyclicBarrier(2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var one=executor.submit(()->{barrier.await(10,TimeUnit.SECONDS);return service.createStaff(request(role,"one"));});
            var two=executor.submit(()->{barrier.await(10,TimeUnit.SECONDS);return service.createStaff(request(role,"two"));});
            return List.of(one.get(25,TimeUnit.SECONDS),two.get(25,TimeUnit.SECONDS));
        }
    }

    @Test void simultaneousGroomsCannotReplaceEachOtherWhenOneBlockRemains() throws Exception {
        long trainer=employee("HEAD_TRAINER","trainer"),a=area("A","REGULAR",trainer);fill(a,2,4);
        var results=concurrent("GROOM");
        assertEquals(1,results.stream().filter(r->"ASSIGNED".equals(r.getAssignmentStatus())).count());
        assertEquals(1,results.stream().filter(r->"UNASSIGNED".equals(r.getAssignmentStatus())).count());
        for(var result:results) assertEquals("ASSIGNED".equals(result.getAssignmentStatus())?3:0,
                jdbc.queryForObject("SELECT count(*) FROM stable_stalls WHERE groom_id=?",Integer.class,result.getUserId()));
    }
    @Test void simultaneousGroomsUseDifferentAvailableBlocks() throws Exception {
        long trainer=employee("HEAD_TRAINER","trainer"),a=area("A","REGULAR",trainer);fill(a,3,4);
        var results=concurrent("GROOM");
        assertTrue(results.stream().allMatch(r->"ASSIGNED".equals(r.getAssignmentStatus())));
        assertTrue(Collections.disjoint(results.get(0).getAssignedStallIds(),results.get(1).getAssignedStallIds()));
    }
    @Test void simultaneousTrainersReceiveDisjointAreasInAlphabeticSequence() throws Exception {
        for(String code:List.of("A","B","Z","AA"))area(code,"REGULAR",null);
        var results=concurrent("HEAD_TRAINER");
        var codes=results.stream().map(StaffCreationResponse::getAssignedAreaCodes).toList();
        assertTrue(codes.contains(List.of("A","B")));assertTrue(codes.contains(List.of("Z","AA")));
    }
    @Test void quarantineNeedsNoTrainerAndFourGroomsFillExactlyTwelveStalls() {
        long q=area("Q-A","QUARANTINE",null);
        for(int i=0;i<4;i++) {
            var result=service.createStaff(request("GROOM","g"+i));
            assertEquals("ASSIGNED",result.getAssignmentStatus());assertEquals(q,result.getAssignedAreaId());
            assertNull(result.getTrainerId());assertNull(result.getTrainerName());
            assertEquals("QUARANTINE",result.getAssignedAreaType().name());
            assertEquals(3,result.getAssignedStallIds().size());
        }
        var extra=service.createStaff(request("GROOM","extra"));
        assertEquals("UNASSIGNED",extra.getAssignmentStatus());assertTrue(extra.getAssignedStallIds().isEmpty());
        assertEquals(4,jdbc.queryForObject("SELECT count(DISTINCT groom_id) FROM stable_stalls WHERE area_id=?",Integer.class,q));
    }
    @Test void occupiedQuarantineBlockTakesPriorityOverEmptyRegularBlocks() {
        long trainer=employee("HEAD_TRAINER","trainer");area("A","REGULAR",trainer);long q=area("Q-A","QUARANTINE",null);
        jdbc.update("INSERT INTO horses(name,current_status,current_stall_id) SELECT 'Q horse','CANDIDATE',id FROM stable_stalls WHERE area_id=? AND stall_number=4",q);
        var result=service.createStaff(request("GROOM","new"));
        assertEquals(q,result.getAssignedAreaId());assertNull(result.getTrainerId());
        assertEquals(List.of(4,5,6),jdbc.queryForList("SELECT stall_number FROM stable_stalls WHERE groom_id=? ORDER BY stall_number",Integer.class,result.getUserId()));
    }
    @Test void groomAllocationBalancesRegularAndQuarantineGroups() {
        long trainer=employee("HEAD_TRAINER","trainer"),a=area("A","REGULAR",trainer),q=area("Q-A","QUARANTINE",null);
        var one=service.createStaff(request("GROOM","one"));var two=service.createStaff(request("GROOM","two"));
        assertEquals(Set.of(a,q),Set.of(one.getAssignedAreaId(),two.getAssignedAreaId()));
    }
    @Test void groomAreaOrderKeepsZBeforeAA() {
        long trainer=employee("HEAD_TRAINER","trainer");long z=area("Z","REGULAR",trainer);area("AA","REGULAR",trainer);
        assertEquals(z,service.createStaff(request("GROOM","new")).getAssignedAreaId());
    }
    @Test void staffAndPartialAssignmentsRollBackTogetherAfterWriteFailure() {
        long trainer=employee("HEAD_TRAINER","trainer");area("A","REGULAR",trainer);
        var wrapped=mock(StableStallRepository.class,delegatesTo(stalls));var saved=new AtomicInteger();
        doAnswer(invocation->{var result=stalls.save(invocation.getArgument(0));em.flush();
            if(saved.incrementAndGet()==2)throw new IllegalStateException("Injected write failure");return result;
        }).when(wrapped).save(any());
        var independent=new StaffManagementService(users,roles,encoder,grooms,vets,trainers,areas,wrapped,policy,em,validator);
        var req=request("GROOM","rollback");
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).execute(tx->independent.createStaff(req)));
        assertEquals(2,saved.get());assertTrue(users.findByEmail(req.getEmail()).isEmpty());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM stable_stalls s JOIN areas a ON s.area_id=a.id WHERE a.deleted_at IS NULL AND s.groom_id IS NOT NULL",Integer.class));
    }
    @Test void duplicateEmailRaceReturnsConflictRatherThanDatabaseError() throws Exception {
        var start=new CyclicBarrier(2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var futures=new ArrayList<Future<Integer>>();
            for(int i=0;i<2;i++)futures.add(executor.submit(()->{start.await();try{service.createStaff(request("VETERINARIAN","same"));return 201;}catch(ResponseStatusException ex){return ex.getStatusCode().value();}}));
            var statuses=new ArrayList<>(List.of(futures.get(0).get(25,TimeUnit.SECONDS),futures.get(1).get(25,TimeUnit.SECONDS)));Collections.sort(statuses);
            assertEquals(List.of(201,409),statuses);
        }
    }
    @ParameterizedTest @ValueSource(strings={"{\"email\":\"invalid\"}","{\"phone\":\"123456789012345678901\"}","{\"fullName\":\" \"}","{\"password\":\" \"}","{\"role\":\"HEAD_TRAINER\"}","{\"licenseIssuedDate\":\"2999-01-01\"}"})
    void invalidApiInputNeverCreatesStaff(String override) throws Exception {
        var mapper=new ObjectMapper();
        var body=mapper.createObjectNode().put("role","VETERINARIAN").put("fullName","Test").put("email",tag+"@test.invalid").put("password","Test@2026").put("licenseNumber","VT-TEST");
        mapper.readTree(override).properties().forEach(entry->body.set(entry.getKey(),entry.getValue()));
        var actor=new UsernamePasswordAuthenticationToken(new AuthenticatedUser(1L,"manager@test.invalid","CLUB_MANAGER"),null,List.of(new SimpleGrantedAuthority("USER_MANAGE")));
        long before=jdbc.queryForObject("SELECT count(*) FROM users",Long.class);
        webAppContextSetup(web).apply(springSecurity()).build().perform(post("/api/staff").with(authentication(actor)).contentType("application/json").content(body.toString()))
                .andExpect(status().isBadRequest());
        assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM users",Long.class));
    }
    @Test void validApiCreatesOnlyTheRequestedProfileAndChecksPermissions() throws Exception {
        var mvc=webAppContextSetup(web).apply(springSecurity()).build();
        String body="{\"role\":\"VETERINARIAN\",\"fullName\":\" New Vet \",\"email\":\" "+tag.toUpperCase()+"@TEST.INVALID \",\"password\":\"Test@2026\",\"phone\":\"12345678901234567890\",\"licenseNumber\":\"VT-1\"}";
        mvc.perform(post("/api/staff").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        var forbidden=new UsernamePasswordAuthenticationToken(new AuthenticatedUser(1L,"trainer@test.invalid","HEAD_TRAINER"),null,List.of());
        mvc.perform(post("/api/staff").with(authentication(forbidden)).contentType("application/json").content(body)).andExpect(status().isForbidden());
        var actor=new UsernamePasswordAuthenticationToken(new AuthenticatedUser(1L,"manager@test.invalid","CLUB_MANAGER"),null,List.of(new SimpleGrantedAuthority("USER_MANAGE")));
        mvc.perform(post("/api/staff").with(authentication(actor)).contentType("application/json").content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fullName").value("New Vet")).andExpect(jsonPath("$.data.email").value(tag+"@test.invalid"));
        long id=users.findByEmail(tag+"@test.invalid").orElseThrow().getId();
        assertTrue(vets.existsById(id));assertFalse(grooms.existsById(id));assertFalse(trainers.existsById(id));
        var login=new LoginRequest();login.setEmail(tag+"@test.invalid");login.setPassword("Test@2026");
        assertNotNull(auth.login(login).token());
    }
}
