package com.bigdata.app.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import java.io.Serializable;



@Data
public class LinkedService implements Serializable {

    @Id
    private String objectId;

    private String name;

    private String objectType;

    private String _org;

}

