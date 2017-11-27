package ua.com.juja.microservices.keepers.slackbot.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Nikolay Horushko
 */
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class UserDTO {
    @JsonProperty
    private String uuid;
    @JsonProperty
    private String slack;
}
