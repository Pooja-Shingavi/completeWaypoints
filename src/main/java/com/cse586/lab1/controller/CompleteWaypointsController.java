package com.cse586.lab1.controller;

import com.cse586.lab1.service.CompleteWaypointsService;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CompleteWaypointsController {

    private static final Logger LOG = LoggerFactory.getLogger(CompleteWaypointsController.class);

    @Autowired
    CompleteWaypointsService waypointsService;

    @PostMapping(value = {"/index"})
    public String defaultForward(@RequestBody JSONObject searchData) {
        LOG.info("Received request and forwarding it");
        return waypointsService.getData(searchData.get("source").toString(),searchData.get("destination").toString());
    }
}