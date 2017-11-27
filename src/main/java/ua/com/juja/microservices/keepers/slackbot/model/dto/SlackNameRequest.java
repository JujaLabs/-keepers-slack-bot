package ua.com.juja.microservices.keepers.slackbot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @author Nikolay Horushko
 */
@Getter
@AllArgsConstructor
public class SlackNameRequest {
    List<String> slackNames;
}
