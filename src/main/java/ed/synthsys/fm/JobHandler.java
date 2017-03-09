/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.synthsys.fm;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.*;
import org.springframework.beans.factory.annotation.*;

/**
 * Handles the running of new jobs and returning the current status of jobs.
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
    
    // Helper class used to build the job status from a simulation result
    private final JobStatusBuilder jobStatusBuilder;
    
    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(JobHandler.class);
            
    /**
     * Constructor
     * 
     * @param jobStatusBuilder helper used to build job status objects.
     */
    @Autowired
    public JobHandler(JobStatusBuilder jobStatusBuilder) {
        this.jobStatusBuilder = jobStatusBuilder;
    }

    /**
     * Creates a new job.  This involves running executing a process to run a
     * job as an operating system process.
     * 
     * @param jobParams job parameters
     * 
     * @return  job status
     */
    public JobStatus createJob(JobParams jobParams) {
        UUID uid = UUID.randomUUID();
        try {
            LOGGER.info("Starting job: {}", uid);
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
            command.add(jobParams.getClockGenotype());
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(WORKING_DIR);
            pb.start();
            return jobStatusBuilder.build(uid, JobStatus.Status.RUNNING);
        } catch (IOException ex) {
            LOGGER.error(
                "I/O error while trying to run job: {} error is: {}", 
                uid, ex.getLocalizedMessage());
            return jobStatusBuilder.build(uid, JobStatus.Status.FAILED);
        }
    }
    
    /**
     * Gets the job status for the specified job id.
     * 
     * @param id job id
     * 
     * @return job status
     */
    public JobStatus getJobStatus(String id) {
        File jobDir = new File(JOBS_DIR, id);
        if (!jobDir.exists()) {
            LOGGER.error(
                    "Job directory: {} does not exist. Job {} has failed.",
                    jobDir.getAbsoluteFile(),id);
            return jobStatusBuilder.build(id, JobStatus.Status.FAILED);
        }
        
        File dataFile = new File(jobDir, DATA_FILE);

        try {
            Reader reader = null;
            
            if (dataFile.exists()) {
                reader = new FileReader(dataFile);
            }
            // If is not an error if the file does it exist, it may not have
            // been created yet.
            
            return jobStatusBuilder.build(id, reader);
        }
        catch (IOException exception) {
            LOGGER.error(
                    "I/O error trying to read result for job: {} error is: {}", 
                    id, exception.getLocalizedMessage());
            return jobStatusBuilder.build(id, JobStatus.Status.FAILED);
        }
    }
}
