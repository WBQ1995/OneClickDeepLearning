package acceler.ocdl.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.File;
public class CmdHelper {

    public static String runCommand(String cmd){
        StringBuilder output = new StringBuilder();

        System.out.println("[DEBug] docker container is running");

        try {

	        File f = new File("/home/ec2-user/model_repo/models/1001/");

            Process p = Runtime.getRuntime().exec(cmd,null,f);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String s;
            int maxOutputLines = 100;
            while ((s = br.readLine()) != null && maxOutputLines-- > 0) {
                output.append(s);
            }
            p.waitFor();
            p.destroy();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();
    }
}
