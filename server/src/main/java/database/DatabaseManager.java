package database;

import util.PgPassReader;
import data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String URL = "jdbc:postgresql://localhost:5432/studs";
    private static final String HOST = "localhost";
    private static final int PORT = 5432;
    private static final String DATABASE = "studs";
    private static final String[] CREDS = PgPassReader.loadCredentials(HOST, PORT, DATABASE);
    private static final String USER = CREDS[0].isEmpty() ? System.getProperty("user.name") : CREDS[0];
    private static final String PASS = CREDS[1];

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /**
     * Проверяет логин и хэш пароля. Возвращает ID пользователя если успешно, или -1 если нет.
     */
    public int validateUser(String login, String hash) {
        if (logger.isDebugEnabled()) {
            logger.debug("Проверка авторизации: login='{}'", login);
        }

        String sql = "SELECT id FROM users WHERE login=? AND password_hash=?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, hash);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                if (logger.isDebugEnabled()) {
                    logger.debug("Пользователь '{}' авторизован, ID: {}", login, id);
                }
                return id;
            } else {
                logger.warn("Неудачная авторизация для пользователя '{}': не найден или неверный пароль", login);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при проверке пользователя '{}': {}", login, e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Регистрирует нового пользователя.
     */
    public boolean registerUser(String login, String hash) {
        String sql = "INSERT INTO users (login, password_hash) VALUES (?, ?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, hash);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Register error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Загружает все лабораторные работы из БД.
     */
    public List<LabWork> loadAllLabWorks() {
        List<LabWork> list = new LinkedList<>();
        String sql = "SELECT * FROM lab_works ORDER BY id";
        try (Connection c = getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            logger.info("Loaded {} items from DB", list.size());
        } catch (SQLException e) {
            logger.error("DB load error: {}", e.getMessage());
        }
        return list;
    }

    /**
     * Добавляет новую лабораторную работу.
     */
    public boolean addLabWork(LabWork lw, int ownerId) {
        String sql = "INSERT INTO lab_works (id, name, coordinates_x, coordinates_y, " +
                "minimal_point, personal_qualities_maximum, difficulty, " +
                "discipline_name, lecture_hours, owner_id, creation_date) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, lw.getId());
            ps.setString(2, lw.getName());
            ps.setDouble(3, lw.getCoordinates().getX());
            ps.setLong(4, lw.getCoordinates().getY());
            ps.setFloat(5, lw.getMinimalPoint());
            ps.setDouble(6, lw.getPersonalQualitiesMaximum());
            ps.setString(7, lw.getDifficulty() != null ? lw.getDifficulty().name() : null);
            ps.setString(8, lw.getDiscipline() != null ? lw.getDiscipline().getName() : null);
            ps.setInt(9, lw.getDiscipline() != null ? lw.getDiscipline().getLectureHours() : 0);
            ps.setInt(10, ownerId);
            ps.setTimestamp(11, new Timestamp(lw.getCreationDate().getTime()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("DB add error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Обновляет существующую лабораторную работу.
     */
    public boolean updateLabWork(LabWork lw, int ownerId) {
        String sql = "UPDATE lab_works SET name=?, coordinates_x=?, coordinates_y=?, " +
                "minimal_point=?, personal_qualities_maximum=?, difficulty=?, " +
                "discipline_name=?, lecture_hours=? WHERE id=? AND owner_id=?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, lw.getName());
            ps.setDouble(2, lw.getCoordinates().getX());
            ps.setLong(3, lw.getCoordinates().getY());
            ps.setFloat(4, lw.getMinimalPoint());
            ps.setDouble(5, lw.getPersonalQualitiesMaximum());
            ps.setString(6, lw.getDifficulty() != null ? lw.getDifficulty().name() : null);
            ps.setString(7, lw.getDiscipline() != null ? lw.getDiscipline().getName() : null);
            ps.setInt(8, lw.getDiscipline() != null ? lw.getDiscipline().getLectureHours() : 0);
            ps.setLong(9, lw.getId());
            ps.setInt(10, ownerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("DB update error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Удаляет лабораторную работу по ID.
     */
    public boolean removeLabWork(Long id, int ownerId) {
        String sql = "DELETE FROM lab_works WHERE id=? AND owner_id=?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setInt(2, ownerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("DB remove error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Очищает все работы конкретного пользователя.
     */
    public int clearUserLabWorks(int ownerId) {
        String sql = "DELETE FROM lab_works WHERE owner_id=?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("DB clear error: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Получает следующий свободный ID из последовательности.
     */
    public long getNextId() {
        try (Connection c = getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT nextval('lab_work_seq')")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            logger.error("Sequence error: {}", e.getMessage());
        }
        return System.currentTimeMillis() % 100000;
    }

    /**
     * Получает ID владельца объекта по его ID.
     */
    public Optional<Integer> getOwnerById(Long id) {
        String sql = "SELECT owner_id FROM lab_works WHERE id=?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(rs.getInt("owner_id"));
        } catch (SQLException e) {
            logger.error("Owner check error: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Проверяет существование объекта по ID.
     */
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM lab_works WHERE id=?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            logger.error("Exists check error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Маппит строку ResultSet в объект LabWork.
     */
    private LabWork mapRow(ResultSet rs) throws SQLException {
        Coordinates coord = new Coordinates(
                rs.getDouble("coordinates_x"),
                rs.getLong("coordinates_y")
        );

        Discipline disc = null;
        String dName = rs.getString("discipline_name");
        if (dName != null && !dName.isEmpty()) {
            disc = new Discipline(dName, rs.getInt("lecture_hours"));
        }

        Difficulty diff = null;
        String diffStr = rs.getString("difficulty");
        if (diffStr != null && !diffStr.isEmpty()) {
            try {
                diff = Difficulty.valueOf(diffStr);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid difficulty: {}", diffStr);
            }
        }

        LabWork lw = new LabWork(
                rs.getString("name"),
                coord,
                rs.getFloat("minimal_point"),
                rs.getDouble("personal_qualities_maximum"),
                diff,
                disc
        );

        lw.setId(rs.getLong("id"));
        lw.setCreationDate(rs.getTimestamp("creation_date"));
        lw.setOwnerId(rs.getInt("owner_id"));
        return lw;
    }

    /**
     * Получает ID пользователя по его логину.
     * Используется для проверки аутентификации.
     */
    public Optional<Integer> getUserIdByLogin(String login) {
        String sql = "SELECT id FROM users WHERE login = ?";
        try (Connection conn = getConnection(); // <-- ИСПРАВЛЕНО: используем getConnection() вместо dataSource
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(rs.getInt("id"));
            }
        } catch (SQLException e) {
            logger.error("Ошибка получения ID пользователя по логину {}: {}", login, e.getMessage());
        }
        return Optional.empty();
    }
}