/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.synthsys.fm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ahume
 */
public class JobStatus {

    public enum Status { RUNNING, FINISHED };
    
    
    private Status                         status;
    final private String                   id;
    final private List<Map<String,Double>> data;
    
    public JobStatus(String id) {
        this.id = id;
        this.status = Status.RUNNING;
        this.data = new ArrayList<>();
    }
    
    public List<Map<String,Double>> getData() {
        return data;
    }
    
    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public void addData(Map<String,Double> map) {
        data.add(map);
    }
}
