package com.bigdata.app.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import java.io.Serializable;



@Data
public class PlanCostShares implements Serializable {

    @Id
    private String objectId;

    private int copay;

    private String objectType;

    private int deductible;

    private String _org;
}
