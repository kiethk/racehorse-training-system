package com.rtms.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mọi quyền mà một @PreAuthorize đòi phải có trong bảng permissions. Đổi tên một quyền ở controller mà quên migration (hoặc ngược lại)
 * làm mọi lời gọi trả 403 một cách âm thầm; test này bắt lỗi đó ngay.
 * (Every authority a @PreAuthorize asks for must exist in the permissions table. Renaming an authority in a controller without the
 * migration (or the other way round) makes every call return 403 silently; this test catches it at once.)
 *
 * Chỉ kiểm các controller của module training. Quyền dạng ROLE_ là vai trò cấp cho người dùng, không nằm trong bảng permissions.
 * (Only the training module's controllers are checked. A ROLE_ name is a role granted to the user, not a row in permissions.)
 */
@SpringBootTest
class PreAuthorizeAuthoritiesExistTest {

    private static final Pattern AUTHORITY = Pattern.compile("hasAuthority\\('([A-Z0-9_]+)'\\)|hasAnyAuthority\\(([^)]*)\\)");
    private static final Pattern QUOTED = Pattern.compile("'([A-Z0-9_]+)'");

    @Autowired private ApplicationContext context;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void everyAuthorityUsedByAControllerExistsInThePermissionsTable() {
        Set<String> known = new HashSet<>(jdbc.queryForList("SELECT code FROM permissions", String.class));
        Set<String> missing = new TreeSet<>();

        for (Object controller : context.getBeansWithAnnotation(RestController.class).values()) {
            Class<?> type = AopUtils.getTargetClass(controller);
            if (!type.getPackageName().startsWith("com.rtms.backend.training")) {
                continue;
            }
            for (String authority : authoritiesOf(type)) {
                if (!authority.startsWith("ROLE_") && !known.contains(authority)) {
                    missing.add(type.getSimpleName() + " -> " + authority);
                }
            }
        }
        assertTrue(missing.isEmpty(), "Authorities used by a controller but missing from permissions: " + missing);
    }

    private static List<String> authoritiesOf(Class<?> type) {
        List<String> found = new ArrayList<>();
        collect(type.getAnnotation(PreAuthorize.class), found);
        for (Method method : type.getDeclaredMethods()) {
            collect(method.getAnnotation(PreAuthorize.class), found);
        }
        return found;
    }

    private static void collect(PreAuthorize annotation, List<String> into) {
        if (annotation == null) {
            return;
        }
        Matcher matcher = AUTHORITY.matcher(annotation.value());
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                into.add(matcher.group(1));
            } else {
                Matcher inner = QUOTED.matcher(matcher.group(2));
                while (inner.find()) {
                    into.add(inner.group(1));
                }
            }
        }
    }
}
