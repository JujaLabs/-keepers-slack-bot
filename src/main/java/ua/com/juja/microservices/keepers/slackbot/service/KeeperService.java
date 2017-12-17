package ua.com.juja.microservices.keepers.slackbot.service;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 * @author Ivan Shapovalov
 * @author Oleksii Skachkov
 */
public interface KeeperService {
    String sendKeeperAddRequest(String fromSlackUser, String text);

    String sendKeeperDeactivateRequest(String fromSlackUser, String text);

    String getKeeperDirections(String fromSlackUser, String text);

    String getMyDirections(String fromSlackUser);
}
