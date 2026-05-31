package data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Основной класс коллекции, представляющий лабораторную работу.
 * Реализует интерфейсы Serializable для сохранения в файл и Comparable для сортировки по ID.
 * Содержит информацию о названии, координатах, дате создания, минимальном балле,
 * максимальном количестве личных качеств, сложности и дисциплине.
 *
 * @author Данил
 * @version 1.0
 * @see Coordinates
 * @see Discipline
 * @see Difficulty
 */

public class LabWork implements Serializable, Comparable<LabWork> {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Coordinates coordinates;
    private Date creationDate;
    private float minimalPoint;
    private double personalQualitiesMaximum;
    private Difficulty difficulty;
    private Discipline discipline;
    private int ownerId;

    /**
     * Конструктор для создания новой лабораторной работы пользователем.
     * ID и дата создания генерируются автоматически.
     *
     * @param name название работы
     * @param coordinates координаты места проведения
     * @param minimalPoint минимальный балл за работу (> 0)
     * @param personalQualitiesMaximum максимальное количество личных качеств (> 0)
     * @param difficulty уровень сложности
     * @param discipline связанная дисциплина
     * @throws IllegalArgumentException если данные не проходят валидацию
     */
    public LabWork(String name, Coordinates coordinates, float minimalPoint,
                   double personalQualitiesMaximum, Difficulty difficulty, Discipline discipline) {
        validate(name, minimalPoint, personalQualitiesMaximum, difficulty, discipline);

        this.creationDate = new Date();
        this.name = name;
        this.coordinates = coordinates;
        this.minimalPoint = minimalPoint;
        this.personalQualitiesMaximum = personalQualitiesMaximum;
        this.difficulty = difficulty;
        this.discipline = discipline;
    }
    /**
     * Конструктор для восстановления объекта из файла (десериализация).
     * Позволяет задать конкретный ID и дату создания.
     *
     * @param id уникальный идентификатор (> 0)
     * @param name название работы
     * @param coordinates координаты
     * @param creationDate дата создания
     * @param minimalPoint минимальный балл
     * @param personalQualitiesMaximum максимальное количество личных качеств
     * @param difficulty уровень сложности
     * @param discipline связанная дисциплина
     * @throws IllegalArgumentException если ID меньше или равно 0 или данные не валидны
     */
    public LabWork(Long id, String name, Coordinates coordinates, Date creationDate,
                   float minimalPoint, double personalQualitiesMaximum,
                   Difficulty difficulty, Discipline discipline) {
        if (id != null && id <= 0) throw new IllegalArgumentException("ID должен быть > 0");
        validate(name, minimalPoint, personalQualitiesMaximum, difficulty, discipline);

        this.id = id;
        this.creationDate = creationDate != null ? creationDate : new Date();
        this.name = name;
        this.coordinates = coordinates;
        this.minimalPoint = minimalPoint;
        this.personalQualitiesMaximum = personalQualitiesMaximum;
        this.difficulty = difficulty;
        this.discipline = discipline;

    }

    private void validate(String name, float minimalPoint, double pqMax, Difficulty diff, Discipline disc) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Name не может быть пустым");
        if (minimalPoint <= 0) throw new IllegalArgumentException("minimalPoint должен быть > 0");
        if (pqMax <= 0) throw new IllegalArgumentException("personalQualitiesMaximum должен быть > 0");
        if (diff == null) throw new IllegalArgumentException("Difficulty не может быть null");
        if (disc == null) throw new IllegalArgumentException("Discipline не может быть null");
    }

    public int getOwnerId() { return ownerId; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public Coordinates getCoordinates() { return coordinates; }
    public Date getCreationDate() { return creationDate; }
    public float getMinimalPoint() { return minimalPoint; }
    public double getPersonalQualitiesMaximum() { return personalQualitiesMaximum; }
    public Difficulty getDifficulty() { return difficulty; }
    public Discipline getDiscipline() { return discipline; }

    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public void setName(String name) { this.name = name; }
    public void setCoordinates(Coordinates coordinates) { this.coordinates = coordinates; }
    public void setMinimalPoint(float minimalPoint) { this.minimalPoint = minimalPoint; }
    public void setPersonalQualitiesMaximum(double personalQualitiesMaximum) { this.personalQualitiesMaximum = personalQualitiesMaximum; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public void setDiscipline(Discipline discipline) { this.discipline = discipline; }
    public void setId(Long id) {this.id = id;}
    public void setCreationDate(Date creationDate) { this.creationDate = creationDate;}

    @Override
    public int compareTo(LabWork other) {
        return this.id.compareTo(other.id);
    }
    /**
     * Обновляет поля текущей лабораторной работы данными из переданного объекта.
     * Поля id и creationDate не изменяются.
     *
     * @param newLabWork объект LabWork с новыми данными для обновления
     */
    public void update(LabWork newLabWork) {
        this.name = newLabWork.name;
        this.coordinates = newLabWork.coordinates;
        this.minimalPoint = newLabWork.minimalPoint;
        this.personalQualitiesMaximum = newLabWork.personalQualitiesMaximum;
        this.difficulty = newLabWork.difficulty;
        this.discipline = newLabWork.discipline;
    }


    @Override
    public String toString() {
        return "LabWork{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", creationDate=" + creationDate +
                ", minimalPoint=" + minimalPoint +
                ", personalQualitiesMaximum=" + personalQualitiesMaximum +
                ", difficulty=" + difficulty +
                ", discipline=" + discipline +
                ", owner=" + ownerId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabWork labWork = (LabWork) o;
        return Objects.equals(id, labWork.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}