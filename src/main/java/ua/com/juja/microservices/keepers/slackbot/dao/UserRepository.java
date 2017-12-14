package ua.com.juja.microservices.keepers.slackbot.dao;

import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;

import java.util.List;

/**
 * @author Nikolay Horushko
 * @author Oleksii Skachkov
 */
public interface UserRepository {
    List<UserDTO> findUsersBySlackUsers(List<String> slackUsers);
}
