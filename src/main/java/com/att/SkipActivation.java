package com.att;

import com.daimler.dvs.hbase.client.HBaseClient;
import com.daimler.dvs.hbase.client.HBaseClientImpl;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SkipActivation {
    public static void main(String[] args) {
        char cj=0xface;
        System.out.println(0xFace);
        System.exit(0);
        //        List<VinClaimCtp> list =  repository.findAllByParams(null, "1", "10", null, 1);
        //        System.out.println(list);
        HBaseClient client = HBaseClientImpl.getInstance();
        FilterList filterList = new FilterList();
        Filter filter = new SingleColumnValueFilter("d".getBytes(), "VerMAIN_PROC".getBytes(),
                CompareFilter.CompareOp.EQUAL, new RegexStringComparator("13.*"));
        filterList.addFilter(filter);

        List<Map<String, String>> list1 = client
                .getRecords("CTP", "d", Collections.singletonList("VerMAIN_PROC"), null, null, null, filterList, 1000, 1000);
        System.out.println(list1);
    }

//    public static void main1(String[] args) {
//        VehicleServiceImpl vehicleService = new VehicleServiceImpl();
//        Map<String, String> map = new HashMap<>();
//        map.put("Vin", "TESTVIN0000000001");
//        map.put("Tan", "TQ1234");
//        Map<String, String> vinClaimVehicle = vehicleService.findById("TESTVIN0000000001");
//        System.out.println(vinClaimVehicle);
//        if(Boolean.parseBoolean(vinClaimVehicle.get("PFCIgnore"))){
//            map.put("PFCIgnore", "false");
//        } else {
//            map.put("PFCIgnore", "true");
//        }
//        vehicleService.save(map);
//    }
}
