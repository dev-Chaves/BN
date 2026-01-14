package com.bn.benefix.management;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("management")
public class ManagementController {

    private final ManagerService managerService;

    public ManagementController(ManagerService managerService) {
        this.managerService = managerService;
    }



}
