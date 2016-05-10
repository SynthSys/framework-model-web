/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.synthsys.fm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.*;
import org.springframework.beans.factory.annotation.*;
/**
 *
 * @author ahume
 */
@Component
public class JobHandler {

    @Value("${runner.dir}")
    private File WORKING_DIR;

    @Value("${jobs.dir}")
    private File JOBS_DIR;
    
    @Value("${result.file}")
    private final String DATA_FILE = "myResultFile.csv";
    
    @Value("${run.command}")    
    private final String RUN_COMMAND = "./runSimulation";
    
    private final String FINISHED_LINE = "Finished";
            
    public JobHandler() {
    }
    
    public JobStatus createJob(JobParams jobParams) {
        try {
            UUID uid = UUID.randomUUID();
            List<String> command = new ArrayList<>();
            command.add(RUN_COMMAND);
            command.add(uid.toString());
            command.add(Double.toString(jobParams.getLight().getDay()));
            command.add(Double.toString(jobParams.getLight().getNight()));
            command.add(Double.toString(jobParams.getLight().getDayLength()));
            command.add(Double.toString(jobParams.getLight().getTwilight()));
            command.add(Double.toString(jobParams.getTemperature().getDay()));
            command.add(Double.toString(jobParams.getTemperature().getNight()));
            command.add(Double.toString(jobParams.getTemperature().getDayLength()));
            command.add(Double.toString(jobParams.getTemperature().getTwilight()));
            command.add(Double.toString(jobParams.getCo2().getDay()));
            command.add(Double.toString(jobParams.getCo2().getNight()));
            command.add(Double.toString(jobParams.getCo2().getDayLength()));
            command.add(Double.toString(jobParams.getCo2().getTwilight()));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(WORKING_DIR);
            Process p = pb.start();
            JobStatus jobStatus = new JobStatus(uid.toString());
            return jobStatus;
        } catch (IOException ex) {
            // TODO - need to handle this much better
                System.err.println("IOException");
        }
        return null;
    }
    
    public JobStatus getJobStatus(String id) {
        JobStatus jobStatus = new JobStatus(id);
        
        File jobDir = new File(JOBS_DIR, id);
        // TODO - check for existing directory etc
        
        File dataFile = new File(jobDir, DATA_FILE);

        jobStatus.setStatus(JobStatus.Status.RUNNING);
        if (dataFile.exists()) {

            // Read the data from the data file
            try (BufferedReader br = new BufferedReader(new FileReader(dataFile))) {

                String line;
                while ((line = br.readLine()) != null) {
                    
                    if (line.equals(FINISHED_LINE)) {
                        jobStatus.setStatus(JobStatus.Status.FINISHED);
                    }    
                    else {
                        jobStatus.addData(lineToValuesMap(line));
                    }
                }
            }
            catch(IOException e) {
                //TODO - do better
                System.out.println("IO Exceptoin");
                e.printStackTrace();
            }

        }
        
        return jobStatus;
    }
    
    private Map<String,Double> lineToValuesMap(String line) {
        Map<String,Double> result = new HashMap<>();
        String[] parts = line.split(",");
        for( String part : parts) {
            String[] keyAndValue = part.split(":");
            Double value = Double.parseDouble(keyAndValue[1]);
            result.put(keyAndValue[0], value);
        }
        return result;
    }
}
