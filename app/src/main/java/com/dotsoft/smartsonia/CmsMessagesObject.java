package com.dotsoft.smartsonia;

import java.util.ArrayList;

public class CmsMessagesObject {

    public class CustomFields{
        public String worker_name;
        public String notes;
        public String read;
        public String timestamp;
        public String date_split_message;
    }

    public class Result{
        public String ID;
        public String post_title;
        public CustomFields custom_fields;
    }

    public class Root{
        public String respond;
        public ArrayList<Result> result;
    }

}
