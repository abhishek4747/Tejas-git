package emulatorinterface;

import generic.Instruction;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 *
 * @author javadb.com
 */
public class ReaderTest {
    
    /**
     * Example method for using the ObjectInputStream class
     */
    public void readPersons(String filename) {
        
        ObjectInputStream inputStream = null;
        
        try {
            
            //Construct the ObjectInputStream object
            inputStream = new ObjectInputStream(new FileInputStream(filename));
            
            Object obj = null;
            
            while ((obj = inputStream.readObject()) != null) {
                
                if (obj instanceof Instruction) {
                
                    System.out.println(((Instruction)obj).toString());
                }
                
            }
            
         
        } catch (EOFException ex) { //This exception will be caught when EOF is reached
            System.out.println("End of file reached.");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the ObjectInputStream
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new ReaderTest().readPersons("microOps.ser");
    }
}