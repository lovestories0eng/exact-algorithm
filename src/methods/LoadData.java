package methods;

import global.Constants;

import java.io.*;
import java.util.ArrayList;

public class LoadData {

    public static ArrayList<String[]> loadNode() throws IOException {
        ArrayList<String[]> result = new ArrayList<String[]>();
        FileInputStream fileInputStream = new FileInputStream(Constants.FILE_PATH + "/Node.txt");
        InputStreamReader reader = new InputStreamReader(fileInputStream);
        BufferedReader bufferedReader = new BufferedReader(reader);
        String strTmp = "";
        while ((strTmp = bufferedReader.readLine()) != null) {
            result.add(strTmp.split(" "));
        }
        bufferedReader.close();
        return result;
    }

    public static ArrayList<int[]> loadEdge() throws IOException {
        ArrayList<int[]> result = new ArrayList<int[]>();
        FileInputStream fileInputStream = new FileInputStream(Constants.FILE_PATH + "/Edge.txt");
        InputStreamReader reader = new InputStreamReader(fileInputStream);
        BufferedReader bufferedReader = new BufferedReader(reader);
        String strTmp = "";
        while ((strTmp = bufferedReader.readLine()) != null) {
            String[] tmp = strTmp.split(" ");
            int[] tmpInt = new int[2];
            for (int i = 0; i < tmp.length; i++) {
                tmpInt[i] = Integer.parseInt(tmp[i]);
                tmpInt[i] = Integer.parseInt(tmp[i]);
            }
            result.add(tmpInt);
        }
        bufferedReader.close();
        return result;
    }
}
