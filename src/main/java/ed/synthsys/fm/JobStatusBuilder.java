/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.synthsys.fm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Builds a JobStatus object from a Reader that gives access to the data
 * written by the simulator.
 */
@Component
public class JobStatusBuilder {
    
    // String used to make a job as finished
    static final private String FINISHED_LINE = "finished";
    
    /**
     * Constructor.
     */
    public JobStatusBuilder() {
    }

    /**
     * Builds a job status by reading the simulation result from the given
     * reader.
     * 
     * @param id       job ID
     * @param reader   reader or null if there is no file to read yet
     * 
     * @return JobStatus object containing the data in the result file
     * 
     * @throws IOException if an IO error occurs
     */
    public JobStatus build(String id, Reader reader) throws IOException {

        JobStatus jobStatus = new JobStatus(id);
        
        jobStatus.setStatus(JobStatus.Status.RUNNING);

        if (reader != null) {
            // Read the data from the data file
            BufferedReader br = new BufferedReader(reader);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.equalsIgnoreCase(FINISHED_LINE)) {
                    jobStatus.setStatus(JobStatus.Status.FINISHED);
                }    
                else {
                    jobStatus.addData(lineToValuesMap(line));
                }
            }
        }
        return jobStatus;
    }
    
    /**
     * Builds a job status with no data.
     * 
     * @param id      job id
     * @param status  job status
     * 
     * @return new job status object.
     */
    public JobStatus build(UUID id, JobStatus.Status status) {
        return build(id.toString(), status);
    }
    
    /**
     * Builds a job status with no data.
     * 
     * @param id      job id
     * @param status  job status
     * 
     * @return new job status object.
     */
    public JobStatus build(String id, JobStatus.Status status) {
        JobStatus jobStatus = new JobStatus(id);
        jobStatus.setStatus(status);
        return jobStatus;
    }
    
    /**
     * Converts a line of key:value pairs into a Map. Pairs are separated by
     * commas.  For example, "t:1,foo:2.4,bar:3.5".
     * 
     * @param line the line to parse 
     * 
     * @return the line in Map format.
     */
    private Map<String,Double> lineToValuesMap(String line) {
        Map<String,Double> result = new HashMap<>();
        String[] parts = line.split(",");
        for( String part : parts) {
            String[] keyAndValue = part.split(":");
            if (keyAndValue.length == 2) {
                Double value = Double.parseDouble(keyAndValue[1]);
                result.put(keyAndValue[0].trim(), value);
            }
        }
        return result;
    }
}
