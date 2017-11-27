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
import ua.com.juja.microservices.keepers.slackbot.dao.KeeperRepository;
import ua.com.juja.microservices.keepers.slackbot.exception.KeeperExchangeException;
import ua.com.juja.microservices.keepers.slackbot.model.request.KeeperRequest;

import javax.inject.Inject;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RestKeeperRepositoryTest {

    @Inject
    private KeeperRepository keeperRepository;

    @Inject
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @Value("${keepers.baseURL}")
    private String urlBaseKeepers;
    @Value("${keepers.rest.api.version}")
    private String version;
    @Value("${keepers.endpoint.keepers}")
    private String urlKeepers;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    public void shouldReturnKeeperIdWhenSendAddKeeperToKeepersService() {
        //given
        String expectedRequestBody = "{\"from\":\"qwer\",\"uuid\":\"67ui\",\"direction\":\"teams\"}";
        String expectedRequestHeader = "application/json";
        mockServer.expect(requestTo(urlBaseKeepers + version + urlKeepers))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(), containsString(expectedRequestHeader)))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedRequestBody)))
                .andRespond(withSuccess("[\"1000\"]", MediaType.APPLICATION_JSON));
        //when
        String[] result = keeperRepository.addKeeper(new KeeperRequest("qwer", "67ui", "teams"));

        // then
        mockServer.verify();
        assertEquals(result.length, 1);
        assertEquals("[1000]", Arrays.toString(result));
    }

    @Test
    public void shouldThrowExceptionWhenSendAddKeeperToKeepersServiceThrowException() {
        // given
        String expectedRequestBody = "{\"from\":\"qwer\",\"uuid\":\"67ui\",\"direction\":\"teams\"}";
        String expectedRequestHeader = "application/json";
        mockServer.expect(requestTo(urlBaseKeepers + version + urlKeepers))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(), containsString(expectedRequestHeader)))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedRequestBody)))
                .andRespond(withBadRequest().body("{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}"));
        //then
        thrown.expect(KeeperExchangeException.class);
        thrown.expectMessage(containsString("Oops something went wrong :("));
        //when
        keeperRepository.addKeeper(new KeeperRequest("qwer", "67ui", "teams"));
    }

    @Test
    public void shouldReturnKeeperIdWhenSendDeactivateKeeperToKeepersService() {
        //given
        String expectedRequestBody = "{\"from\":\"qwer\",\"uuid\":\"67ui\",\"direction\":\"teams\"}";
        String expectedRequestHeader = "application/json";
        mockServer.expect(requestTo(urlBaseKeepers + version + urlKeepers))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(), containsString(expectedRequestHeader)))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedRequestBody)))
                .andRespond(withSuccess("[\"1000\"]", MediaType.APPLICATION_JSON));
        //when
        String[] result = keeperRepository.deactivateKeeper(new KeeperRequest("qwer", "67ui", "teams"));

        // then
        mockServer.verify();
        assertEquals(result.length, 1);
        assertEquals("[1000]", Arrays.toString(result));
    }

    @Test
    public void shouldThrowExceptionWhenSendDeactivateKeeperToKeepersServiceThrowException() {
        // given
        String expectedRequestBody = "{\"from\":\"qwer\",\"uuid\":\"67ui\",\"direction\":\"teams\"}";
        String expectedRequestHeader = "application/json";
        mockServer.expect(requestTo(urlBaseKeepers + version + urlKeepers))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(), containsString(expectedRequestHeader)))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedRequestBody)))
                .andRespond(withBadRequest().body("{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}"));
        //then
        thrown.expect(KeeperExchangeException.class);
        thrown.expectMessage(containsString("Oops something went wrong :("));
        //when
        keeperRepository.deactivateKeeper(new KeeperRequest("qwer", "67ui", "teams"));
    }

    @Test
    public void shouldReturnKeeperDirections() {
        //given
        String expectedRequestBody = "{\"from\":\"fromUser\",\"uuid\":\"0000-1111\",\"direction\":\"direction1\"}";
        String expectedRequestHeader = "application/json";
        mockServer.expect(requestTo(urlBaseKeepers + version + urlKeepers + "/0000-1111"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(), containsString(expectedRequestHeader)))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedRequestBody)))
                .andRespond(withSuccess("[\"direction1\"]", MediaType.APPLICATION_JSON));
        //when
        String[] actualList = keeperRepository.getKeeperDirections(
                new KeeperRequest("fromUser", "0000-1111", "direction1"));
        // then
        mockServer.verify();
        assertEquals("[direction1]", Arrays.toString(actualList));
    }
}