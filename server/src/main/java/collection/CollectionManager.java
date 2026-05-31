package collection;

import data.LabWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Менеджер коллекции с поддержкой многопоточности и прав доступа (Лаба 7).
 */
public class CollectionManager {
    private static final Logger logger = LoggerFactory.getLogger(CollectionManager.class);

    private final LinkedList<LabWork> collection;
    private final Date creationDate;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public CollectionManager(List<LabWork> initialData) {
        this.collection = new LinkedList<>(initialData);
        this.creationDate = new Date();
        Collections.sort(collection);
        logger.info("CollectionManager initialized. Size: {}", collection.size());
    }

    public LinkedList<LabWork> getCollection() {
        lock.readLock().lock();
        try {
            return new LinkedList<>(collection);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Date getCreationDate() {
        return creationDate; // immutable, можно без лока
    }

    public int getSize() {
        lock.readLock().lock();
        try {
            return collection.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getInfo() {
        lock.readLock().lock();
        try {
            return "Тип коллекции: " + collection.getClass().getSimpleName() +
                    "\nДата инициализации: " + creationDate +
                    "\nКоличество элементов: " + collection.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public String showSortedByName() {
        lock.readLock().lock();
        try {
            if (collection.isEmpty()) return "Коллекция пуста.";

            return collection.stream()
                    .sorted((l1, l2) -> l1.getName().compareTo(l2.getName()))
                    .map(LabWork::toString)
                    .collect(Collectors.joining("\n\n"));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Date getCreationDateById(Long id) {
        lock.readLock().lock();
        try {
            return collection.stream()
                    .filter(lw -> lw.getId().equals(id))
                    .findFirst()
                    .map(LabWork::getCreationDate)
                    .orElse(new Date());
        } finally {
            lock.readLock().unlock();
        }
    }

    public String printFieldDescendingDifficulty() {
        lock.readLock().lock();
        try {
            if (collection.isEmpty()) return "Коллекция пуста.";
            return collection.stream()
                    .filter(lw -> lw.getDifficulty() != null)
                    .sorted((l1, l2) -> l2.getDifficulty().compareTo(l1.getDifficulty()))
                    .map(lw -> String.valueOf(lw.getDifficulty()))
                    .collect(Collectors.joining(", "));
        } finally {
            lock.readLock().unlock();
        }
    }

    public String filterLessThanDiscipline(String name) {
        lock.readLock().lock();
        try {
            List<String> result = collection.stream()
                    .filter(lw -> lw.getDiscipline() != null &&
                            lw.getDiscipline().getName() != null &&
                            lw.getDiscipline().getName().compareTo(name) < 0)
                    .map(LabWork::toString)
                    .collect(Collectors.toList());

            if (result.isEmpty()) return "Элементы не найдены.";
            return String.join("\n", result);
        } finally {
            lock.readLock().unlock();
        }
    }

    public float sumOfMinimalPoint() {
        lock.readLock().lock();
        try {
            return collection.stream()
                    .map(LabWork::getMinimalPoint)
                    .reduce(0f, Float::sum);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean existsById(Long id) {
        lock.readLock().lock();
        try {
            return collection.stream().anyMatch(lw -> lw.getId().equals(id));
        } finally {
            lock.readLock().unlock();
        }
    }
    public boolean add(LabWork lw) {
        lock.writeLock().lock();
        try {
            boolean res = collection.add(lw);
            if (res) Collections.sort(collection);
            logger.info("Element ID={} added", lw.getId());
            return res;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Обновляет элемент по ID, только если текущий пользователь — владелец.
     */
    public boolean updateById(Long id, LabWork lw, int ownerId) {
        lock.writeLock().lock();
        try {
            Optional<LabWork> existing = collection.stream()
                    .filter(item -> item.getId().equals(id))
                    .findFirst();

            if (existing.isPresent()) {
                LabWork item = existing.get();
                if (item.getOwnerId() == ownerId) {
                    item.update(lw);
                    logger.info("Element ID={} updated by user {}", id, ownerId);
                    return true;
                } else {
                    logger.warn("User {} cannot update element ID={} (not owner)", ownerId, id);
                    return false;
                }
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Удаляет элемент по ID, только если текущий пользователь — владелец.
     */
    public boolean removeById(Long id, int ownerId) {
        lock.writeLock().lock();
        try {
            Optional<LabWork> existing = collection.stream()
                    .filter(item -> item.getId().equals(id))
                    .findFirst();

            if (existing.isPresent()) {
                if (existing.get().getOwnerId() == ownerId) {
                    collection.removeIf(lw -> lw.getId().equals(id));
                    logger.info("Element ID={} removed by user {}", id, ownerId);
                    return true;
                } else {
                    logger.warn("User {} cannot remove element ID={} (not owner)", ownerId, id);
                    return false;
                }
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Очищает коллекцию от всех элементов, принадлежащих указанному пользователю.
     */
    public void clear(int ownerId) {
        lock.writeLock().lock();
        try {
            int before = collection.size();
            collection.removeIf(lw -> lw.getOwnerId() == ownerId);
            logger.info("Cleared {} elements for user {}", before - collection.size(), ownerId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Добавляет элемент, если он больше максимального в коллекции.
     */
    public boolean addIfMax(LabWork lw) {
        lock.writeLock().lock();
        try {
            if (collection.isEmpty()) {
                collection.add(lw);
                return true;
            }
            LabWork max = collection.stream()
                    .max(LabWork::compareTo)
                    .orElse(null);

            if (max != null && lw.compareTo(max) > 0) {
                collection.add(lw);
                Collections.sort(collection);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Удаляет первый элемент, только если текущий пользователь — его владелец.
     */
    public boolean removeFirst(int ownerId) {
        lock.writeLock().lock();
        try {
            if (!collection.isEmpty()) {
                LabWork first = collection.getFirst();
                if (first.getOwnerId() == ownerId) {
                    collection.removeFirst();
                    logger.info("First element ID={} removed by user {}", first.getId(), ownerId);
                    return true;
                }
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
}