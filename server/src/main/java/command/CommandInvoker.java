package command;

import collection.CollectionManager;
import database.DatabaseManager;
import commands.*;
import data.LabWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class CommandInvoker {
    private static final Logger logger = LoggerFactory.getLogger(CommandInvoker.class);

    private final CollectionManager collectionManager;
    private final DatabaseManager dbManager;

    public CommandInvoker(CollectionManager collectionManager, DatabaseManager dbManager) {
        this.collectionManager = collectionManager;
        this.dbManager = dbManager;
        logger.info("CommandInvoker initialized");
    }

    /**
     * Проверяет логин и хэш пароля пользователя.
     */
    private boolean authenticateUser(int userId, String login, String passwordHash) {
        try {
            int validatedId = dbManager.validateUser(login, passwordHash);

            if (validatedId == -1) {
                logger.warn("Неверный пароль для пользователя '{}'", login);
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("Ошибка аутентификации пользователя '{}': {}", login, e.getMessage());
            return false;
        }
    }

    /**
     * Получает реальный ID пользователя по логину из БД.
     */
    private int getRealUserId(String login) {
        Optional<Integer> realUserIdOpt = dbManager.getUserIdByLogin(login);
        if (!realUserIdOpt.isPresent()) {
            logger.error("Пользователь '{}' не найден в базе", login);
            return -1;
        }
        return realUserIdOpt.get();
    }

    public String execute(Command cmd, int userId, String login, String passwordHash) {
        if (cmd == null) {
            logger.error("Получена null команда!");
            return "Ошибка: пустая команда";
        }

        String cmdType = cmd.getType();
        if (cmdType == null) {
            logger.error("Получена команда с null type!");
            return "Ошибка: команда без типа";
        }

        logger.info(">>> ВЫПОЛНЕНИЕ КОМАНДЫ: '{}' от пользователя '{}' (ID={})",
                cmdType, login, userId);

        // Проверяем аутентификацию для всех команд КРОМЕ register
        if (!"register".equalsIgnoreCase(cmdType)) {
            if (!authenticateUser(userId, login, passwordHash)) {
                logger.warn("ОТКАЗАНО В ДОСТУПЕ: Неверный логин или пароль для пользователя '{}' (ID={})", login, userId);
                return "Ошибка аутентификации: Неверный логин или пароль.";
            }
        }

        try {
            switch (cmdType.toLowerCase()) {
                case "info":
                    logger.info("Обработка команды info");
                    String baseInfo = collectionManager.getInfo();
                    int realUserId = getRealUserId(login);
                    return baseInfo + "\nПользователь: " + login + " (ID=" + realUserId + ")";

                case "show":
                    logger.info("Обработка команды show");
                    return collectionManager.showSortedByName();

                case "add":
                    logger.info("Обработка команды add");
                    return handleAdd((AddCommand) cmd, userId, login);

                case "update":
                    logger.info("Обработка команды update");
                    return handleUpdate((UpdateCommand) cmd, userId, login);

                case "remove_by_id":
                    logger.info("Обработка команды remove_by_id");
                    return handleRemoveById((RemoveByIdCommand) cmd, userId, login);

                case "clear":
                    logger.info("Обработка команды clear");
                    return handleClear(userId, login);

                case "remove_first":
                    logger.info("Обработка команды remove_first");
                    return handleRemoveFirst(userId, login);

                case "sum_of_minimal_point":
                    logger.info("Обработка команды sum_of_minimal_point");
                    return "Сумма minimalPoint: " + collectionManager.sumOfMinimalPoint();

                case "print_field_descending_difficulty":
                    logger.info("Обработка команды print_field_descending_difficulty");
                    return collectionManager.printFieldDescendingDifficulty();

                case "filter_less_than_discipline":
                    logger.info("Обработка команды filter_less_than_discipline");
                    String disciplineName = ((FilterLessThanDisciplineCommand) cmd).getDisciplineName();
                    return collectionManager.filterLessThanDiscipline(disciplineName);

                case "add_if_max":
                    logger.info("Обработка команды add_if_max");
                    return handleAddIfMax((AddIfMaxCommand) cmd, userId, login);

                case "register":
                    logger.info("Обработка команды register");
                    RegisterCommand regCmd = (RegisterCommand) cmd;

                    String regLogin = regCmd.getLogin();
                    if (regLogin == null || regLogin.length() < 3 || regLogin.length() > 20) {
                        return "Ошибка: логин должен содержать от 3 до 20 символов";
                    }

                    String regHash = regCmd.getPasswordHash();
                    if (regHash == null || regHash.isEmpty()) {
                        return "Ошибка: пароль не может быть пустым";
                    }

                    if (dbManager.registerUser(regLogin, regHash)) {
                        logger.info("Пользователь '{}' успешно зарегистрирован", regLogin);
                        return "Пользователь '" + regLogin + "' успешно зарегистрирован! Теперь вы можете войти.";
                    } else {
                        logger.warn("Не удалось зарегистрировать '{}': возможно, логин занят", regLogin);
                        return "Ошибка: пользователь с таким логином уже существует";
                    }

                default:
                    logger.error("НЕИЗВЕСТНАЯ КОМАНДА: '{}'", cmdType);
                    return "Неизвестная команда: " + cmdType;
            }
        } catch (Exception e) {
            logger.error("ОШИБКА при выполнении команды '{}': {}", cmdType, e.getMessage(), e);
            return "Внутренняя ошибка сервера: " + e.getMessage();
        }
    }

    private String handleAdd(AddCommand cmd, int userId, String login) {
        try {
            LabWork lwAdd = cmd.getLabWork();
            if (lwAdd == null) {
                return "Ошибка: пустой объект LabWork";
            }

            int realUserId = getRealUserId(login);
            if (realUserId == -1) {
                return "Ошибка: пользователь не найден";
            }

            long newId = dbManager.getNextId();
            lwAdd.setId(newId);
            lwAdd.setOwnerId(realUserId);

            if (dbManager.addLabWork(lwAdd, realUserId)) {
                collectionManager.add(lwAdd);
                logger.info("Элемент ID={} добавлен пользователем {} (ID={})", newId, login, realUserId);
                return "Элемент успешно добавлен с ID: " + newId;
            } else {
                return "Ошибка при добавлении в базу данных";
            }
        } catch (Exception e) {
            logger.error("Ошибка в handleAdd: {}", e.getMessage(), e);
            return "Ошибка добавления: " + e.getMessage();
        }
    }

    private String handleUpdate(UpdateCommand cmd, int userId, String login) {
        try {
            Long idUpdate = cmd.getId();
            LabWork lwUpdate = cmd.getLabWork();

            if (idUpdate == null || lwUpdate == null) {
                return "Ошибка: некорректные данные для обновления";
            }

            int realUserId = getRealUserId(login);
            if (realUserId == -1) {
                return "Ошибка: пользователь не найден";
            }

            lwUpdate.setCreationDate(collectionManager.getCreationDateById(idUpdate));

            if (!dbManager.existsById(idUpdate)) {
                return "Элемент с ID=" + idUpdate + " не найден в базе данных";
            }

            var ownerOpt = dbManager.getOwnerById(idUpdate);
            if (ownerOpt.isPresent() && ownerOpt.get() != realUserId) {
                return "Ошибка: у вас нет прав на изменение этого элемента";
            }

            if (dbManager.updateLabWork(lwUpdate, realUserId)) {
                collectionManager.updateById(idUpdate, lwUpdate, realUserId);
                logger.info("Элемент ID={} обновлён пользователем {} (ID={})", idUpdate, login, realUserId);
                return "Элемент с ID=" + idUpdate + " успешно обновлён";
            } else {
                return "Ошибка при обновлении в базе данных";
            }
        } catch (Exception e) {
            logger.error("Ошибка в handleUpdate: {}", e.getMessage(), e);
            return "Ошибка обновления: " + e.getMessage();
        }
    }

    private String handleRemoveById(RemoveByIdCommand cmd, int userId, String login) {
        try {
            Long idRemove = cmd.getId();
            if (idRemove == null) {
                return "Ошибка: не указан ID для удаления";
            }

            int realUserId = getRealUserId(login);
            if (realUserId == -1) {
                return "Ошибка: пользователь не найден";
            }

            if (!dbManager.existsById(idRemove)) {
                return "Элемент с ID=" + idRemove + " не найден";
            }

            var ownerOpt = dbManager.getOwnerById(idRemove);
            if (ownerOpt.isPresent() && ownerOpt.get() != realUserId) {
                return "Ошибка: у вас нет прав на удаление этого элемента";
            }

            if (dbManager.removeLabWork(idRemove, realUserId)) {
                collectionManager.removeById(idRemove, realUserId);
                logger.info("Элемент ID={} удалён пользователем {} (ID={})", idRemove, login, realUserId);
                return "Элемент с ID=" + idRemove + " успешно удалён";
            } else {
                return "Ошибка при удалении из базы данных";
            }
        } catch (Exception e) {
            logger.error("Ошибка в handleRemoveById: {}", e.getMessage(), e);
            return "Ошибка удаления: " + e.getMessage();
        }
    }

    private String handleClear(int userId, String login) {
        try {
            int realUserId = getRealUserId(login);
            if (realUserId == -1) {
                return "Ошибка: пользователь не найден";
            }

            int removedFromDb = dbManager.clearUserLabWorks(realUserId);
            collectionManager.clear(realUserId);
            logger.info("Пользователь {} (ID={}) очистил {} элементов", login, realUserId, removedFromDb);
            return "Коллекция очищена. Удалено элементов: " + removedFromDb;
        } catch (Exception e) {
            logger.error("Ошибка в handleClear: {}", e.getMessage(), e);
            return "Ошибка очистки: " + e.getMessage();
        }
    }

    private String handleRemoveFirst(int userId, String login) {
        try {
            int realUserId = getRealUserId(login);
            if (realUserId == -1) {
                return "Ошибка: пользователь не найден";
            }

            var firstOpt = collectionManager.getCollection().stream().findFirst();
            if (firstOpt.isEmpty()) {
                return "Коллекция пуста";
            }

            LabWork first = firstOpt.get();
            if (first.getOwnerId() != realUserId) {
                return "Ошибка: вы не являетесь владельцем первого элемента";
            }

            if (dbManager.removeLabWork(first.getId(), realUserId)) {
                collectionManager.removeFirst(realUserId);
                logger.info("Первый элемент ID={} удалён пользователем {} (ID={})", first.getId(), login, realUserId);
                return "Первый элемент успешно удалён";
            } else {
                return "Ошибка при удалении первого элемента из базы данных";
            }
        } catch (Exception e) {
            logger.error("Ошибка в handleRemoveFirst: {}", e.getMessage(), e);
            return "Ошибка удаления первого элемента: " + e.getMessage();
        }
    }

    private String handleAddIfMax(AddIfMaxCommand cmd, int userId, String login) {
        try {
            LabWork lwIfMax = cmd.getLabWork();
            if (lwIfMax == null) {
                return "Ошибка: пустой объект LabWork";
            }

            int realUserId = getRealUserId(login);
            if (realUserId == -1) {
                return "Ошибка: пользователь не найден";
            }

            lwIfMax.setOwnerId(realUserId);
            long newId = dbManager.getNextId();
            lwIfMax.setId(newId);

            if (collectionManager.addIfMax(lwIfMax)) {
                if (dbManager.addLabWork(lwIfMax, realUserId)) {
                    logger.info("Элемент ID={} добавлен как максимальный пользователем {} (ID={})", newId, login, realUserId);
                    return "Элемент добавлен как максимальный с ID: " + newId;
                } else {
                    collectionManager.removeById(newId, realUserId);
                    return "Ошибка при сохранении в базу данных";
                }
            } else {
                return "Элемент не является максимальным, добавление отменено";
            }
        } catch (Exception e) {
            logger.error("Ошибка в handleAddIfMax: {}", e.getMessage(), e);
            return "Ошибка add_if_max: " + e.getMessage();
        }
    }
}