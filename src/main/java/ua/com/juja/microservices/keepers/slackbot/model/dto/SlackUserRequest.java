package ua.com.juja.microservices.keepers.slackbot.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @author Nikolay Horushko
 * @author Oleksii Skachkov
 */
@Getter
@AllArgsConstructor
public class SlackUserRequest {
    @JsonProperty("slackIds")
    List<String> slackUsers;
}
