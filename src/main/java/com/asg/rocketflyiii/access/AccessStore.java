package com.asg.rocketflyiii.access;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessStore implements UserDetailsService {
    public record Profile(String username, long roleId, String roleName, long version) {
        public boolean admin() {
            return roleId == 1;
        }
    }

    public record Grant(String kind, String id) {}

    public record Role(long id, String name, List<Grant> grants) {}

    public record RoleInput(String name, Set<Grant> grants) {}

    private final JdbcClient db;
    private final PasswordEncoder encoder;

    public AccessStore(JdbcClient db, PasswordEncoder encoder) {
        this.db = db;
        this.encoder = encoder;
    }

    public boolean needsSetup() {
        return db.sql("select count(*) from rf_profile").query(Long.class).single() == 0;
    }

    @Override
    public UserDetails loadUserByUsername(String name) {
        return db.sql("select * from rf_profile where username=?")
                .param(normalize(name))
                .query(
                        (r, n) ->
                                User.withUsername(r.getString("username"))
                                        .password(r.getString("password_hash"))
                                        .authorities("version:" + r.getLong("credential_version"))
                                        .build())
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("Unknown profile"));
    }

    private static String normalize(String name) {
        return name == null ? "" : name.strip().toLowerCase(Locale.ROOT);
    }

    public List<Profile> profiles() {
        return db.sql(
                        "select p.username, p.role_id, r.name role_name, p.credential_version version from rf_profile p join rf_role r on r.id=p.role_id order by p.username")
                .query(Profile.class)
                .list();
    }

    public Profile profile(String username) {
        return db.sql(
                        "select p.username, p.role_id, r.name role_name, p.credential_version version from rf_profile p join rf_role r on r.id=p.role_id where p.username=?")
                .param(username)
                .query(Profile.class)
                .single();
    }

    public List<Grant> grants(long role) {
        return db.sql(
                        "select kind, resource_id id from rf_grant where role_id=? order by kind,resource_id")
                .param(role)
                .query(Grant.class)
                .list();
    }

    @Transactional(readOnly = true)
    public List<Role> roles() {
        return db.sql("select id,name from rf_role order by name")
                .query((r, n) -> new Role(r.getLong(1), r.getString(2), grants(r.getLong(1))))
                .list();
    }

    private String hash(String password) {
        if (password == null
                || password.length() < 12
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw bad("Use a password of at least 12 characters and at most 72 UTF-8 bytes.");
        return encoder.encode(password);
    }

    public void create(String username, String password, long role) {
        String name = normalize(username);
        if (!name.matches("[a-z0-9][a-z0-9._-]{0,63}")) throw bad("Choose a valid username.");
        db.sql("insert into rf_profile(username,password_hash,role_id) values (?,?,?)")
                .params(name, hash(password), role)
                .update();
    }

    @Transactional
    public void assign(String username, long role) {
        lockAdmin();
        var user = profile(username);
        if (user.admin()
                && role != 1
                && db.sql("select count(*) from rf_profile where role_id=1")
                                .query(Long.class)
                                .single()
                        == 1) throw bad("Keep at least one System Admin.");
        db.sql("update rf_profile set role_id=? where username=?").params(role, username).update();
    }

    private void lockAdmin() {
        db.sql("select id from rf_role where id=1 for update").query(Long.class).single();
    }

    @Transactional
    public void bootstrap(String username, String password) {
        lockAdmin();
        if (needsSetup()) create(username, password, 1);
        else if (db.sql(
                                "update rf_profile set password_hash=?, credential_version=credential_version+1 where username=? and role_id=1")
                        .params(hash(password), normalize(username))
                        .update()
                != 1) throw bad("Enter an existing System Admin username.");
    }

    @Transactional
    public void saveRole(Long id, RoleInput input) {
        if (input.name() == null
                || input.name().isBlank()
                || input.name().strip().length() > 80
                || input.grants() == null) throw bad("Enter a role name and grants.");
        String name = input.name().strip();
        if (id == null) {
            db.sql("insert into rf_role(name) values (?)").param(name).update();
            id =
                    db.sql("select id from rf_role where name=?")
                            .param(name)
                            .query(Long.class)
                            .single();
        } else {
            if (id == 1
                    || db.sql("update rf_role set name=? where id=?").params(name, id).update()
                            != 1) throw bad("Choose a custom role.");
            db.sql("delete from rf_grant where role_id=?").param(id).update();
        }
        for (var grant : input.grants()) {
            if (grant == null
                    || !("account".equals(grant.kind()) || "budget".equals(grant.kind()))
                    || grant.id() == null
                    || !grant.id().matches("[1-9][0-9]{0,18}"))
                throw bad("Invalid resource grant.");
            db.sql("insert into rf_grant values (?,?,?)")
                    .params(id, grant.kind(), grant.id())
                    .update();
        }
    }

    public void deleteRole(long id) {
        if (id == 1) throw bad("The System Admin role is built in.");
        db.sql("delete from rf_role where id=?").param(id).update();
    }

    static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
