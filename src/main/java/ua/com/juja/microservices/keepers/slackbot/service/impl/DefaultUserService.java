package ua.com.juja.microservices.keepers.slackbot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.keepers.slackbot.dao.UserRepository;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;
import ua.com.juja.microservices.keepers.slackbot.service.UserService;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Nikolay Horushko
 * @author Oleksii Skachkov
 */
@Service
public class DefaultUserService implements UserService {
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public DefaultUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserDTO> findUsersBySlackUsers(List<String> slackUsers) {
        logger.debug("Received SlackUsers: [{}] for conversion", slackUsers.toString());
        List<UserDTO> users = userRepository.findUsersBySlackUsers(slackUsers);
        logger.info("Found users: [{}] by SlackUsers: [{}]", users.toString(), slackUsers.toString());
        return users;
    }
}
