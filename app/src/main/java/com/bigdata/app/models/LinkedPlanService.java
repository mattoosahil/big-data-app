package com.bigdata.app.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import java.io.Serializable;



@Data
public class LinkedPlanService implements Serializable {

    @Id
    private String objectId;

    PlanCostShares planserviceCostShares;

    private String _org;

    private String objectType;

    LinkedService linkedService;

}