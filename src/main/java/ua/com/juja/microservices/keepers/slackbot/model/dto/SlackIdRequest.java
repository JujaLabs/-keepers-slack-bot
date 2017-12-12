package ua.com.juja.microservices.keepers.slackbot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @author Nikolay Horushko
 * @author Oleksii Skachkov
 */
@Getter
@AllArgsConstructor
public class SlackIdRequest {
    List<String> slackIds;
}
