package com.dotsoft.smartsonia;

import java.util.ArrayList;

public class CmsUserObject {

    /**
     * CMS USER CLASS
     */
    public class Result{
        public String ID;
        public String nickname;
        public String Access_Token;
    }

    public class Root{
        public String respond;
        public ArrayList<Result> result;
    }


}
