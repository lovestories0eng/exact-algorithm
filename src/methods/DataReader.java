package methods;

import global.Config;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DataReader {
    private String graphFile = null;
    private String vertexFile = null;
    private String edgeFile = null;
    private String attributeFile = null;
    private int vertexNum = 0;
    private int edgeNum = 0;

    public DataReader(String graphFile, String vertexFile, String edgeFile, String attributeFile){
        this.graphFile = graphFile;
        this.vertexFile = vertexFile;
        this.edgeFile = edgeFile;
        this.attributeFile = attributeFile;

        // compute the number of nodes
        try{
            File test= new File(graphFile);
            long fileLength = test.length();
            LineNumberReader rf = new LineNumberReader(new FileReader(test));
            rf.skip(fileLength);
            vertexNum = rf.getLineNumber();//obtain the number of nodes
            rf.close();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    //return the graph edge information
    public int[][] readGraph(){
        // Variable-length array for each vertex
        int[][] graph = new int[vertexNum][];
        try{
            BufferedReader stdin = new BufferedReader(new FileReader(graphFile));
            String line = null;
            while((line = stdin.readLine()) != null){
                String[] s = line.split(" ");
                // read vertex id
                int vertexId = Integer.parseInt(s[0]);
                // read the adjacent list
                int[] nb = new int[s.length - 1];
                for(int i = 1;i < s.length;i ++)
                    nb[i - 1] = Integer.parseInt(s[i]);
                graph[vertexId] = nb;
                edgeNum += nb.length / 2;
            }
            stdin.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println(graphFile + " |V|=" + vertexNum + " |E|=" + edgeNum / 2);//each edge is bidirectional
        return graph;
    }

    public static String removeCharAt(String s, int pos) {
        return s.substring(0, pos) + s.substring(pos + 1);
    }

    public double[][] readVertexAttribute() {
        double[][] attribute = new double[vertexNum][];
        try {
            BufferedReader stdin = new BufferedReader(new FileReader(attributeFile));
            // 读取第一行无用数据
            String line = stdin.readLine();
            while ((line = stdin.readLine()) != null) {
                String tmp = line;
                String[] s = line.split(" ");
                tmp = removeCharAt(tmp, s[0].length() + 1);
                tmp = tmp.substring(0, tmp.length() - 1);
                s = tmp.split(" ");

                int vertexId = Integer.parseInt(s[0]);
                double[] nb = new double[s.length - 1];
                for (int i = 1; i < s.length; i++)
                    nb[i - 1] = Double.parseDouble(s[i]);
                attribute[vertexId] = nb;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attribute;
    }

    //return the type of each vertex
    public int[] readVertexType(){
        int[] vertexType = new int[vertexNum];

        try{
            BufferedReader stdin = new BufferedReader(new FileReader(vertexFile));
            String line = null;
            while((line = stdin.readLine()) != null){
                String s[] = line.split(" ");
                int id = Integer.parseInt(s[0]);
                int type = Integer.parseInt(s[1]);
                vertexType[id] = type;
            }
            stdin.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return vertexType;
    }

    //return the type of each edge
    public int[] readEdgeType(){
        int[] edgeType = new int[edgeNum];

        try{
            BufferedReader stdin = new BufferedReader(new FileReader(edgeFile));
            String line = null;
            while((line = stdin.readLine()) != null){
                String s[] = line.split(" ");
                int id = Integer.parseInt(s[0]);
                int type = Integer.parseInt(s[1]);
                edgeType[id] = type;
            }
            stdin.close();
        }catch(Exception e){
            e.printStackTrace();
        }

        return edgeType;
    }

    public HashMap<Map.Entry<Integer, Integer>, Integer> readVertexPairMapEdge(){
        HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge = new HashMap<>();
        // Variable-length array for each vertex
        try{
            BufferedReader stdin = new BufferedReader(new FileReader(graphFile));
            String line = null;
            while((line = stdin.readLine()) != null){
                String[] s = line.split(" ");
                // read vertex id
                int vertexId = Integer.parseInt(s[0]);

                // read the adjacent list
                for(int i = 1;i < s.length; i = i + 2)
                    vertexPairMapEdge.put(Map.entry(vertexId, Integer.parseInt(s[i])), Integer.parseInt(s[i + 1]));
            }
            stdin.close();
        }catch(Exception e){
            e.printStackTrace();
        }

        // System.out.println(graphFile + " |V|=" + vertexNum + " |E|=" + edgeNum / 2);//each edge is bidirectional

        return vertexPairMapEdge;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ArrayList<String[]> loadNode() throws IOException {
        ArrayList<String[]> result = new ArrayList<String[]>();
        FileInputStream fileInputStream = new FileInputStream(Config.FILE_PATH + "/Node.txt");
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
        FileInputStream fileInputStream = new FileInputStream(Config.FILE_PATH + "/Edge.txt");
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
