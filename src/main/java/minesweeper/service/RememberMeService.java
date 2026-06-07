package minesweeper.service;

import minesweeper.model.User;
import minesweeper.repository.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import utils.CryptUtils;
public class RememberMeService {

    private static final Logger LOG = LoggerFactory.getLogger(RememberMeService.class);

    private static final Path TOKEN_FILE = Path.of(
            System.getProperty("user.home"), ".minesweeper", "remember_me.dat");

    private final MySqlUserService userService;

    public RememberMeService() {
        this(new MySqlUserService());
    }

    public RememberMeService(MySqlUserService userService) {
        this.userService = userService;
    }

    public void save(User user) throws DataAccessException {
        String token = UUID.randomUUID().toString();
        userService.updateRememberToken(user.getId(), token);
        writeTokenFile(user.getUsername(), token);
        LOG.info("Remember-me token saved for user={}", user.getUsername());
    }

    public User tryAutoLogin() {
        String[] parts = readTokenFile();
        if (parts == null) return null;

        String username = parts[0];
        String token    = parts[1];

        try {
            User user = userService.getAuthUserByUsername(username);
            if (user == null || !user.isActive()) {
                clear();
                return null;
            }
            String storedToken = userService.getRememberToken(user.getId());
            if (storedToken == null || !storedToken.equals(token)) {
                clear();
                return null;
            }

            userService.updateLastLogin(user.getId());
            SessionManager.createSession(user);
            LOG.info("Auto-login success for user={}", username);
            return user;
        } catch (DataAccessException e) {
            LOG.warn("Auto-login failed for user={}", username, e);
            clear();
            return null;
        }
    }


    public void clear() {
        try {
            Files.deleteIfExists(TOKEN_FILE);
        } catch (IOException e) {
            LOG.warn("Could not delete remember-me file", e);
        }

        User current = SessionManager.getCurrentUser();
        if (current != null) {
            try {
                userService.updateRememberToken(current.getId(), null);
            } catch (DataAccessException e) {
                LOG.warn("Could not clear remember-me token in DB", e);
            }
        }
    }

    private void writeTokenFile(String username, String token) {
        try {
            Files.createDirectories(TOKEN_FILE.getParent());
            String plainText = username + "\n" + token;
            String encryptedText = CryptUtils.encryptAES(plainText);
            Files.writeString(TOKEN_FILE, encryptedText,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOG.error("Failed to write remember-me token file", e);
        }
    }
    private String[] readTokenFile() {
        if (!Files.exists(TOKEN_FILE)) return null;
        try {
            String encryptedContent = Files.readString(TOKEN_FILE).trim();
            String decryptedContent = CryptUtils.decryptAES(encryptedContent);
            
            if (decryptedContent == null) {
                // Có thể là file cũ dạng plain-text hoặc bị lỗi. Cứ thử coi như plain-text xem sao.
                decryptedContent = encryptedContent; 
            }

            String[] lines = decryptedContent.split("\n", 2);
            if (lines.length == 2 && !lines[0].isBlank() && !lines[1].isBlank()) {
                return new String[]{lines[0].trim(), lines[1].trim()};
            }
        } catch (IOException e) {
            LOG.warn("Failed to read remember-me token file", e);
        }
        return null;
    }
}