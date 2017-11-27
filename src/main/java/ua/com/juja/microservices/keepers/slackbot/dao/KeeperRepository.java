package ua.com.juja.microservices.keepers.slackbot.dao;

import ua.com.juja.microservices.keepers.slackbot.model.request.KeeperRequest;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 */
public interface KeeperRepository {
    String[] addKeeper(KeeperRequest keeperRequest);

    String[] deactivateKeeper(KeeperRequest keeperRequest);

    String[] getKeeperDirections(KeeperRequest keeperRequest);
}
