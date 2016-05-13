/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.synthsys.fm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for the Model Runner web service.
 */
@RestController()
public class ModelRunnerController {

    // Job hander user to submit jobs and get the job status
    private final JobHandler jobHandler;
    
    /**
     * Constructor.
     * 
     * @param jobHandler  job handler to process jobs
     */
    @Autowired
    public ModelRunnerController(JobHandler jobHandler) {
        this.jobHandler = jobHandler;
    }
    
    /**
     * Runs a new job.
     * 
     * @param jobParams parameters for the job to run.
     * 
     * @return the job status
     */
    @RequestMapping(value="/modelRunner", method=POST)
    public JobStatus runJob(@RequestBody JobParams jobParams) {
        return jobHandler.createJob(jobParams);
    }
    
    /**
     * Gets the current status of a job.
     * 
     * @param jobId job id
     * 
     * @return job status
     */
    @RequestMapping(value="/modelRunner/{jobId}", method=GET)
    public JobStatus getJobStatus(@PathVariable String jobId) {
        return jobHandler.getJobStatus(jobId);
    }  
}
