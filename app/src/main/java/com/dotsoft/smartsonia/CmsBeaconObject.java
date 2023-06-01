package com.dotsoft.smartsonia;

import java.util.ArrayList;

/**
 * CMS BEACON OBJECT CLASS ( FOR BEACON MAJOR IDs LIST)
 */

public class CmsBeaconObject {

    public class CustomFields{
        public String beacon_id;
        public String active;
    }

    public class Result{
        public String ID;
        public CustomFields custom_fields;
    }

    public class Root{
        public String respond;
        public ArrayList<Result> result;
    }

}
