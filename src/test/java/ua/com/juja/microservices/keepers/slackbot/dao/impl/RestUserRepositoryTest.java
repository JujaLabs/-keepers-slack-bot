package ua.com.juja.microservices.keepers.slackbot.dao.impl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.keepers.slackbot.dao.UserRepository;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Nikolay Horushko
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RestUserRepositoryTest {

    @Inject
    private UserRepository userRepository;

    @Inject
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @Value("${users.baseURL}")
    private String urlBaseUsers;
    @Value("${users.rest.api.version}")
    private String version;
    @Value("${users.endpoint.usersBySlackNames}")
    private String urlGetUsers;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    public void shouldReturnListUserDTOWhenSendSlackNameList() {
        //given
        List<String> slackNames = new ArrayList<>();
        slackNames.add("@bob.slack");
        slackNames.add("john.slack");
        mockServer.expect(requestTo(urlBaseUsers + version + urlGetUsers))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string("{\"slackNames\":[\"@bob.slack\",\"@john.slack\"]}"))
                .andRespond(withSuccess("[{\"uuid\":\"AAAA123\",\"slack\":\"@bob.slack\"}, " +
                        "{\"uuid\":\"AAAA321\",\"slack\":\"@john.slack\"}]", MediaType.APPLICATION_JSON_UTF8));
        //when
        List<UserDTO> result = userRepository.findUsersBySlackNames(slackNames);
        // then
        mockServer.verify();
        assertEquals("[UserDTO(uuid=AAAA123, slack=@bob.slack), UserDTO(uuid=AAAA321, slack=@john.slack)]",
                result.toString());
    }
}