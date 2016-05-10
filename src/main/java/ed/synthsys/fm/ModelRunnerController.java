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
 *
 * @author ahume
 */
@RestController()
public class ModelRunnerController {
    
    private final JobHandler jobHandler;
    
    @Autowired
    public ModelRunnerController(JobHandler jobHandler) {
        this. jobHandler = jobHandler;
    }
    
    // This method to allow calling across domains for the moment. Hopefully
    // when deployed will be in one domain
    // see: https://itsmebhavin.wordpress.com/2014/07/09/corsangularjs-no-access-control-allow-origin-header-is-present-on-the-requested-resource-web-api-angularjs/
    @RequestMapping(value="/proxy.html", method=GET)
    public String getProxy() {
        return "<!DOCTYPE HTML>\n" +
               "<script src=”//rawgit.com/jpillora/xdomain/gh-pages/dist/0.6/xdomain.js” master=”*”></script>\n";
    }

    @RequestMapping(value="/modelRunner", method=POST)
    public JobStatus runJob(@RequestBody JobParams input) {
        return jobHandler.createJob(input);
    }
    
    @RequestMapping(value="/modelRunner/{jobId}", method=GET)
    public JobStatus getJobStatus(@PathVariable String jobId) {
        return jobHandler.getJobStatus(jobId);
    }  
}
