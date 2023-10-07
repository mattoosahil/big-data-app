package com.bigdata.app.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.index.Indexed;
import java.io.Serializable;


@Data
public class Plan implements Serializable {

    @Id
    @Indexed
    private String objectId;

    private PlanCostShares planCostShares;

    private String _org;

    private String objectType;

    private LinkedPlanService[] linkedPlanServices;

    private String planType;

    private String creationDate;
}
