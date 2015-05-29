package org.odhsi.athena.controllers;

import org.odhsi.athena.dto.VocabularyBuildLogDTO;
import org.odhsi.athena.dto.VocabularyStatusDTO;
import org.odhsi.athena.services.VocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Created by GMalikov on 13.05.2015.
 */

@Controller
public class VocabularyBuilderController {

    @Autowired
    private VocabularyService vocabularyService;

    private static final Logger LOGGER = LoggerFactory.getLogger(VocabularyBuilderController.class);

    @RequestMapping(value = "/getVocabularyStatuses", method = RequestMethod.GET)
    @ResponseBody
    public List<VocabularyStatusDTO> getVocabularyStatuses(){
        return vocabularyService.getAllVocabularyStatuses();
    }

    @RequestMapping(value = "/getLogForVocabulary", method = RequestMethod.GET)
    public List<VocabularyBuildLogDTO> getLogForVocabulary(@RequestParam String vocabularyId){
        LOGGER.info("Getting logs");
        List<VocabularyBuildLogDTO> result = vocabularyService.getLogForVocabulary(vocabularyId);
        return result;
    }
}