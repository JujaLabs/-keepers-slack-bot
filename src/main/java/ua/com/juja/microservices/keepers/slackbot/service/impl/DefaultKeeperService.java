package ua.com.juja.microservices.keepers.slackbot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.keepers.slackbot.dao.KeeperRepository;
import ua.com.juja.microservices.keepers.slackbot.exception.WrongCommandFormatException;
import ua.com.juja.microservices.keepers.slackbot.model.SlackParsedCommand;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;
import ua.com.juja.microservices.keepers.slackbot.model.request.KeeperRequest;
import ua.com.juja.microservices.keepers.slackbot.service.KeeperService;

import javax.inject.Inject;
import java.util.Arrays;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 * @author Konstantin Sergey
 * @author Ivan Shapovalov
 * @author Oleksii Skachkov
 */
@Service
public class DefaultKeeperService implements KeeperService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private KeeperRepository keeperRepository;
    private SlackUserHandlerService slackUserHandlerService;

    @Inject
    public DefaultKeeperService(KeeperRepository keeperRepository, SlackUserHandlerService slackUserHandlerService) {
        this.keeperRepository = keeperRepository;
        this.slackUserHandlerService = slackUserHandlerService;
    }

    @Override
    public String sendKeeperAddRequest(String fromSlackUser, String text) {
        logger.debug("Started create slackParsedCommand and create keeper request");
        SlackParsedCommand slackParsedCommand = slackUserHandlerService.createSlackParsedCommand(fromSlackUser, text);
        KeeperRequest keeperRequest = new KeeperRequest(slackParsedCommand.getFromUser().getUuid(),
                receiveToUser(slackParsedCommand).getUuid(),
                receiveToDirections(slackParsedCommand));

        logger.debug("Received KeeperRequest: [{}]", keeperRequest.toString());
        String[] ids = keeperRepository.addKeeper(keeperRequest);
        logger.info("Added Keeper: [{}]", Arrays.toString(ids));

        String result;

        if (ids.length > 0) {
            result = String.format("Thanks, we added a new Keeper: %s in direction: %s",
                    SlackParsedCommand.wrapSlackUserInFullPattern(slackParsedCommand.getFirstUserFromText().getSlackUser()),
                    keeperRequest.getDirection());
        } else {
            result = "ERROR. Something went wrong. Keeper was not added :(";
        }
        return result;
    }

    @Override
    public String sendKeeperDeactivateRequest(String fromSlackUser, String text) {
        logger.debug("Started create slackParsedCommand and create keeper request");
        SlackParsedCommand slackParsedCommand = slackUserHandlerService.createSlackParsedCommand(fromSlackUser, text);
        KeeperRequest keeperRequest = new KeeperRequest(slackParsedCommand.getFromUser().getUuid(),
                receiveToUser(slackParsedCommand).getUuid(),
                receiveToDirections(slackParsedCommand));

        logger.debug("Received KeeperRequest: [{}]", keeperRequest.toString());
        String[] ids = keeperRepository.deactivateKeeper(keeperRequest);
        logger.info("Deactivated Keeper: [{}]", Arrays.toString(ids));

        String result;

        if (ids.length > 0) {
            result = String.format("Keeper: %s in direction: %s deactivated",
                    SlackParsedCommand.wrapSlackUserInFullPattern(slackParsedCommand.getFirstUserFromText().getSlackUser()),
                    keeperRequest.getDirection());
        } else {
            result = "ERROR. Something went wrong. Keeper was not deactivated :(";
        }
        return result;
    }

    @Override
    public String getKeeperDirections(String fromSlackUser, String text) {
        logger.debug("Started create slackParsedCommand and create keeper request");
        SlackParsedCommand slackParsedCommand = slackUserHandlerService.createSlackParsedCommand(fromSlackUser, text);
        if (slackParsedCommand.getFromUser().equals(slackParsedCommand.getFirstUserFromText())) {
            throw new WrongCommandFormatException("Your own slack in command. To get your " +
                    "own directions use another command");
        }
        KeeperRequest keeperRequest = new KeeperRequest(slackParsedCommand.getFromUser().getUuid(),
                slackParsedCommand.getFirstUserFromText().getUuid(),
                slackParsedCommand.getTextWithoutSlackUsers());

        return getKeeperDirectionsFromRepository(keeperRequest, slackParsedCommand.getFirstUserFromText().getSlackUser());
    }

    @Override
    public String getMyDirections(String fromSlackUser) {
        logger.debug("Started create slackParsedCommand and create keeper request");
        SlackParsedCommand slackParsedCommand = slackUserHandlerService.createSlackParsedCommand(fromSlackUser, "");
        KeeperRequest keeperRequest = new KeeperRequest(slackParsedCommand.getFromUser().getUuid(),
                slackParsedCommand.getFromUser().getUuid(),
                slackParsedCommand.getTextWithoutSlackUsers());

        return getKeeperDirectionsFromRepository(keeperRequest, slackParsedCommand.getFirstUserFromText().getSlackUser());
    }

    private String getKeeperDirectionsFromRepository(KeeperRequest keeperRequest, String keeperSlackUser) {
        logger.debug("Received request to get directions of keeper with uuid: [{}]", keeperRequest.toString());
        String[] directions = keeperRepository.getKeeperDirections(keeperRequest);
        logger.info("Received response from keeperRepository: [{}]", Arrays.toString(directions));

        String responseMessage = "The keeper " + SlackParsedCommand.wrapSlackUserInFullPattern(keeperSlackUser) +
                " has no active directions.";

        if (directions.length > 0) {
            responseMessage = "The keeper " + SlackParsedCommand.wrapSlackUserInFullPattern(keeperSlackUser) +
                    " has active directions: " + Arrays.toString(directions);
        }
        return responseMessage;
    }

    private UserDTO receiveToUser(SlackParsedCommand slackParsedCommand) {

        int userCount = slackParsedCommand.getUserCountInText();

        if (userCount > 1) {
            throw new WrongCommandFormatException(String.format("We found %d slack users in your command '%s'. " +
                            "You can not perform actions with several slack users.",
                    slackParsedCommand.getUserCountInText(), slackParsedCommand.getText()));
        }

        if (userCount == 0) {
            throw new WrongCommandFormatException(String.format("We didn't find any slack user in your command '%s'. " +
                            "You must write the user's slack to perform the action with keepers.",
                    slackParsedCommand.getText()));
        }

        return slackParsedCommand.getFirstUserFromText();
    }

    private String receiveToDirections(SlackParsedCommand parsedCommand) {

        String textWithoutSlackUsers = parsedCommand.getTextWithoutSlackUsers();

        if (textWithoutSlackUsers.length() == 0) {
            throw new WrongCommandFormatException(String.format("We didn't find direction in your command '%s'. " +
                    "You must write the direction to perform the action with keepers.", parsedCommand.getText()));
        }

        if (textWithoutSlackUsers.split(" ").length > 1) {
            throw new WrongCommandFormatException(String.format("We found several directions in your command '%s'. " +
                    "You can perform the action with keepers on one direction only.", parsedCommand.getTextWithoutSlackUsers()));
        }

        return parsedCommand.getTextWithoutSlackUsers();
    }
}