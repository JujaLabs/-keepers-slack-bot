package ua.com.juja.microservices.keepers.slackbot.service;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 * @author Ivan Shapovalov
 */
public interface KeeperService {
    String sendKeeperAddRequest(String fromUser, String text);

    String sendKeeperDeactivateRequest(String fromUser, String text);

    String getKeeperDirections(String fromUser, String text);

    String getMyDirections(String fromUser);
}
