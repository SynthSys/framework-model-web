/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.synthsys.fm;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
//import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Tests for JobStatusBuilder.
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = FrameworkModelRestServiceApplication.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@WebAppConfiguration
public class JobStatusBuilderTests {
    
    final static private double EPSILON = 0.0001;
    
    // Data for creating dummy simulation data
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    final static private double[] T   = {  1,    2,     3  };
    final static private double[] BAR = {  1.1,  2.2,   3.3};
    final static private double[] FOO = { 11.1, 22.2,  33.3};
    final static private String ID = "c1272-272892-22202";
        
    /**
     * Tests the core functionality of reading in a correct simulation output.
     * 
     * @throws java.io.IOException
     */
    @Test
    public void testCoreFunctionality() throws IOException {
        
        String simResult = buildSimulationResult(true, false);
        
        System.out.println(simResult);
        JobStatusBuilder jsb = new JobStatusBuilder();
        JobStatus js = jsb.build(ID, new StringReader(simResult));
        
        assertEquals("Status value is incorrect", 
                JobStatus.Status.FINISHED, js.getStatus());
        assertEquals("Id value is incorrect", js.getId(), ID);

        List<Map<String, Double>> data = js.getData();
        
        assertEquals("Data length is incorrect",
                T.length, data.size());
        
        for (int i=0; i<T.length; ++i) {
            assertEquals("t[" + i + "] value is incorrect",
                    T[i], data.get(i).get("t"), EPSILON);
            assertEquals("bar[" + i + "] value is incorrect",
                    BAR[i], data.get(i).get("bar"), EPSILON);
            assertEquals("foo[" + i + "] value is incorrect",
                    FOO[i], data.get(i).get("foo"), EPSILON);
        }
    }
    
    /**
     * Tests the core functionality when the simulation data has spaces between
     * keys and values and after commas.
     * 
     * @throws IOException 
     */
    @Test
    public void testCoreFunctionalityWithSpaces() throws IOException {
        
        String simResult = buildSimulationResult(true, true);
        
        System.out.println(simResult);
        JobStatusBuilder jsb = new JobStatusBuilder();
        JobStatus js = jsb.build(ID, new StringReader(simResult));
        
        assertEquals("Status value is incorrect", 
                JobStatus.Status.FINISHED, js.getStatus());
        assertEquals("Id value is incorrect", js.getId(), ID);

        List<Map<String, Double>> data = js.getData();
        
        assertEquals("Data length is incorrect",
                T.length, data.size());
        
        assertTrue("Data should contain values for t",
                data.get(0).containsKey("t"));
        assertTrue("Data should contain values for bar",
                data.get(0).containsKey("bar"));
        assertTrue("Data should contain values for foo",
                data.get(0).containsKey("foo"));
        
        for (int i=0; i<T.length; ++i) {
            assertEquals("t[" + i + "] value is incorrect",
                    T[i], data.get(i).get("t"), EPSILON);
            assertEquals("bar[" + i + "] value is incorrect",
                    BAR[i], data.get(i).get("bar"), EPSILON);
            assertEquals("foo[" + i + "] value is incorrect",
                    FOO[i], data.get(i).get("foo"), EPSILON);
        }
    }

    /**
     * Tests running simulations return the RUNNING state.
     * 
     * @throws java.io.IOException
     */
    @Test
    public void testUnfinishedSimulation() throws IOException {
        String simResult = buildSimulationResult(false, false);
        
        JobStatusBuilder jsb = new JobStatusBuilder();
        JobStatus js = jsb.build(ID, new StringReader(simResult));
        
        assertEquals("Status value is incorrect", 
                JobStatus.Status.RUNNING, js.getStatus());
    }
    
    /**
     * Tests using a null reader.
     * 
     * @throws java.io.IOException
     */
    @Test
    public void testNullReader() throws IOException {
        JobStatusBuilder jsb = new JobStatusBuilder();
        Reader reader = null;
        JobStatus js = jsb.build(ID, reader);
        assertEquals("Status value is incorrect", 
                JobStatus.Status.RUNNING, js.getStatus());
    }
    
    /**
     * Tests creating a failed job.
     * 
     * @throws java.io.IOException
     */
    @Test
    public void testFailedJobStatusCreation() throws IOException {
        JobStatusBuilder jsb = new JobStatusBuilder();
        JobStatus js = jsb.build(ID, JobStatus.Status.FAILED);
        assertEquals("Status value is incorrect", 
                JobStatus.Status.FAILED, js.getStatus());
        assertEquals("Status should have no data",
                0, js.getData().size());
        assertEquals("Id value is incorrect", js.getId(), ID);
    }

    /**
     * Tests creating a running job with no data.
     * 
     * @throws java.io.IOException
     */
    @Test
    public void testRunningJobStatusCreation() throws IOException {
        JobStatusBuilder jsb = new JobStatusBuilder();
        JobStatus js = jsb.build(ID, JobStatus.Status.RUNNING);
        assertEquals("Status value is incorrect", 
                JobStatus.Status.RUNNING, js.getStatus());
        assertEquals("Status should have no data",
                0, js.getData().size());
        assertEquals("Id value is incorrect", js.getId(), ID);
    }

    /**
     * Builds a simulation result to be processed.
     * 
     * @param isFinished should result say the simulation has finished?
     * 
     * @return simulation result
     */
    private String buildSimulationResult(
            boolean isFinished, boolean includeSpaces) {
        String simResult = "";
        String spaces = "";
        if (includeSpaces) spaces = "  ";
              
        for (int i=0; i<T.length; ++i) {
            simResult += 
                    "t" + spaces + ":" + spaces + T[i] + spaces +
                    "," + spaces + "bar" + spaces + ":" + spaces + BAR[i] + 
                    "," + spaces + "foo" + spaces + ":" + spaces + FOO[i] + 
                    "\n";
        }
        if (isFinished) {
            simResult += "Finished\n";
        }
        return simResult;
    }
}
