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
    public List<UserDTO> findUsersBySlackIds(List<String> slackIds) {
        logger.debug("Received SlackId: [{}] for conversion", slackIds.toString());
        List<UserDTO> users = userRepository.findUsersBySlackIds(slackIds);
        logger.info("Found users: [{}] by SlackId: [{}]", users.toString(), slackIds.toString());
        return users;
    }
}
