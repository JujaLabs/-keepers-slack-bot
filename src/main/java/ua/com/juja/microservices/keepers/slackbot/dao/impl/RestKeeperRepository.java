package ua.com.juja.microservices.keepers.slackbot.dao.impl;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ua.com.juja.microservices.keepers.slackbot.dao.KeeperRepository;
import ua.com.juja.microservices.keepers.slackbot.dao.feign.KeepersClient;
import ua.com.juja.microservices.keepers.slackbot.exception.ApiError;
import ua.com.juja.microservices.keepers.slackbot.exception.KeeperExchangeException;
import ua.com.juja.microservices.keepers.slackbot.model.request.KeeperRequest;
import ua.com.juja.microservices.keepers.slackbot.utils.Utils;

import javax.inject.Inject;
import java.util.Arrays;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 * @author Konstantin Sergey
 * @author Ivan Shapovalov
 */
@Repository
@Slf4j
public class RestKeeperRepository implements KeeperRepository {
    @Inject
    private KeepersClient keepersClient;

    @Override
    public String[] addKeeper(KeeperRequest keeperRequest) {
        log.debug("Received KeeperRequest: [{}], url: [{}], HttpMethod: [{}] ", keeperRequest.toString());
        String[] addedKeeperIDs;
        try {
            addedKeeperIDs = keepersClient.addKeeper(keeperRequest);
            log.debug("Finished request to Keepers service. Response is: [{}]", Arrays.toString(addedKeeperIDs));
        } catch (FeignException ex) {
            ApiError error = Utils.convertToApiError(ex.getMessage());
            log.warn("Keepers service returned an error: [{}]", error);
            throw new KeeperExchangeException(error, ex);
        }
        log.info("KeeperRepository processed result: [{}]", Arrays.toString(addedKeeperIDs));
        return addedKeeperIDs;      }

    @Override
    public String[] deactivateKeeper(KeeperRequest keeperRequest) {
        log.debug("Received KeeperRequest: [{}], url: [{}], HttpMethod: [{}] ", keeperRequest.toString());
        String[] deactivatedKeeperIDs;
        try {
            deactivatedKeeperIDs = keepersClient.deactivateKeeper(keeperRequest);
            log.debug("Finished request to Keepers service. Response is: [{}]", Arrays.toString(deactivatedKeeperIDs));
        } catch (FeignException ex) {
            ApiError error = Utils.convertToApiError(ex.getMessage());
            log.warn("Keepers service returned an error: [{}]", error);
            throw new KeeperExchangeException(error, ex);
        }
        log.info("KeeperRepository processed result: [{}]", Arrays.toString(deactivatedKeeperIDs));
        return deactivatedKeeperIDs;    }

    @Override
    public String[] getKeeperDirections(KeeperRequest keeperRequest) {
        log.debug("Received KeeperRequest: [{}], url: [{}], HttpMethod: [{}] ", keeperRequest.toString());
        String[] keepersDirections;
        try {
            keepersDirections = keepersClient.getKeeperDirections(keeperRequest, keeperRequest.getUuid());
            log.debug("Finished request to Keepers service. Response is: [{}]", Arrays.toString(keepersDirections));
        } catch (FeignException ex) {
            ApiError error = Utils.convertToApiError(ex.getMessage());
            log.warn("Keepers service returned an error: [{}]", error);
            throw new KeeperExchangeException(error, ex);
        }
        log.info("KeeperRepository processed result: [{}]", Arrays.toString(keepersDirections));
        return keepersDirections;
    }
}
