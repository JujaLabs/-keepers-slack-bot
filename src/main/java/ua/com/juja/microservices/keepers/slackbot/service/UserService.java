package ua.com.juja.microservices.keepers.slackbot.service;

import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;

import java.util.List;

/**
 * @author Nikolay Horushko
 * @author OleksiiSkachkov
 */
public interface UserService {
    List<UserDTO> findUsersBySlackIds(List<String> slackIds);
}
