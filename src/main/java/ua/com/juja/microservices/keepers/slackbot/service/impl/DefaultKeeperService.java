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
 */
@Service
public class DefaultKeeperService implements KeeperService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private KeeperRepository keeperRepository;
    private SlackNameHandlerService slackNameHandlerService;

    @Inject
    public DefaultKeeperService(KeeperRepository keeperRepository, SlackNameHandlerService slackNameHandlerService) {
        this.keeperRepository = keeperRepository;
        this.slackNameHandlerService = slackNameHandlerService;
    }

    @Override
    public String sendKeeperAddRequest(String fromUser, String text) {
        logger.debug("Started create slackParsedCommand and create keeper request");
        SlackParsedCommand slackParsedCommand = slackNameHandlerService.createSlackParsedCommand(fromUser, text);
        KeeperRequest keeperRequest = new KeeperRequest(slackParsedCommand.getFromUser().getUuid(),
                receiveToUser(slackParsedCommand).getUuid(),
                receiveToDirections(slackParsedCommand));

        logger.debug("Received KeeperRequest: [{}]", keeperRequest.toString());
        String[] ids = keeperRepository.addKeeper(keeperRequest);
        logger.info("Added Keeper: [{}]", Arrays.toString(ids));

        String result;

        if (ids.length > 0) {
            result = String.format("Thanks, we added a new Keeper: %s in direction: %s",
                    slackParsedCommand.getFirstUserFromText().getSlack(), keeperRequest.getDirection());
        } else {
            result = "ERROR. Something went wrong. Keeper was not added :(";
        }
        return result;
    }

    @Override
    public String sendKeeperDeactivateRequest(String fromUser, String text) {
        logger.debug("Started create slackParsedCommand and create keeper request");
        SlackParsedCommand slackParsedCommand = slackNameHandlerService.createSlackParsedCommand(fromUser, text);
        KeeperRequest keeperRequest = new KeeperRequest(slackParsedCommand.getFromUser().getUuid(),
                receiveToUser(slackParsedCommand).getUuid(),
                receiveToDirections(slackParsedCommand));

        logger.debug("Received KeeperRequest: [{}]", keeperRequest.toString());
        String[] ids = keeperRepository.deactivateKeeper(keeperRequest);
        logger.info("Deactivated Keeper: [{}]", Arrays.toString(ids));

        String result;

        if (ids.length > 0) {
            result = String.format("Keeper: %s in direction: %s deactivated",
                    slackParsedCommand.getFirstUserFromText().getSlack(), keeperRequest.getDirection());
        } else {
            result = "ERROR. Something went wrong. Keeper was not deactivated :(";
        }
        return result;
    }

    @Override
    public String getKeeperDirections(String fromUser, String text) {
        logger.debug("Started create slackParsedCommand and create keeper request");
        SlackParsedCommand slackParsedCommand = slackNameHandlerService.createSlackParsedCommand(fromUser, text);
        if (slackParsedCommand.getFromUser().equals(slackParsedCommand.getFirstUserFromText())) {
            throw new WrongCommandFormatException("Your own slackname in command. To get your " +
                    "own directions use another command");
        }
        KeeperRequest keeperRequest = new KeeperRequest(slackParsedCommand.getFromUser().getUuid(),
                slackParsedCommand.getFirstUserFromText().getUuid(),
                slackParsedCommand.getTextWithoutSlackNames());

        return getKeeperDirectionsFromRepository(keeperRequest, slackParsedCommand.getFirstUserFromText().getSlack());
    }

    @Override
    public String getMyDirections(String fromUser) {
        logger.debug("Started create slackParsedCommand and create keeper request");
        SlackParsedCommand slackParsedCommand = slackNameHandlerService.createSlackParsedCommand(fromUser, "");
        KeeperRequest keeperRequest = new KeeperRequest(slackParsedCommand.getFromUser().getUuid(),
                slackParsedCommand.getFromUser().getUuid(),
                slackParsedCommand.getTextWithoutSlackNames());

        return getKeeperDirectionsFromRepository(keeperRequest, slackParsedCommand.getFirstUserFromText().getSlack());
    }

    private String getKeeperDirectionsFromRepository(KeeperRequest keeperRequest, String keeperSlackName) {
        logger.debug("Received request to get directions of keeper with uuid: [{}]", keeperRequest.toString());
        String[] directions = keeperRepository.getKeeperDirections(keeperRequest);
        logger.info("Received response from keeperRepository: [{}]", Arrays.toString(directions));

        String responseMessage = "The keeper " + keeperSlackName + " has no active directions.";

        if (directions.length > 0) {
            responseMessage = "The keeper " + keeperSlackName + " has active directions: " + Arrays.toString(directions);
        }
        return responseMessage;
    }

    private UserDTO receiveToUser(SlackParsedCommand slackParsedCommand) {

        int userCount = slackParsedCommand.getUserCountInText();

        if (userCount > 1) {
            throw new WrongCommandFormatException(String.format("We found %d slack names in your command: '%s' " +
                            "You can not perform actions with several slack names.",
                    slackParsedCommand.getUserCountInText(), slackParsedCommand.getText()));
        }

        if (userCount == 0) {
            throw new WrongCommandFormatException(String.format("We didn't find any slack name in your command. '%s' " +
                    "You must write the user's slack name to perform the action with keepers.", slackParsedCommand.getText()));
        }

        return slackParsedCommand.getFirstUserFromText();
    }

    private String receiveToDirections(SlackParsedCommand parsedCommand) {

        String textWithoutSlackNames = parsedCommand.getTextWithoutSlackNames();

        if (textWithoutSlackNames.length() == 0) {
            throw new WrongCommandFormatException(String.format("We didn't find direction in your command: '%s' " +
                    "You must write the direction to perform the action with keepers.", parsedCommand.getText()));
        }

        if (textWithoutSlackNames.split(" ").length > 1) {
            throw new WrongCommandFormatException(String.format("We found several directions in your command: '%s' " +
                    "You can perform the action with keepers on one direction only.", parsedCommand.getTextWithoutSlackNames()));
        }

        return parsedCommand.getTextWithoutSlackNames();
    }
}