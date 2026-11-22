import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.HexFormat;

// Run locally on the installation host, or through docker compose exec.
class SetupToken {
    public static void main(String[] args) throws Exception {
        Path dir = Path.of(System.getenv().getOrDefault("ROCKETFLY_DATA_DIR", "./rocketfly-data"));
        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, "setup-", ".tmp");
        try {
            if (Files.getFileStore(dir).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rw-------"));
            }
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            String token = HexFormat.of().formatHex(bytes);
            Files.writeString(temp, token);
            Files.move(temp, dir.resolve("setup-token"), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            System.out.println("Open /setup.html within 15 minutes. Your one-time token is " + token);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
