package minesweeper.repository;

import minesweeper.model.EmailVerificationToken;
import minesweeper.repository.exception.DataAccessException;

public interface EmailVerificationRepository {

    /** Lưu token mới (xoá token cũ của userId cùng lúc). */
    void save(EmailVerificationToken token) throws DataAccessException;

    /** Lấy token chưa dùng, chưa hết hạn của userId. */
    EmailVerificationToken findActiveByUserId(long userId) throws DataAccessException;

    /** Đánh dấu token đã dùng. */
    void markUsed(long tokenId) throws DataAccessException;

    /** Xoá tất cả token của userId (dọn dẹp sau xác nhận). */
    void deleteByUserId(long userId) throws DataAccessException;
}
