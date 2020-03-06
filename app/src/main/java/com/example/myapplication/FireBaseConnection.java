package com.example.myapplication;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class FireBaseConnection {
    FirebaseFirestore db;

    public void saveLocationParameters(int gps_quality,int noOfSatellite,double lat,double lot,Object avgSnr,Object hdop,String tag,Double error){
        HashMap<String,Object> map = new HashMap<>();
        map.put("gps_quality",gps_quality);
        map.put("noOfSatellite",noOfSatellite);
        map.put("lat",lat);
        map.put("lot",lot);
        map.put("avgSnr",avgSnr);
        map.put("hdop",hdop);
        map.put("routeTag",tag);
        map.put("accuracy",error);
        db = FirebaseFirestore.getInstance();
        db.collection("route").add(map);
    }


}
