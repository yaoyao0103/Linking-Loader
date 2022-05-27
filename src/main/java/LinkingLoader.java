import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.pow;

public class LinkingLoader {

    //private List<ESTABItem> myESTAB = new ArrayList<ESTABItem>();
    public static void main(String[] args){
        /*********************** Definition ************************/

        Map<String, Integer> ESTAB = new HashMap<String, Integer>();
        List<String> memoryContent = new ArrayList<String>();
        // get program address
        int PROGADDR = Integer.valueOf(args[0],16); // hex to dec
        int memorySpace = 0;
        /////////////////////////////////////////////////////////////

        /************************* Pass1 ***************************/
        List<String> lines = new ArrayList<String>();
        int CSADDR = PROGADDR;

        // iterate all obj program
        for(int i = 1; i < args.length; i++){
            lines = new ArrayList<String>();
            // get lines in obj file
            try{
                File doc = new File(args[i]);
                BufferedReader obj = new BufferedReader(new FileReader(doc));

                String str;
                while ((str = obj.readLine()) != null)
                    lines.add(str);
            }
            catch (Exception e){
                System.out.println(e);
            }

            // get the control section name without space and its length
            String csName = lines.get(0).substring(1, 7).replace(" ", "");
            int csLength = Integer.valueOf(lines.get(0).substring(13, 19),16);

            memorySpace += csLength;
            // put control section name and its address to external table
            ESTAB.put(csName, CSADDR);

            // iterate all lines
            for(int j = 1; j < lines.size(); j++){
                String line = lines.get(j);

                // if the line is an D record
                if( line.charAt(0) == 'D' ){
                    // remove 'D'
                    int n = (line.length()-1)/12;
                    for(int k = 0; k < n; k++){
                        // get external symbol name and its address
                        String name = line.substring(1+(12*k), 7+(12*k)).split(" ")[0];
                        int address = Integer.valueOf(line.substring(7+(12*k), 13+(12*k)),16);
                        // put the external definition in ESTAB
                        ESTAB.put(name, CSADDR + address);
                    }
                }
            }
            // set the address to the next obj program
            CSADDR += csLength;
        }
        //////////////////////////////////////////////////////////////

        // fill memory content with '.'
        for(int i = 0; i < memorySpace * 2; i++) memoryContent.add(".");
        // show external table
        System.out.println(ESTAB);


        /************************* Pass2 ***************************/
        // set control section address
        CSADDR = PROGADDR;

        // iterate all obj program
        for(int i = 1; i < args.length; i++){
            lines = new ArrayList<String>();
            // get lines in obj file
            try{
                File doc = new File(args[i]);
                BufferedReader obj = new BufferedReader(new FileReader(doc));

                String str;
                while ((str = obj.readLine()) != null)
                    lines.add(str);
            }
            catch (Exception e) {
                System.out.println(e);
            }

            int csLength = Integer.valueOf(lines.get(0).substring(13, 19),16);

            for(int j = 1; j < lines.size(); j++){
                String line = lines.get(j);
                // process T-record and M-record
                if(line.charAt(0) == 'T') processTRecord(line, CSADDR, PROGADDR, memoryContent);
                else if(line.charAt(0) == 'M') processMRecord(line, CSADDR, PROGADDR, memoryContent, ESTAB);
            }

            // set the address to the next obj program
            CSADDR += csLength;

        }
        //////////////////////////////////////////////////////////
        // show memory content
        for(int i = 0; i < memorySpace * 2; i++){
            if(i%32 == 0){
                System.out.print("\n" + Integer.toHexString(PROGADDR + (int)((i/2))).toUpperCase() + " ");
            }
            System.out.print(memoryContent.get(i) + " ");
        }
    }


    public static void processTRecord(String line, int CSADDR, int PROGADDR, List<String> memoryContent){
        int address = Integer.valueOf(line.substring(1, 7),16);
        // add control section address and then minus the program address
        address += CSADDR;
        address -= PROGADDR;
        address *= 2;

        int len = Integer.valueOf(line.substring(7, 9),16);

        // set new data to memory
        for(int i = 0; i < len * 2; i++){
            memoryContent.set(address, line.substring(9+i, 10+i));
            address += 1;
        }
    }
    public static void processMRecord(String line, int CSADDR, int PROGADDR, List<String> memoryContent, Map<String, Integer> ESTAB){
        // add control section address and then minus the program address
        int address = Integer.valueOf(line.substring(1, 7),16);
        address += CSADDR;
        address -= PROGADDR;
        address *= 2;

        int len = Integer.valueOf(line.substring(7, 9),16);
        if(len == 5) address += 1;

        // get current data
        String current = String.join("",memoryContent).substring(address, address+len);

        // convert the current data
        int value = Integer.valueOf(current,16);
        if( value >= (int)(pow(2, (len*4)-1)) ) value -= (int)(pow(2, (len*4)));
        // get the external symbol name
        String token = line.substring(10);
        // do '+' or '-' operation
        if(line.charAt(9) == '+') value += ESTAB.get(token);
        else value -= ESTAB.get(token);

        // convert new data to hex string
        String hexStr = Integer.toHexString(value).toUpperCase();
        if(value < 0) hexStr = hexStr.substring(hexStr.length()-6);

        // fill with "0"
        int n = len - hexStr.length();
        for(int i = 0; i < n; i++){
            hexStr = "0" + hexStr;
        }

        // set new data to memory
        for(int i = 0; i < len; i++){
            memoryContent.set(address, hexStr.substring(i, i+1));
            address += 1;
        }

    }
}

