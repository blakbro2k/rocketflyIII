package com.asg.rocketflyiii.access;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AccessController {
    private final AccessStore store;
    private final ResourceDiscovery discovery;
    private final Path tokenFile;

    public AccessController(
            AccessStore store,
            ResourceDiscovery discovery,
            @Value("${rocketfly.data-dir}") String dir) {
        this.store = store;
        this.discovery = discovery;
        tokenFile = Path.of(dir, "setup-token");
    }

    public record Setup(String token, String username, String password) {}

    public record Create(String username, String password, long roleId) {}

    public record Assignment(long roleId) {}

    @GetMapping("/api/csrf")
    public Map<String, String> csrf(CsrfToken csrf) {
        return Map.of("header", csrf.getHeaderName(), "token", csrf.getToken());
    }

    @PostMapping("/api/setup")
    public synchronized void setup(@RequestBody Setup input) throws java.io.IOException {
        if (input.token() == null
                || !Files.exists(tokenFile)
                || Files.getLastModifiedTime(tokenFile)
                        .toInstant()
                        .plus(Duration.ofMinutes(15))
                        .isBefore(Instant.now())
                || !MessageDigest.isEqual(
                        Files.readString(tokenFile).strip().getBytes(StandardCharsets.UTF_8),
                        input.token().getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Files.delete(tokenFile);
        store.bootstrap(input.username(), input.password());
    }

    @GetMapping("/api/me")
    public AccessStore.Profile current(@RequestAttribute AccessStore.Profile profile) {
        return profile;
    }

    @GetMapping("/api/admin/profiles")
    public List<AccessStore.Profile> profiles() {
        return store.profiles();
    }

    @PostMapping("/api/admin/profiles")
    public void create(@RequestBody Create p) {
        store.create(p.username(), p.password(), p.roleId());
    }

    @PutMapping("/api/admin/profiles/{name}/role")
    public void assign(@PathVariable String name, @RequestBody Assignment a) {
        store.assign(name, a.roleId());
    }

    @GetMapping("/api/admin/roles")
    public List<AccessStore.Role> roles() {
        return store.roles();
    }

    @PostMapping("/api/admin/roles")
    public void createRole(@RequestBody AccessStore.RoleInput role) {
        store.saveRole(null, role);
    }

    @PutMapping("/api/admin/roles/{id}")
    public void editRole(@PathVariable long id, @RequestBody AccessStore.RoleInput role) {
        store.saveRole(id, role);
    }

    @DeleteMapping("/api/admin/roles/{id}")
    public void deleteRole(@PathVariable long id) {
        store.deleteRole(id);
    }

    @GetMapping("/api/admin/resources")
    public List<ResourceDiscovery.Resource> discover() {
        return discovery.discover();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void conflict() {}
}
