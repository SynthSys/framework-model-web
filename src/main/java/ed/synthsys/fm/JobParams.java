/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.synthsys.fm;

/**
 *
 * @author ahume
 */
public class JobParams {

    public static class InputSignalStepFunction {
        private double day, night;
        private double dayLength;
        private double twilight;
        
        public InputSignalStepFunction() {}
        
        public InputSignalStepFunction( double day, double night, double dayLength, double twilight) {
            this.day       = day;
            this.night     = night;
            this.dayLength = dayLength;
            this.twilight  = twilight;
        }
        
        public double getDay()       { return day;       }
        public double getNight()     { return night;     }
        public double getDayLength() { return dayLength; }
        public double getTwilight()  { return twilight;  }

        public void setDay( double day ) { this.day  = day; }
        public void setNight(double night) { this.night = night;}
        public void setDayLength(double dayLength) { this.dayLength = dayLength;}
        public void setTwilight(double twilight) { this.twilight = twilight; }
    }
    
    private InputSignalStepFunction light;
    private InputSignalStepFunction temperature;
    private InputSignalStepFunction co2;
    
    public InputSignalStepFunction getLight()       { return light;       }
    public InputSignalStepFunction getTemperature() { return temperature; }
    public InputSignalStepFunction getCo2()         { return co2;         }
    
    public void setLight(InputSignalStepFunction issf)       { light       = issf;}
    public void setTemperature(InputSignalStepFunction issf) { temperature = issf;}
    public void setCo2(InputSignalStepFunction issf)         { co2         = issf;}
    
    public JobParams() {
    }
}
