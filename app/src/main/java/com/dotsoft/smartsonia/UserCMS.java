package com.dotsoft.smartsonia;

import java.util.ArrayList;

public class UserCMS {

    public class CustomFields{
        public String user_role;
        public String user_info;
    }

    public class Result{
        public String ID;
        public String session_id;
        public String nickname;
        public CustomFields custom_fields;
        public String Access_Token;
    }

    public class Root{
        public String respond;
        public ArrayList<Result> result;
    }

}


