package ua.com.juja.microservices.keepers.slackbot.model.request;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Dmitriy Lyashenko
 */
@Getter
@ToString
@EqualsAndHashCode
public class KeeperRequest {

    private String from;
    private String uuid;
    private String direction;

    public KeeperRequest(String from, String uuid, String direction) {
        this.from = from;
        this.uuid = uuid;
        this.direction = direction;
    }
}
